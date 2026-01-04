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

    public void insertUser(long telegramId, String username, String firstName) {
        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return;
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return;
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
        }
    }

    public boolean saveSearch(long telegramId, String animalName) {
        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return false;
        }

        String query = "INSERT INTO search_history (telegram_id, animal_name) VALUES (?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setLong(1, telegramId);
            statement.setString(2, animalName);

            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Errore di query: " + e.getMessage());
            return false;
        }
    }

    public String getUserHistory(long  telegramId) {
        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return "Errore di connessione al database";
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return "Errore di connessione al database";
        }

        String query = """
            SELECT animal_name, searched_at
            FROM search_history
            WHERE telegram_id = ?
            ORDER BY searched_at DESC
            LIMIT 10
        """;

        String result = "Ultime ricerche:\n";

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, telegramId);

            ResultSet rs = statement.executeQuery();

            boolean empty = true;
            while (rs.next()) {
                empty = false;
                result += "- " + rs.getString("animal_name");
                result += " (" + rs.getString("searched_at") + ")\n";
            }

            return empty ? "Nessuna ricerca trovata" : result;
        } catch (SQLException e) {
            System.err.println("Errore di query: " + e.getMessage());
            return "Errore nel recupero della cronologia";
        }
    }

    public boolean addFavourite(long telegramId, int animalId, String animalName) {
        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return false;
        }

        String query = "INSERT OR IGNORE INTO favourites (telegram_id, animal_id, animal_name) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            statement.setLong(1, telegramId);
            statement.setInt(2, animalId);
            statement.setString(3, animalName);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Errore di query: " + e.getMessage());
            return false;
        }
    }

    public String getFavourites(long telegramId) {
        try {
            if (connection == null || !connection.isValid(5)) {
                System.err.println("Errore di connessione al database");
                return "Errore di connessione al database";
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database");
            return "Errore di connessione al database";
        }

        String query = "SELECT animal_name FROM favourites WHERE telegram_id = ?";

        String result = "Animali preferiti:\n";

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setLong(1, telegramId);

            ResultSet rs = statement.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                result += "- " + rs.getString("animal_name") + "\n";
            }

            return count == 0 ? "Nessun animale nei preferiti" : result;
        } catch (SQLException e) {
            System.err.println("Errore di query: " + e.getMessage());
            return "Errore nel recupero delle preferenze";
        }
    }
}
