package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.entity.player.PlayerStatistics;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;
import self.izouir.bitkionline.service.player.PlayerStatisticsService;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.*;
import static self.izouir.bitkionline.util.constant.MessageConstant.*;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.*;
import static self.izouir.bitkionline.util.constant.commander.ProfileCommanderConstant.*;

@RequiredArgsConstructor
@Component
public class ProfileCommander {
    private final PlayerStatisticsService playerStatisticsService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "PROFILE_STATISTICS" -> showStatistics(bot, chatId, messageId);
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
            case "PROFILE_STATISTICS_CANCEL", "PROFILE_EGGS_REFRESH_NO", "PROFILE_DROP_NO" -> {
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
                sendMessage(bot, chatId, PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE);
            }
        } else {
            sendMessage(bot, chatId, PLAYER_NOT_REGISTERED_MESSAGE);
        }
    }

    private void showStatistics(DispatcherBot bot, Long chatId, Integer messageId) {
        PlayerStatistics playerStatistics = playerStatisticsService.findByPlayerId(playerService.findByChatId(chatId).getId());
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(String.format(STATISTICS_MESSAGE,
                        playerStatisticsService.calculateWinRate(playerStatistics),
                        playerStatisticsService.calculateHeadAttackSuccessRate(playerStatistics),
                        playerStatisticsService.calculateSideAttackSuccessRate(playerStatistics),
                        playerStatisticsService.calculateAssAttackSuccessRate(playerStatistics),
                        playerStatistics.getTotalDamageDealt(),
                        playerStatistics.getTotalDamageTaken(),
                        playerStatistics.getTotalRankPointsEarned(),
                        playerStatistics.getTotalRankPointsLost(),
                        playerStatistics.getTotalEggsObtained(),
                        playerStatistics.getHolyEggsObtained(),
                        playerStatistics.getStrongEggsObtained(),
                        playerStatistics.getWeakEggsObtained()))
                .build();
        message.setReplyMarkup(generateStatisticsReplyMarkup());
        sendEditMessageText(bot, message);
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
                if (playerService.isAccurateUsername(username)) {
                    if (playerService.notExistsByUsernameIgnoreCase(username)) {
                        playerService.changeUsername(player, username);
                        sendMessage(bot, chatId, String.format(USERNAME_CHANGE_SUCCESS_MESSAGE, username));
                    } else {
                        sendMessage(bot, chatId, String.format(USERNAME_ALREADY_EXISTS_MESSAGE, username));
                    }
                } else {
                    sendMessage(bot, chatId, INCORRECT_USERNAME_FORMAT_MESSAGE);
                }
                return true;
            }
        }
        return false;
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> statisticsRow = new ArrayList<>();
        InlineKeyboardButton statisticsButton = new InlineKeyboardButton();
        statisticsButton.setText(STATISTICS_BUTTON_TEXT);
        statisticsButton.setCallbackData("PROFILE_STATISTICS");
        statisticsRow.add(statisticsButton);

        List<InlineKeyboardButton> changeUsernameRow = new ArrayList<>();
        InlineKeyboardButton changeUsernameButton = new InlineKeyboardButton();
        changeUsernameButton.setText(CHANGE_USERNAME_BUTTON_TEXT);
        changeUsernameButton.setCallbackData("PROFILE_USERNAME_CHANGE");
        changeUsernameRow.add(changeUsernameButton);

        List<InlineKeyboardButton> refreshEggsRow = new ArrayList<>();
        InlineKeyboardButton refreshEggsButton = new InlineKeyboardButton();
        refreshEggsButton.setText(REFRESH_EGGS_BUTTON_TEXT);
        refreshEggsButton.setCallbackData("PROFILE_EGGS_REFRESH");
        refreshEggsRow.add(refreshEggsButton);

        List<InlineKeyboardButton> dropProfileRow = new ArrayList<>();
        InlineKeyboardButton dropProfileButton = new InlineKeyboardButton();
        dropProfileButton.setText(DROP_PROFILE_BUTTON_TEXT);
        dropProfileButton.setCallbackData("PROFILE_DROP");
        dropProfileRow.add(dropProfileButton);

        List<InlineKeyboardButton> closeProfileRow = new ArrayList<>();
        InlineKeyboardButton closeProfileButton = new InlineKeyboardButton();
        closeProfileButton.setText(CLOSE_BUTTON_TEXT);
        closeProfileButton.setCallbackData("PROFILE_CLOSE");
        closeProfileRow.add(closeProfileButton);

        keyboard.add(statisticsRow);
        keyboard.add(changeUsernameRow);
        keyboard.add(refreshEggsRow);
        keyboard.add(dropProfileRow);
        keyboard.add(closeProfileRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateStatisticsReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PROFILE_STATISTICS_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateUsernameChangeReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PROFILE_USERNAME_CHANGE_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateEggsRefreshConfirmationReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmationRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(YES_BUTTON_TEXT);
        yesButton.setCallbackData("PROFILE_EGGS_REFRESH_YES");
        confirmationRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(NO_BUTTON_TEXT);
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
        yesButton.setText(YES_BUTTON_TEXT);
        yesButton.setCallbackData("PROFILE_DROP_YES");
        confirmationRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(NO_BUTTON_TEXT);
        noButton.setCallbackData("PROFILE_DROP_NO");
        confirmationRow.add(noButton);

        keyboard.add(confirmationRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
