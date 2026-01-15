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
import java.util.*;

public class AnimalBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final INaturalistApi api = new INaturalistApi();
    // Collezione di stringhe non duplicate
    private final Set<String> answeredQuizzes = new HashSet<>();

    public AnimalBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            if(message_text.startsWith("/start")){
                startMessage(update);
            } else if (message_text.startsWith("/help")){
                helpMessage(chat_id);
            } else if (message_text.startsWith("/animal")){
                animalMessage(chat_id, message_text);
            } else if (message_text.startsWith("/quiz")) {
                quizMessage(chat_id);
            } else if (message_text.startsWith("/score")){
                    scoreMessages(chat_id);
            } else if (message_text.startsWith("/history")) {
                historyMessage(chat_id);
            } else if (message_text.startsWith("/clearhistory")) {
                    clearHistoryMessage(chat_id);
            } else if (message_text.startsWith("/favourites")) {
                favouritesMessage(chat_id);
            } else if(message_text.startsWith("/stats")) {
                statsMessage(chat_id);
            } else {
                unknownMessage(chat_id);
            }
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
            } else if (call_data.startsWith("QUIZ_")) {
                // Controlla se questo quiz è già stato risposto
                String quizId = chat_id + "_" + update.getCallbackQuery().getMessage().getMessageId();

                if (answeredQuizzes.contains(quizId)) {
                    // Quiz già risposto, ignora
                    return;
                }

                // Segna il quiz come risposto
                answeredQuizzes.add(quizId);

                String payload = call_data.replace("QUIZ_", "");
                String[] parts = payload.split("\\|");

                int correctId = Integer.parseInt(parts[0]);
                int chosenId = Integer.parseInt(parts[1]);
                String correctName = parts[2];

                boolean correct = (correctId == chosenId);

                try {
                    if (correct) {
                        Database.getInstance().updateScore(telegram_id, +1);
                        sendMessage(chat_id, "Risposta corretta! +1 punto");
                    } else {
                        Database.getInstance().updateScore(telegram_id, -1);
                        sendMessage(chat_id, "Risposta errata! -1 punto\nLa risposta corretta era: " + correctName);
                    }
                } catch (SQLException e) {
                    sendMessage(chat_id, "Errore nel salvataggio del punteggio");
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
                "/quiz - Quiz: indovina l'animale dall'immagine\n" +
                "/score - Mostra il punteggio totale ottenuto nei quiz" +
                "/history - Visualizza le ultime ricerche\n" +
                "/clearhistory - Elimina la cronologia delle ricerche\n" +
                "/favourites - Mostra gli animali preferiti\n" +
                "/stats - Mostra gli animali più ricercati";
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

    private boolean isSimilarName(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        for (String part : a.split(" ")) {
            if (b.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private void quizMessage(long chatId){
        sendMessage(chatId, "Generazione del quiz in corso...\nAttendere...");
        Animal animal = api.getRandomAnimalWithImage();

        if(animal == null) {
            sendMessage(chatId, "Impossibile avviare il quiz");
            return;
        }

        String targetTaxon = animal.getIconicTaxon();
        List<Animal> options = new ArrayList<>();
        options.add(animal);

        // Tentativi per trovare simili
        int attempts = 0;
        while (options.size() < 4 && attempts < 20) {
            attempts++;
            Animal a = api.getRandomAnimal();
            if (a == null || a.getDisplayName() == null) continue;
            // L'animale deve appartenere alla stessa categoria principale
            if (!targetTaxon.equals(a.getIconicTaxon())) continue;

            if (!isSimilarName(animal.getDisplayName(), a.getDisplayName())) continue;
            // Evita duplicati
            if (options.stream().anyMatch(o -> o.getId() == a.getId())) continue;

            options.add(a);
        }

        // Se non trova abbastanza simili
        while (options.size() < 4) {
            Animal a = api.getRandomAnimal();
            if (a != null && options.stream().noneMatch(o -> o.getId() == a.getId())) {
                options.add(a);
            }
        }
        // Mischia l'ordine delle opzioni
        Collections.shuffle(options);

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Animal a : options) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(a.getDisplayName())
                            .callbackData("QUIZ_" + animal.getId() + "|" + a.getId() + "|" + animal.getDisplayName())
                            .build()
            ));
        }

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();

        sendPhoto(chatId, animal.getImageUrl(), "Quiz!\nChe animale è?", keyboard);
    }

    private void scoreMessages(long chatId) {
        try {
            int score = Database.getInstance().getScore(chatId);
            sendMessage(chatId, "Il tuo punteggio: " + score);
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

    private void statsMessage(long chatId){
        try {
            String stats = Database.getInstance().getTopSearchedAnimals();
            sendMessage(chatId, stats);
        } catch (SQLException e) {
            sendMessage(chatId, "Errore database");
        }
    }

    private void unknownMessage(long chatId){
        sendMessage(chatId, "Comando non riconosciuto\nScrivi /help per vedere i comandi disponibili");
    }

    private void sendMessage(long chatId, String text){
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
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
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}