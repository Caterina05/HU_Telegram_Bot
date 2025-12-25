package org.example;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

public class Main {
    public static void main(String[] args) {
        String botToken = ConfigurationSingleton.getInstance().getProperty("BOT_TOKEN");
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()){
            botsApplication.registerBot(botToken, new AnimalBot(botToken));
            System.out.println("AnimalBot successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}