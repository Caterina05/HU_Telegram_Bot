package org.example;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.example.AnimalBot;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


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