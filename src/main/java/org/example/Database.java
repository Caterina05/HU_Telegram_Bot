package org.example;

import java.sql.*;

public class Database {
    private Connection connection;
    private static Database instance;

    private Database() throws SQLException {
        String url = "jdbc:sqlite:database/animalbot.db";
        connection = DriverManager.getConnection(url);
        System.out.println("Connected to database");
    }

    public static Database getInstance() throws SQLException {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean userExists(long telegramId) {
        String query = "SELECT 1 FROM users WHERE telegram_id = ?";

        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return false;
        }

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, telegramId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Errore di query: " + e.getMessage());
            return false;
        }
    }

    public boolean insertUser(long telegramId, String username, String firstName) {
        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return false;
        }

        String query = "INSERT INTO users (telegram_id, username, first_name) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setLong(1, telegramId);
            statement.setString(2, username);
            statement.setString(3, firstName);

            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore di query: " + e.getMessage());
            return false;
        }
        return true;
    }
}
