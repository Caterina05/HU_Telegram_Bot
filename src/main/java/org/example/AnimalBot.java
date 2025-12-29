package org.example;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.SQLException;

public class AnimalBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final INaturalistApi api = new INaturalistApi();

    public AnimalBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Set variables
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            if(message_text.startsWith("/start")){
                startMessage(update);
            } else if (message_text.startsWith("/help")){
                helpMessage(chat_id);
            } else if (message_text.startsWith("/animal")){
                animalMessage(chat_id, message_text);
            } else if (message_text.startsWith("/random")){

            } else {
                unknownMessage(chat_id);
            }
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            // Message contains photo
        }
    }

    private void startMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        var tgUser = update.getMessage().getFrom();

        try {
            Database db = Database.getInstance();
            if(!db.userExists(tgUser.getId())){
                db.insertUser(tgUser.getId(), tgUser.getUserName(), tgUser.getFirstName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sendMessage(chatId, "Benvenuto in AnimalBot\nScrivi /help per vedere i comandi disponibili");
    }

    private void helpMessage(long chatId){
        String message =
                "Comandi disponibili:\n" +
                "/start - Avvia il bot\n" +
                "/help - Mostra i comandi disponibili\n" +
                "/animal <nome> - Informazioni su un animale\n" +
                "/random - Animale casuale";
        sendMessage(chatId, message);
    }

    private void animalMessage(long chatId, String text){
        String name = text.replace("/animal", "").trim();

        if(name.isEmpty()){
            sendMessage(chatId, "Comando: /animal <nome>");
            return;
        }

        AutocompleteResponse response = api.autocompleteAnimal(name);
        if(response == null || response.getResults() == null || response.getResults().length == 0) {
            sendMessage(chatId, "Animale non trovato");
            return;
        }

        AutocompleteAnimal animal = response.getResults()[0];

        String extinctStatus = animal.isExtinct() ? "Stato: estinto" : "Stato: non estinto";

        String caption = animal.getDisplayName() + "\n" +
                "Nome scientifico: " + animal.getScientificName() + "\n" +
                "Classe: " + animal.getIconicTaxon() + "\n" +
                extinctStatus + "\n" +
                animal.getWikipediaUrl();

        if(animal.getImageUrl() != null) {
            sendPhoto(chatId, animal.getImageUrl(), caption);
        } else {
            sendMessage(chatId, caption + "\n(Immagine non disponibile)");
        }
    }

    private void unknownMessage(long chatId){
        sendMessage(chatId, "Comando non riconosciuto\nScrivi /help per vedere i comandi disponibili");
    }

    private void sendMessage(long chatId, String text){
        SendMessage message = SendMessage // Create a message object
                .builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message); // Sending our message object to user
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(long chatId, String photoUrl, String caption){
        SendPhoto photo = SendPhoto
                .builder()
                .chatId(chatId)
                .photo(new InputFile(photoUrl))
                .caption(caption)
                .build();

        try {
            telegramClient.execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}