package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.*;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.CLOSE_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.REFRESH_BUTTON_TEXT;

@RequiredArgsConstructor
@Component
public class RankCommander {
    private final PlayerService playerService;

    public void processCallbackQuery(final DispatcherBot bot, final Long chatId, final Integer messageId, final String callbackData) {
        switch (callbackData) {
            case "RANK_REFRESH" -> {
                final EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(playerService.generateRankInfo(chatId))
                        .build();
                message.setReplyMarkup(generateReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "RANK_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void rank(final DispatcherBot bot, final Long chatId) {
        final SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(playerService.generateRankInfo(chatId))
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendMessage(bot, message);
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> refreshRow = new ArrayList<>();
        final InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText(REFRESH_BUTTON_TEXT);
        refreshButton.setCallbackData("RANK_REFRESH");
        refreshRow.add(refreshButton);

        final List<InlineKeyboardButton> closeRow = new ArrayList<>();
        final InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText(CLOSE_BUTTON_TEXT);
        closeButton.setCallbackData("RANK_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(refreshRow);
        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
