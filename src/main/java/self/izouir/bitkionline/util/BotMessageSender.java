package self.izouir.bitkionline.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Path;

@Slf4j
@UtilityClass
public class BotMessageSender {
    public static void sendMessage(TelegramLongPollingBot bot, Long chatId, String text) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendMessage(TelegramLongPollingBot bot, SendMessage message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendEditMessageText(TelegramLongPollingBot bot, Long chatId, Integer messageId, String text) {
        try {
            bot.execute(EditMessageText.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(messageId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendEditMessageText(TelegramLongPollingBot bot, EditMessageText message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendSticker(TelegramLongPollingBot bot, Long chatId, Path image) {
        try {
            bot.execute(SendSticker.builder()
                    .chatId(String.valueOf(chatId))
                    .sticker(new InputFile(image.toFile()))
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendSticker(TelegramLongPollingBot bot, SendSticker sticker) {
        try {
            bot.execute(sticker);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void deleteMessage(TelegramLongPollingBot bot, Long chatId, Integer messageId) {
        try {
            bot.execute(DeleteMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(messageId)
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
