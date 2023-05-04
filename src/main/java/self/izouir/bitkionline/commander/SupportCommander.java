package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.service.SupportMessageService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.deleteMessage;
import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;
import static self.izouir.bitkionline.util.constant.MessageConstant.PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE;
import static self.izouir.bitkionline.util.constant.MessageConstant.PLAYER_NOT_REGISTERED_MESSAGE;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.CLOSE_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constant.commander.SupportCommanderConstant.*;

@RequiredArgsConstructor
@Component
public class SupportCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;
    private final SupportMessageService supportMessageService;

    public void processCallbackQuery(final DispatcherBot bot, final Long chatId, final Integer messageId, final String callbackData) {
        if (callbackData.equals("SUPPORT_CLOSE")) {
            final Player player = playerService.findByChatId(chatId);
            playerBotService.applyLastState(player, PlayerBotState.NO_STATE);
            deleteMessage(bot, chatId, messageId);
        }
    }

    public void support(final DispatcherBot bot, final Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            final Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                playerBotService.applyLastState(player, PlayerBotState.AWAIT_SUPPORT_MESSAGE);
                final SendMessage message = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text(AWAIT_SUPPORT_MESSAGE_MESSAGE)
                        .build();
                message.setReplyMarkup(generateReplyMarkup());
                sendMessage(bot, message);
            } else {
                sendMessage(bot, chatId, PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE);
            }
        } else {
            sendMessage(bot, chatId, PLAYER_NOT_REGISTERED_MESSAGE);
        }
    }

    public boolean publishSupportMessage(final DispatcherBot bot, final Long chatId, final String message) {
        if (playerService.existsByChatId(chatId)) {
            final Player player = playerService.findByChatId(chatId);
            final PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastState() == PlayerBotState.AWAIT_SUPPORT_MESSAGE) {
                if (supportMessageService.isAccurateSupportMessage(message)) {
                    supportMessageService.publishSupportMessage(player, message);
                    sendMessage(bot, chatId, SUPPORT_MESSAGE_PUBLICATION_SUCCESS_MESSAGE);
                } else {
                    sendMessage(bot, chatId, INCORRECT_SUPPORT_MESSAGE_FORMAT_MESSAGE);
                }
                return true;
            }
        }
        return false;
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> closeRow = new ArrayList<>();
        final InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText(CLOSE_BUTTON_TEXT);
        closeButton.setCallbackData("SUPPORT_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
