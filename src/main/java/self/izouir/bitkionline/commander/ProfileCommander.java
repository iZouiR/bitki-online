package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.constants.BotMessageSenderConstants.*;
import static self.izouir.bitkionline.constants.ReplyMarkupConstants.*;
import static self.izouir.bitkionline.util.BotMessageSender.*;

@Component
public class ProfileCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    @Autowired
    public ProfileCommander(PlayerService playerService,
                            PlayerBotService playerBotService) {
        this.playerService = playerService;
        this.playerBotService = playerBotService;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "PROFILE_USERNAME_CHANGE" -> startUsernameChange(bot, chatId, messageId);
            case "PROFILE_USERNAME_CHANGE_CANCEL" -> cancelUsernameChange(bot, chatId, messageId);
            case "PROFILE_EGGS_REFRESH" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(EGGS_REFRESH_CONFIRMATION_MESSAGE)
                        .build();
                message.setReplyMarkup(generateEggsRefreshConfirmationReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_EGGS_REFRESH_YES" -> {
                playerService.refreshEggs(bot, chatId);
                sendEditMessageText(bot, chatId, messageId, EGGS_REFRESH_SUCCESS_MESSAGE);
            }
            case "PROFILE_DROP" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(PROFILE_DROP_CONFIRMATION_MESSAGE)
                        .build();
                message.setReplyMarkup(generateDropConfirmationReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_DROP_YES" -> {
                playerService.dropPlayerProfile(chatId);
                sendEditMessageText(bot, chatId, messageId, PROFILE_DROP_SUCCESS_MESSAGE);
            }
            case "PROFILE_EGGS_REFRESH_NO", "PROFILE_DROP_NO" -> {
                Player player = playerService.findByChatId(chatId);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(playerService.generatePlayerProfileInfo(player))
                        .build();
                message.setReplyMarkup(generateReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void profile(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                SendMessage message = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text(playerService.generatePlayerProfileInfo(player))
                        .build();
                message.setReplyMarkup(generateReplyMarkup());
                sendMessage(bot, message);
            } else {
                sendMessage(bot, chatId, PLAYER_NOT_REGISTERED_MESSAGE);
            }
        } else {
            sendMessage(bot, chatId, PLAYER_NOT_EXISTS_MESSAGE);
        }
    }

    private void startUsernameChange(DispatcherBot bot, Long chatId, Integer messageId) {
        Player player = playerService.findByChatId(chatId);
        playerBotService.setLastState(player, PlayerBotState.AWAIT_NEW_USERNAME);
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(AWAIT_NEW_USERNAME_MESSAGE)
                .build();
        message.setReplyMarkup(generateUsernameChangeReplyMarkup());
        sendEditMessageText(bot, message);
    }

    private void cancelUsernameChange(DispatcherBot bot, Long chatId, Integer messageId) {
        Player player = playerService.findByChatId(chatId);
        playerBotService.setLastState(player, PlayerBotState.NO_STATE);
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(playerService.generatePlayerProfileInfo(player))
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendEditMessageText(bot, message);
    }

    public boolean finishUsernameChange(DispatcherBot bot, Long chatId, String username) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastState() == PlayerBotState.AWAIT_NEW_USERNAME) {
                if (playerService.notExistsByUsernameIgnoreCase(username)) {
                    playerService.changeUsername(player, username);
                    sendMessage(bot, chatId, String.format(USERNAME_CHANGE_SUCCESS_MESSAGE, username));
                } else {
                    sendMessage(bot, chatId, String.format(PLAYER_ALREADY_EXISTS_MESSAGE, username));
                }
                return true;
            }
        }
        return false;
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> changeUsernameRow = new ArrayList<>();
        InlineKeyboardButton changeUsernameButton = new InlineKeyboardButton();
        changeUsernameButton.setText("Change username");
        changeUsernameButton.setCallbackData("PROFILE_USERNAME_CHANGE");
        changeUsernameRow.add(changeUsernameButton);

        List<InlineKeyboardButton> refreshEggsRow = new ArrayList<>();
        InlineKeyboardButton refreshEggsButton = new InlineKeyboardButton();
        refreshEggsButton.setText("Refresh eggs");
        refreshEggsButton.setCallbackData("PROFILE_EGGS_REFRESH");
        refreshEggsRow.add(refreshEggsButton);

        List<InlineKeyboardButton> dropProfileRow = new ArrayList<>();
        InlineKeyboardButton dropProfileButton = new InlineKeyboardButton();
        dropProfileButton.setText("Drop");
        dropProfileButton.setCallbackData("PROFILE_DROP");
        dropProfileRow.add(dropProfileButton);

        List<InlineKeyboardButton> closeProfileRow = new ArrayList<>();
        InlineKeyboardButton closeProfileButton = new InlineKeyboardButton();
        closeProfileButton.setText(CLOSE);
        closeProfileButton.setCallbackData("PROFILE_CLOSE");
        closeProfileRow.add(closeProfileButton);

        keyboard.add(changeUsernameRow);
        keyboard.add(refreshEggsRow);
        keyboard.add(dropProfileRow);
        keyboard.add(closeProfileRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateUsernameChangeReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> goBackRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(CANCEL);
        yesButton.setCallbackData("PROFILE_USERNAME_CHANGE_CANCEL");
        goBackRow.add(yesButton);

        keyboard.add(goBackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateEggsRefreshConfirmationReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmationRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(YES);
        yesButton.setCallbackData("PROFILE_EGGS_REFRESH_YES");
        confirmationRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(NO);
        noButton.setCallbackData("PROFILE_EGGS_REFRESH_NO");
        confirmationRow.add(noButton);

        keyboard.add(confirmationRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateDropConfirmationReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmationRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(YES);
        yesButton.setCallbackData("PROFILE_DROP_YES");
        confirmationRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(NO);
        noButton.setCallbackData("PROFILE_DROP_NO");
        confirmationRow.add(noButton);

        keyboard.add(confirmationRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
