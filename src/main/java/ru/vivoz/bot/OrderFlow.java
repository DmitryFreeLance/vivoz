package ru.vivoz.bot;

import java.util.ArrayList;
import java.util.List;

public final class OrderFlow {
    private static final List<Option> PACKING_OPTIONS = List.of(
            new Option("yes", "✅ Да", "Да"),
            new Option("no", "❌ Нет", "Нет"),
            new Option("consult", "💬 Нужна консультация", "Нужна консультация")
    );

    private static final String FROM_TEXT = "Укажите <b>откуда</b> везём\n\n" +
            "<i>Например:\n" +
            "Ленина 65, 5 подъезд, въезд во двор закрыт. Необходимо набрать домофон. 4 этаж, лифт грузовой</i>";

    private static final String TO_TEXT = "Укажите <b>куда</b> везём\n\n" +
            "<i>Например:\n" +
            "Малышева 6, 2 подъезд, въезд свободный. 3 этаж, лифта нет</i>";

    private static final String ADDRESS_WORK_TEXT = "Укажите <b>адрес проведения работ</b>\n\n" +
            "<i>Например:\n" +
            "Ленина 65, 5 подъезд, кв 82</i>";

    private OrderFlow() {
    }

    public static List<Question> questionsFor(OrderType type) {
        return switch (type) {
            case HOME_MOVE -> homeMove();
            case OFFICE_MOVE -> officeMove();
            case LOADERS_ONLY -> loadersOnly();
            case GAZELLE_ONLY -> gazelleOnly();
        };
    }

    private static List<Question> homeMove() {
        List<Question> list = new ArrayList<>();
        list.add(new Question("from", "Откуда", FROM_TEXT));
        list.add(new Question("to", "Куда", TO_TEXT));
        list.add(new Question("cargo", "Груз", "<b>Кратко</b> опишите перевозимый груз:"));
        list.add(new Question("packing", "Разборка/упаковка", "Нужна ли <b>разборка/упаковка</b> техники?", PACKING_OPTIONS));
        list.add(new Question("date", "Дата", "На какую <b>дату</b> вы планируете переезд?"));
        list.add(new Question("phone", "Телефон", "Укажите номер телефона для связи:"));
        return list;
    }

    private static List<Question> officeMove() {
        return homeMove();
    }

    private static List<Question> loadersOnly() {
        List<Question> list = new ArrayList<>();
        list.add(new Question("people", "Количество людей", "Какое <b>количество человек</b> необходимо?"));
        list.add(new Question("works", "Работы", "Какие <b>работы</b> необходимо сделать?"));
        list.add(new Question("address", "Адрес работ", ADDRESS_WORK_TEXT));
        list.add(new Question("date", "Дата", "На какую <b>дату</b> вы планируете заказ?"));
        list.add(new Question("phone", "Телефон", "Укажите номер телефона для связи:"));
        return list;
    }

    private static List<Question> gazelleOnly() {
        List<Question> list = new ArrayList<>();
        list.add(new Question("from", "Откуда", FROM_TEXT));
        list.add(new Question("to", "Куда", TO_TEXT));
        list.add(new Question("cargo", "Груз", "<b>Кратко</b> опишите перевозимый груз:"));
        list.add(new Question("weight", "Вес", "Укажите вес <b>перевозимого</b> груза:"));
        list.add(new Question("date", "Дата", "На какую <b>дату</b> вы планируете переезд?"));
        list.add(new Question("phone", "Телефон", "Укажите номер телефона для связи:"));
        return list;
    }

    public static String resolveOptionValue(Question question, String code) {
        if (question == null || !question.hasOptions()) {
            return null;
        }
        for (Option option : question.getOptions()) {
            if (option.getCode().equals(code)) {
                return option.getValue();
            }
        }
        return null;
    }

    public static List<Option> packingOptions() {
        return PACKING_OPTIONS;
    }
}
