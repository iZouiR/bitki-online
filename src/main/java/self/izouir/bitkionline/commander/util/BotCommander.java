package self.izouir.bitkionline.commander.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Path;

@Slf4j
@UtilityClass
public class BotCommander {
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

    public static void sendSticker(TelegramLongPollingBot bot, long chatId, Path image) {
        try {
            bot.execute(SendSticker.builder()
                            .chatId(String.valueOf(chatId))
                            .sticker(new InputFile(image.toFile()))
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
