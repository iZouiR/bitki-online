package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.commander.util.BotCommander.sendEditMessageText;
import static self.izouir.bitkionline.commander.util.BotCommander.sendMessage;

@Component
public class ProfileCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    private final EggCommander eggCommander;

    @Autowired
    public ProfileCommander(PlayerService playerService,
                            PlayerBotService playerBotService,
                            EggCommander eggCommander) {
        this.playerService = playerService;
        this.playerBotService = playerBotService;
        this.eggCommander = eggCommander;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        Player player = playerService.findByChatId(chatId);

        switch (callbackData) {
            case "CHANGE_USERNAME" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Ok, please. Enter your new username")
                        .build();

                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                playerBot.setLastBotState(BotState.CHANGE_USERNAME);
                playerBotService.save(playerBot);

                sendEditMessageText(bot, message);
            }
            case "REFRESH_EGGS" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Are you sure you want to drop your eggs and pick new ones?")
                        .build();
                message.setReplyMarkup(getRefreshEggsInlineKeyboardMarkup());
                sendEditMessageText(bot, message);
            }
            case "CONFIRM_REFRESH_EGGS" -> {
                eggCommander.deleteAllPlayersEggs(player);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("All your eggs were dropped and now will be refreshed")
                        .build();
                sendEditMessageText(bot, message);
                eggCommander.generateStarterEggs(bot, chatId, player);
            }
            case "DROP_PROFILE" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Are you sure you want to drop your whole profile?")
                        .build();
                message.setReplyMarkup(getDropProfileInlineKeyboardMarkup());
                sendEditMessageText(bot, message);
            }
            case "CONFIRM_DROP_PROFILE" -> {
                dropPlayer(player);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Your profile was deleted, now you can register another one - /start")
                        .build();
                sendEditMessageText(bot, message);
            }
            case "DENY_REFRESH_EGGS", "DENY_DROP_PROFILE" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(getPlayerInfo(player))
                        .build();
                message.setReplyMarkup(getProfileInlineKeyboardMarkup());
                sendEditMessageText(bot, message);
            }
        }
    }

    // register player when entering username
    // * true, if expected operation was registration
    // * false, if expected operation was not registration
    public boolean register(DispatcherBot bot, Long chatId, String username) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (playerBotService.existsByPlayerId(player.getId())) {
                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                if (playerBot.getLastBotState() == BotState.AWAIT_USERNAME) {
                    if (playerService.notExistsByUsernameIgnoreCase(username)) {
                        player.setUsername(username);
                        player.setRank(0L);
                        player.setRegisteredAt(Timestamp.from(Instant.now()));
                        playerService.save(player);

                        playerBot.setLastBotState(null);
                        playerBotService.save(playerBot);

                        sendMessage(bot, chatId, "Congratulations, you are now registered with username " + username + "!");

                        eggCommander.generateStarterEggs(bot, chatId, player);
                    } else {
                        sendMessage(bot, chatId, "Player with username " + username + " already exists, try another variant");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean changeUsername(DispatcherBot bot, Long chatId, String username) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (playerBotService.existsByPlayerId(player.getId())) {
                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                if (playerBot.getLastBotState() == BotState.CHANGE_USERNAME) {
                    if (playerService.notExistsByUsernameIgnoreCase(username)) {
                        player.setUsername(username);
                        playerService.save(player);

                        playerBot.setLastBotState(null);
                        playerBotService.save(playerBot);

                        sendMessage(bot, chatId, "Success, you are now named " + username);
                    } else {
                        sendMessage(bot, chatId, "Player with username " + username + " already exists, try another variant");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void dropPlayer(Player player) {
        eggCommander.deleteAllPlayersEggs(player);
        playerBotService.deleteByPlayerId(player.getId());
        playerService.delete(player);
    }

    public String getPlayerInfo(Player player) {
        return "Username: " + player.getUsername() + "\n" +
               "Rank: " + player.getRank() + " points\n" +
               "Registered: " + player.getRegisteredAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy [hh:mm]")) + "\n";
    }

    public InlineKeyboardMarkup getProfileInlineKeyboardMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> changeUsernameRow = new ArrayList<>();
        InlineKeyboardButton changeUsernameButton = new InlineKeyboardButton();
        changeUsernameButton.setText("Change username");
        changeUsernameButton.setCallbackData("CHANGE_USERNAME");
        changeUsernameRow.add(changeUsernameButton);

        List<InlineKeyboardButton> refreshEggsRow = new ArrayList<>();
        InlineKeyboardButton refreshEggsButton = new InlineKeyboardButton();
        refreshEggsButton.setText("Refresh eggs");
        refreshEggsButton.setCallbackData("REFRESH_EGGS");
        refreshEggsRow.add(refreshEggsButton);

        List<InlineKeyboardButton> dropProfileRow = new ArrayList<>();
        InlineKeyboardButton dropProfileButton = new InlineKeyboardButton();
        dropProfileButton.setText("Drop profile");
        dropProfileButton.setCallbackData("DROP_PROFILE");
        dropProfileRow.add(dropProfileButton);

        keyboard.add(changeUsernameRow);
        keyboard.add(refreshEggsRow);
        keyboard.add(dropProfileRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getRefreshEggsInlineKeyboardMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("CONFIRM_REFRESH_EGGS");
        confirmRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("DENY_REFRESH_EGGS");
        confirmRow.add(noButton);

        keyboard.add(confirmRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup getDropProfileInlineKeyboardMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> confirmRow = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("CONFIRM_DROP_PROFILE");
        confirmRow.add(yesButton);
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("DENY_DROP_PROFILE");
        confirmRow.add(noButton);

        keyboard.add(confirmRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
