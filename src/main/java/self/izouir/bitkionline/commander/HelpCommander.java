package self.izouir.bitkionline.commander;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.constants.ReplyMarkupConstants.CLOSE_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constants.commander.HelpCommanderConstants.HELP_MESSAGE;
import static self.izouir.bitkionline.util.BotMessageSender.deleteMessage;
import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;

@Component
public class HelpCommander {
    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        if (callbackData.equals("HELP_CLOSE")) {
            deleteMessage(bot, chatId, messageId);
        }
    }

    public void help(DispatcherBot bot, Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(HELP_MESSAGE)
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendMessage(bot, message);
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText(CLOSE_BUTTON_TEXT);
        closeButton.setCallbackData("HELP_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
