package ru.vivoz.bot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderRepository {
    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final String dbPath;

    public OrderRepository(String dbPath) {
        this.dbPath = dbPath;
    }

    public void init() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        username TEXT,
                        full_name TEXT,
                        order_type TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        date_value TEXT,
                        phone TEXT,
                        answers_json TEXT
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admin_users (
                        user_id INTEGER PRIMARY KEY,
                        added_at TEXT NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init database", e);
        }
    }

    public Set<Long> loadAdmins() {
        String sql = "SELECT user_id FROM admin_users";
        Set<Long> result = new HashSet<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load admins", e);
        }
        return result;
    }

    public boolean addAdmin(long userId, String addedAt) {
        String sql = """
                INSERT OR IGNORE INTO admin_users (user_id, added_at)
                VALUES (?, ?)
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, addedAt);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add admin", e);
        }
    }

    public long save(OrderPayload payload) {
        String sql = """
                INSERT INTO orders (user_id, username, full_name, order_type, created_at, date_value, phone, answers_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, payload.userId());
            statement.setString(2, payload.username());
            statement.setString(3, payload.fullName());
            statement.setString(4, payload.type().name());
            statement.setString(5, payload.createdAt());
            statement.setString(6, payload.dateValue());
            statement.setString(7, payload.phone());
            statement.setString(8, GSON.toJson(payload.answers()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save order", e);
        }
        return -1;
    }

    public List<OrderSummary> lastOrders(int limit) {
        String sql = """
                SELECT id, order_type, date_value, phone, created_at
                FROM orders
                ORDER BY id DESC
                LIMIT ?
                """;
        List<OrderSummary> result = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new OrderSummary(
                            rs.getLong("id"),
                            rs.getString("order_type"),
                            rs.getString("date_value"),
                            rs.getString("phone"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read last orders", e);
        }
        return result;
    }

    public int countAll() {
        return count("SELECT COUNT(*) FROM orders");
    }

    public int countToday() {
        return count("SELECT COUNT(*) FROM orders WHERE date(created_at) = date('now', 'localtime')");
    }

    private int count(String sql) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count orders", e);
        }
        return 0;
    }

    public Map<String, String> parseAnswers(String json) {
        return GSON.fromJson(json, MAP_TYPE);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_PREFIX + dbPath);
    }
}
