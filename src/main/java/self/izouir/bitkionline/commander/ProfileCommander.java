package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.*;

@Component
public class ProfileCommander {
    private final EggService eggService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    @Autowired
    public ProfileCommander(EggService eggService,
                            PlayerService playerService,
                            PlayerBotService playerBotService) {
        this.eggService = eggService;
        this.playerService = playerService;
        this.playerBotService = playerBotService;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "PROFILE_CHANGE_USERNAME" -> {
                Player player = playerService.findByChatId(chatId);
                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                playerBot.setLastBotState(BotState.AWAIT_NEW_USERNAME);
                playerBotService.save(playerBot);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Enter your new username")
                        .build();
                message.setReplyMarkup(generateProfileChangeUsernameReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_CHANGE_USERNAME_GO_BACK" -> {
                Player player = playerService.findByChatId(chatId);
                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                playerBot.setLastBotState(null);
                playerBotService.save(playerBot);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(generatePlayerProfileInfo(player))
                        .build();
                message.setReplyMarkup(generateProfileReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_REFRESH_EGGS" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Are you sure you want to delete all your eggs and generate new ones?")
                        .build();
                message.setReplyMarkup(generateProfileRefreshEggsReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_REFRESH_EGGS_YES" -> {
                Player player = playerService.findByChatId(chatId);
                eggService.unbindAllByOwner(player);
                sendEditMessageText(bot, chatId, messageId, "All your eggs were deleted and now will be refreshed");
                eggService.generateStartInventory(bot, player);
            }
            case "PROFILE_DROP" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Are you sure you want to drop your whole profile?")
                        .build();
                message.setReplyMarkup(generateProfileDropReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PROFILE_DROP_YES" -> {
                Player player = playerService.findByChatId(chatId);
                dropProfile(player);
                sendEditMessageText(bot, chatId, messageId, "Your profile was dropped");
            }
            case "PROFILE_REFRESH_EGGS_NO", "PROFILE_DROP_NO" -> {
                Player player = playerService.findByChatId(chatId);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(generatePlayerProfileInfo(player))
                        .build();
                message.setReplyMarkup(generateProfileReplyMarkup());
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
                        .text(generatePlayerProfileInfo(player))
                        .build();
                message.setReplyMarkup(generateProfileReplyMarkup());
                sendMessage(bot, message);
            } else {
                sendMessage(bot, chatId, "Finish registration before continuing");
            }
        } else {
            sendMessage(bot, chatId, "You aren't authorized - /start");
        }
    }

    private String generatePlayerProfileInfo(Player player) {
        return "Username: " + player.getUsername() + "\n" +
               "Rank: " + player.getRank() + " points\n" +
               "Registered: " + player.getRegisteredAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy [hh:mm]")) + "\n";
    }

    public boolean changeUsername(DispatcherBot bot, Long chatId, String username) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastBotState() == BotState.AWAIT_NEW_USERNAME) {
                if (playerService.notExistsByUsernameIgnoreCase(username)) {
                    player.setUsername(username);
                    playerService.save(player);
                    sendMessage(bot, chatId, "Success, you're now named " + username);
                    playerBot.setLastBotState(null);
                    playerBotService.save(playerBot);
                } else {
                    sendMessage(bot, chatId, "Player with username " + username + " already exists, try another variant");
                }
                return true;
            }
        }
        return false;
    }

    private void dropProfile(Player player) {
        eggService.unbindAllByOwner(player);
        playerBotService.deleteByPlayerId(player.getId());
        playerService.delete(player);
    }

    private InlineKeyboardMarkup generateProfileReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> changeUsernameRow = new ArrayList<>();
        InlineKeyboardButton changeUsernameButton = new InlineKeyboardButton();
        changeUsernameButton.setText("Change username");
        changeUsernameButton.setCallbackData("PROFILE_CHANGE_USERNAME");
        changeUsernameRow.add(changeUsernameButton);

        List<InlineKeyboardButton> refreshEggsRow = new ArrayList<>();
        InlineKeyboardButton refreshEggsButton = new InlineKeyboardButton();
        refreshEggsButton.setText("Refresh eggs");
        refreshEggsButton.setCallbackData("PROFILE_REFRESH_EGGS");
        refreshEggsRow.add(refreshEggsButton);

        List<InlineKeyboardButton> dropProfileRow = new ArrayList<>();
        InlineKeyboardButton dropProfileButton = new InlineKeyboardButton();
        dropProfileButton.setText("Drop");
        dropProfileButton.setCallbackData("PROFILE_DROP");
        dropProfileRow.add(dropProfileButton);

        List<InlineKeyboardButton> closeProfileRow = new ArrayList<>();
        InlineKeyboardButton closeProfileButton = new InlineKeyboardButton();
        closeProfileButton.setText("Close");
        closeProfileButton.setCallbackData("PROFILE_CLOSE");
        closeProfileRow.add(closeProfileButton);

        keyboard.add(changeUsernameRow);
        keyboard.add(refreshEggsRow);
        keyboard.add(dropProfileRow);
        keyboard.add(closeProfileRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateProfileChangeUsernameReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> goBackRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Go back");
        yesButton.setCallbackData("PROFILE_CHANGE_USERNAME_GO_BACK");
        goBackRow.add(yesButton);

        keyboard.add(goBackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateProfileRefreshEggsReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmationRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("PROFILE_REFRESH_EGGS_YES");
        confirmationRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("PROFILE_REFRESH_EGGS_NO");
        confirmationRow.add(noButton);

        keyboard.add(confirmationRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateProfileDropReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmationRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("PROFILE_DROP_YES");
        confirmationRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("PROFILE_DROP_NO");
        confirmationRow.add(noButton);

        keyboard.add(confirmationRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
