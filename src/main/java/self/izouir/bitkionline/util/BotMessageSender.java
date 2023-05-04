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
    public static void sendMessage(final TelegramLongPollingBot bot, final Long chatId, final String text) {
        try {
            bot.executeAsync(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .build());
        } catch (final TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendMessage(final TelegramLongPollingBot bot, final SendMessage message) {
        try {
            bot.executeAsync(message);
        } catch (final TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendEditMessageText(final TelegramLongPollingBot bot, final Long chatId, final Integer messageId, final String text) {
        try {
            bot.executeAsync(EditMessageText.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(messageId)
                    .text(text)
                    .build());
        } catch (final TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendEditMessageText(final TelegramLongPollingBot bot, final EditMessageText message) {
        try {
            bot.executeAsync(message);
        } catch (final TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public static void sendSticker(final TelegramLongPollingBot bot, final Long chatId, final Path image) {
        bot.executeAsync(SendSticker.builder()
                .chatId(String.valueOf(chatId))
                .sticker(new InputFile(image.toFile()))
                .build());
    }

    public static void sendSticker(final TelegramLongPollingBot bot, final SendSticker sticker) {
        bot.executeAsync(sticker);
    }

    public static void deleteMessage(final TelegramLongPollingBot bot, final Long chatId, final Integer messageId) {
        try {
            bot.executeAsync(DeleteMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(messageId)
                    .build());
        } catch (final TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
