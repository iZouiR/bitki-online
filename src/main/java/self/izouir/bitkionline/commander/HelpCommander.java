package self.izouir.bitkionline.commander;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.deleteMessage;
import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.CLOSE_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constant.commander.HelpCommanderConstant.HELP_MESSAGE;

@Component
public class HelpCommander {
    public void processCallbackQuery(final DispatcherBot bot, final Long chatId, final Integer messageId, final String callbackData) {
        if (callbackData.equals("HELP_CLOSE")) {
            deleteMessage(bot, chatId, messageId);
        }
    }

    public void help(final DispatcherBot bot, final Long chatId) {
        final SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(HELP_MESSAGE)
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendMessage(bot, message);
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> closeRow = new ArrayList<>();
        final InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText(CLOSE_BUTTON_TEXT);
        closeButton.setCallbackData("HELP_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
