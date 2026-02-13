package ru.vivoz.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        String token = envOrThrow("BOT_TOKEN");
        String username = envOrDefault("BOT_USERNAME", "");
        String adminIdsRaw = envOrDefault("ADMIN_IDS", "");
        String dbPath = envOrDefault("DB_PATH", "data/bot.db");

        Path dbFile = Path.of(dbPath);
        Path parent = dbFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        OrderRepository repository = new OrderRepository(dbPath);
        repository.init();

        Set<Long> adminIds = new HashSet<>(BotUtils.parseAdminIds(adminIdsRaw));
        adminIds.addAll(repository.loadAdmins());

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        VivezBot bot = new VivezBot(token, username, adminIds, repository);
        try {
            api.registerBot(bot);
            System.out.println("Bot started");
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to register bot", e);
        }
    }

    private static String envOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env: " + key);
        }
        return value.trim();
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
