package ru.vivoz.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VivezBot extends TelegramLongPollingBot {
    private static final String CALLBACK_MENU_HOME = "MENU_HOME";
    private static final String CALLBACK_MENU_OFFICE = "MENU_OFFICE";
    private static final String CALLBACK_MENU_LOADERS = "MENU_LOADERS";
    private static final String CALLBACK_MENU_GAZELLE = "MENU_GAZELLE";
    private static final String CALLBACK_MENU_ADMIN = "MENU_ADMIN";
    private static final String CALLBACK_BACK_MENU = "BACK_MENU";
    private static final String CALLBACK_ADMIN_LAST = "ADMIN_LAST";
    private static final String CALLBACK_ADMIN_STATS = "ADMIN_STATS";

    private final String token;
    private final String username;
    private final Set<Long> adminIds;
    private final OrderRepository repository;
    private final SessionStore sessions = new SessionStore();

    public VivezBot(String token, String username, Set<Long> adminIds, OrderRepository repository) {
        this.token = token;
        this.username = username;
        this.adminIds = adminIds;
        this.repository = repository;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return username == null ? "" : username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        if (message == null || !message.hasText()) {
            return;
        }
        String text = message.getText().trim();
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        if ("/start".equalsIgnoreCase(text)) {
            sessions.clear(userId);
            sendStart(chatId, userId);
            return;
        }

        if ("/admin".equalsIgnoreCase(text)) {
            if (isAdmin(userId)) {
                sendAdminPanel(chatId);
            } else {
                sendText(chatId, "Нет доступа к админ панели.");
            }
            return;
        }

        Session session = sessions.get(userId);
        if (session == null) {
            sendMenu(chatId, userId, "Пожалуйста, выберите, что вас интересует👇🏼");
            return;
        }

        Question question = session.currentQuestion();
        if (question == null) {
            sessions.clear(userId);
            sendMenu(chatId, userId, "Пожалуйста, выберите, что вас интересует👇🏼");
            return;
        }

        if (question.hasOptions()) {
            sendQuestion(chatId, question, "Пожалуйста, выберите вариант кнопкой ниже.");
            return;
        }

        String answer = text.trim();
        if (answer.isEmpty()) {
            sendQuestion(chatId, question, "Пожалуйста, напишите ответ.");
            return;
        }

        session.getAnswers().put(question.getKey(), answer);
        if (session.advance()) {
            Question next = session.currentQuestion();
            sendQuestion(chatId, next, null);
        } else {
            finalizeOrder(chatId, message.getFrom(), session);
            sessions.clear(userId);
        }
    }

    private void handleCallback(CallbackQuery query) {
        String data = query.getData();
        long userId = query.getFrom().getId();
        long chatId = query.getMessage().getChatId();

        if (CALLBACK_BACK_MENU.equals(data)) {
            sessions.clear(userId);
            sendStart(chatId, userId);
            return;
        }

        if (CALLBACK_MENU_HOME.equals(data)) {
            startOrder(chatId, userId, OrderType.HOME_MOVE);
            return;
        }
        if (CALLBACK_MENU_OFFICE.equals(data)) {
            startOrder(chatId, userId, OrderType.OFFICE_MOVE);
            return;
        }
        if (CALLBACK_MENU_LOADERS.equals(data)) {
            startOrder(chatId, userId, OrderType.LOADERS_ONLY);
            return;
        }
        if (CALLBACK_MENU_GAZELLE.equals(data)) {
            startOrder(chatId, userId, OrderType.GAZELLE_ONLY);
            return;
        }
        if (CALLBACK_MENU_ADMIN.equals(data)) {
            if (isAdmin(userId)) {
                sendAdminPanel(chatId);
            } else {
                sendText(chatId, "Нет доступа к админ панели.");
            }
            return;
        }
        if (CALLBACK_ADMIN_LAST.equals(data)) {
            if (isAdmin(userId)) {
                sendLastOrders(chatId);
            }
            return;
        }
        if (CALLBACK_ADMIN_STATS.equals(data)) {
            if (isAdmin(userId)) {
                sendAdminStats(chatId);
            }
            return;
        }

        if (data != null && data.startsWith("OPT:")) {
            handleOptionCallback(query);
        }
    }

    private void handleOptionCallback(CallbackQuery query) {
        String data = query.getData();
        long userId = query.getFrom().getId();
        long chatId = query.getMessage().getChatId();
        Session session = sessions.get(userId);
        if (session == null) {
            sendMenu(chatId, userId, "Пожалуйста, выберите, что вас интересует👇🏼");
            return;
        }
        Question question = session.currentQuestion();
        if (question == null || !question.hasOptions()) {
            sendMenu(chatId, userId, "Пожалуйста, выберите, что вас интересует👇🏼");
            return;
        }

        String[] parts = data.split(":", 3);
        if (parts.length < 3) {
            return;
        }
        String key = parts[1];
        String code = parts[2];
        if (!question.getKey().equals(key)) {
            return;
        }
        String value = OrderFlow.resolveOptionValue(question, code);
        if (value == null) {
            return;
        }

        session.getAnswers().put(question.getKey(), value);
        if (session.advance()) {
            Question next = session.currentQuestion();
            sendQuestion(chatId, next, null);
        } else {
            finalizeOrder(chatId, query.getFrom(), session);
            sessions.clear(userId);
        }
    }

    private void startOrder(long chatId, long userId, OrderType type) {
        Session session = new Session(type);
        sessions.put(userId, session);
        Question question = session.currentQuestion();
        sendQuestion(chatId, question, null);
    }

    private void sendStart(long chatId, long userId) {
        String text = "👋 Здравствуйте! Рады приветствовать в компании Вывезем Всё\n\n" +
                "Выберите что вы хотите заказать👇🏼";
        sendMenu(chatId, userId, text);
    }

    private void sendMenu(long chatId, long userId, String text) {
        InlineKeyboardMarkup markup = buildMenuKeyboard(isAdmin(userId));
        sendText(chatId, text, markup);
    }

    private void sendQuestion(long chatId, Question question, String prefix) {
        String text = question.getText();
        if (prefix != null && !prefix.isBlank()) {
            text = prefix + "\n\n" + text;
        }
        InlineKeyboardMarkup markup = buildQuestionKeyboard(question);
        sendText(chatId, text, markup);
    }

    private InlineKeyboardMarkup buildMenuKeyboard(boolean isAdmin) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(button("🏠 Домашний переезд", CALLBACK_MENU_HOME),
                button("🏢 Офисный переезд", CALLBACK_MENU_OFFICE)));
        rows.add(List.of(button("👷 Только грузчики", CALLBACK_MENU_LOADERS),
                button("🚚 Только газель", CALLBACK_MENU_GAZELLE)));
        if (isAdmin) {
            rows.add(List.of(button("🛠 Админ панель", CALLBACK_MENU_ADMIN)));
        }
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup buildQuestionKeyboard(Question question) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (question.hasOptions()) {
            List<Option> options = question.getOptions();
            if (options.size() >= 2) {
                rows.add(List.of(optionButton(question.getKey(), options.get(0)),
                        optionButton(question.getKey(), options.get(1))));
            }
            if (options.size() >= 3) {
                rows.add(List.of(optionButton(question.getKey(), options.get(2))));
            }
            if (options.size() > 3) {
                for (int i = 3; i < options.size(); i++) {
                    rows.add(List.of(optionButton(question.getKey(), options.get(i))));
                }
            }
        }
        rows.add(List.of(button("🔙 Вернуться в меню", CALLBACK_BACK_MENU)));
        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardMarkup buildAdminKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(button("📋 Последние 5 заявок", CALLBACK_ADMIN_LAST)));
        rows.add(List.of(button("📊 Статистика", CALLBACK_ADMIN_STATS)));
        rows.add(List.of(button("🔙 В меню", CALLBACK_BACK_MENU)));
        return new InlineKeyboardMarkup(rows);
    }

    private void sendAdminPanel(long chatId) {
        String text = "🛠 Админ панель\n" +
                "Всего заявок: " + repository.countAll() + "\n" +
                "Сегодня: " + repository.countToday();
        sendText(chatId, text, buildAdminKeyboard());
    }

    private void sendAdminStats(long chatId) {
        String text = "📊 Статистика\n" +
                "Всего заявок: " + repository.countAll() + "\n" +
                "Сегодня: " + repository.countToday();
        sendText(chatId, text, buildAdminKeyboard());
    }

    private void sendLastOrders(long chatId) {
        List<OrderSummary> orders = repository.lastOrders(5);
        StringBuilder sb = new StringBuilder("📋 Последние 5 заявок\n");
        if (orders.isEmpty()) {
            sb.append("Нет заявок.");
        } else {
            int index = 1;
            for (OrderSummary order : orders) {
                sb.append(index++)
                        .append(") #")
                        .append(order.id())
                        .append(" • ")
                        .append(resolveTypeTitle(order.orderType()))
                        .append(" • ")
                        .append(order.dateValue() == null ? "-" : order.dateValue())
                        .append(" • ")
                        .append(order.phone() == null ? "-" : order.phone())
                        .append("\n");
            }
        }
        sendText(chatId, sb.toString(), buildAdminKeyboard());
    }

    private void finalizeOrder(long chatId, org.telegram.telegrambots.meta.api.objects.User user, Session session) {
        long userId = user.getId();
        String username = user.getUserName();
        String fullName = BotUtils.fullName(user.getFirstName(), user.getLastName());
        Map<String, String> answers = session.getAnswers();

        String createdAt = BotUtils.formatCreatedAt(LocalDateTime.now());
        String dateValue = answers.getOrDefault("date", "");
        String phone = answers.getOrDefault("phone", "");

        OrderPayload payload = new OrderPayload(
                userId,
                username,
                fullName,
                session.getType(),
                createdAt,
                dateValue,
                phone,
                answers
        );
        repository.save(payload);

        notifyAdmins(payload);

        String confirmation = "Ваша заявка принята, для уточнения деталей и расчета стоимости с Вами свяжется наш менеджер.\n" +
                "<b>Спасибо что выбрали нас.</b>\n" +
                "ВывеземВсё.рф";
        sendText(chatId, confirmation, buildMenuKeyboard(isAdmin(userId)));
    }

    private void notifyAdmins(OrderPayload payload) {
        if (adminIds == null || adminIds.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("🚚 Новый заказ!\n");
        sb.append("🏷 Тип: ").append(payload.type().getTitle()).append("\n");
        sb.append("👤 Имя: ").append(payload.fullName()).append("\n");
        sb.append("🏷 Тег: ").append(payload.username() == null || payload.username().isBlank() ? "-" : "@" + payload.username()).append("\n");
        sb.append("📞 Номер телефона: ").append(payload.phone().isBlank() ? "-" : payload.phone()).append("\n");
        sb.append("📅 Дата: ").append(payload.dateValue().isBlank() ? "-" : payload.dateValue()).append("\n");

        List<Question> questions = OrderFlow.questionsFor(payload.type());
        for (Question question : questions) {
            String key = question.getKey();
            if ("date".equals(key) || "phone".equals(key)) {
                continue;
            }
            String value = payload.answers().get(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            sb.append("<b>").append(question.getLabel()).append(":</b> ").append(value).append("\n");
        }

        String text = sb.toString();
        for (Long adminId : adminIds) {
            sendTextSafe(adminId, text, null);
        }
    }

    private InlineKeyboardButton button(String text, String data) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(data);
        return button;
    }

    private InlineKeyboardButton optionButton(String key, Option option) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(option.getLabel());
        button.setCallbackData("OPT:" + key + ":" + option.getCode());
        return button;
    }

    private void sendText(long chatId, String text) {
        sendText(chatId, text, null);
    }

    private void sendText(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        if (markup != null) {
            message.setReplyMarkup(markup);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }

    private void sendTextSafe(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        if (markup != null) {
            message.setReplyMarkup(markup);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Failed to send admin message to chatId=" + chatId + ": " + e.getMessage());
        }
    }

    private boolean isAdmin(long userId) {
        return adminIds != null && adminIds.contains(userId);
    }

    private String resolveTypeTitle(String typeName) {
        try {
            return OrderType.valueOf(typeName).getTitle();
        } catch (Exception e) {
            return typeName;
        }
    }
}
