package org.example;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

            } else if (message_text.startsWith("/history")) {
                historyMessage(chat_id);
            } else if (message_text.startsWith("/clearhistory")) {
                    clearHistoryMessage(chat_id);
            } else if (message_text.startsWith("/favourites")) {
                favouritesMessage(chat_id);
            } else {
                unknownMessage(chat_id);
            }
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            // Message contains photo
        } else if (update.hasCallbackQuery()){
            String call_data = update.getCallbackQuery().getData();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            long telegram_id = update.getCallbackQuery().getFrom().getId();

            if (call_data.startsWith("FAV_")) {
                String payload = call_data.replace("FAV_", "");
                String[] parts = payload.split("\\|");

                int animalId = Integer.parseInt(parts[0]);
                String animalName = parts[1];

                try {
                    boolean saved = Database.getInstance().addFavourite(telegram_id, animalId, animalName);
                    if(saved) {
                        sendMessage(chat_id, "Animale aggiunto ai preferiti");
                    } else {
                        sendMessage(chat_id, "Questo animale è già nei preferiti");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else if (call_data.startsWith("DEL_")) {
                int animalId = Integer.parseInt(call_data.replace("DEL_", ""));
                try {
                    Database.getInstance().removeFavourite(telegram_id, animalId);
                    sendMessage(chat_id, "Animale rimosso dai preferiti");
                } catch (SQLException e) {
                    sendMessage(chat_id, "Errore database");
                }
            }
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
                "/animal <nome> - Mostra alcune informazioni sull'animale ricercato\n" +
                "/random - Mostra alcune informazioni su un animale casuale\n" +
                "/history - Visualizza le ultime ricerche\n" +
                "/clearhistory - Elimina la cronologia delle ricerche\n" +
                "/favourites - Mostra gli animali preferiti";
        sendMessage(chatId, message);
    }

    private void animalMessage(long chatId, String text){
        String name = text.replace("/animal", "").trim();

        if(name.isEmpty()){
            sendMessage(chatId, "Comando: /animal <nome>");
            return;
        }

        Animal animal = api.autocompleteAnimal(name);
        if(animal == null) {
            sendMessage(chatId, "Animale non trovato");
            return;
        }

        String extinctStatus = animal.isExtinct() ? "Stato: estinto" : "Stato: non estinto";

        String caption = animal.getDisplayName() + "\n" +
                "Nome scientifico: " + animal.getScientificName() + "\n" +
                "Classe: " + animal.getIconicTaxon() + "\n" +
                extinctStatus + "\n" +
                animal.getWikipediaUrl();

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(
                        new InlineKeyboardRow(InlineKeyboardButton
                                .builder()
                                .text("❤\uFE0F Aggiungi ai preferiti")
                                .callbackData("FAV_" + animal.getId() + "|" + animal.getDisplayName())
                                .build()
                        )
                )
                .build();

        if(animal.getImageUrl() != null) {
            sendPhoto(chatId, animal.getImageUrl(), caption, keyboard);
        } else {
            sendKeyboard(chatId, caption + "\n(Immagine non disponibile)", keyboard);
        }

        try {
            Database.getInstance().saveSearch(chatId, animal.getDisplayName());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void historyMessage(long chatId){
        try {
            String history = Database.getInstance().getUserHistory(chatId);
            sendMessage(chatId, history);
        } catch (SQLException e) {
            sendMessage(chatId, "Errore database");
        }
    }

    private void clearHistoryMessage(long chatId){
        try {
            boolean cleared = Database.getInstance().clearUserHistory(chatId);
            if(cleared){
                sendMessage(chatId, "Cronologia delle ricerche eliminata");
            } else {
                sendMessage(chatId, "Non c'era alcuna cronologia da eliminare");
            }
        } catch (SQLException e) {
            sendMessage(chatId, "Errore database");
        }
    }

    private void favouritesMessage(long chatId){
        try {
            List<Animal> favourites = Database.getInstance().getFavourites(chatId);

            if(favourites.isEmpty()){
                sendMessage(chatId, "Nessun animale nei preferiti");
                return;
            }

            List<InlineKeyboardRow> rows = new ArrayList<>();
            for(Animal animal : favourites){
                InlineKeyboardButton nameBtn = InlineKeyboardButton
                        .builder()
                        .text(animal.getDisplayName())
                        .callbackData("INFO_" + animal.getId())
                        .build();

                InlineKeyboardButton removeBtn = InlineKeyboardButton
                        .builder()
                        .text("❌")
                        .callbackData("DEL_" +animal.getId())
                        .build();
                rows.add(new InlineKeyboardRow(nameBtn, removeBtn));
            }

            InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                    .keyboard(rows)
                    .build();

            sendKeyboard(chatId, "Animali preferiti:", keyboard);
        } catch (SQLException e) {
            sendMessage(chatId, "Errore database");
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

    private void sendPhoto(long chatId, String photoUrl, String caption, InlineKeyboardMarkup keyboard){
        SendPhoto photo = SendPhoto
                .builder()
                .chatId(chatId)
                .photo(new InputFile(photoUrl))
                .caption(caption)
                .replyMarkup(keyboard)
                .build();

        try {
            telegramClient.execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard){
        SendMessage message = SendMessage // Create a message object
                .builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramClient.execute(message); // Sending our message object to user
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}