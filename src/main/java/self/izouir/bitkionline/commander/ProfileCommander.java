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

import static self.izouir.bitkionline.commander.util.BotCommander.*;

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
            case "REFRESH_RANK" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(getRankInfo(chatId))
                        .build();
                message.setReplyMarkup(getRankInlineKeyboardMarkup());
                sendEditMessageText(bot, message);
            }
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
                eggCommander.deleteAllPlayerEggs(player);
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
            case "CLOSE_RANK", "CLOSE_PROFILE" -> deleteMessage(bot, chatId, messageId);
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
        eggCommander.deleteAllPlayerEggs(player);
        playerBotService.deleteByPlayerId(player.getId());
        playerService.delete(player);
    }

    public String getRankInfo(Long chatId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getTopPlayersRankInfo());
        if (playerService.existsByChatId(chatId)) {
            stringBuilder.append("--------------------------------------------------------------\n");

            Player player = playerService.findByChatId(chatId);
            stringBuilder.append(getPlayerRankInfo(player));
        }
        return stringBuilder.toString();
    }

    private String getTopPlayersRankInfo() {
        StringBuilder stringBuilder = new StringBuilder();

        List<Player> topPlayers = playerService.findAllOrderedByRankDesc(3L);
        if (!topPlayers.isEmpty()) {
            for (int i = 0; i < topPlayers.size(); i++) {
                if (i == 0) {
                    stringBuilder.append("\uD83E\uDD47");
                }
                if (i == 1) {
                    stringBuilder.append("\uD83E\uDD48");
                }
                if (i == 2) {
                    stringBuilder.append("\uD83E\uDD49");
                }
                stringBuilder.append(topPlayers.get(i).getUsername());
                stringBuilder.append(" - ");
                stringBuilder.append(topPlayers.get(i).getRank());
                stringBuilder.append("\n");
            }
        } else {
            stringBuilder.append("Looks like there are no players at all, come back later");
        }

        return stringBuilder.toString();
    }

    private String getPlayerRankInfo(Player player) {
        StringBuilder stringBuilder = new StringBuilder();

        List<Player> players = playerService.findAllOrderedByRankDesc();
        long place = players.indexOf(player) + 1;

        stringBuilder.append("You are top-");
        stringBuilder.append(place);
        if (place == 1) {
            stringBuilder.append(" (\uD83E\uDD47)");
        }
        if (place == 2) {
            stringBuilder.append(" (\uD83E\uDD48)");
        }
        if (place == 3) {
            stringBuilder.append(" (\uD83E\uDD49)");
        }
        stringBuilder.append(" player with ");
        stringBuilder.append(player.getRank());
        stringBuilder.append(" points");

        return stringBuilder.toString();
    }

    public String getPlayerInfo(Player player) {
        return "Username: " + player.getUsername() + "\n" +
               "Rank: " + player.getRank() + " points\n" +
               "Registered: " + player.getRegisteredAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy [hh:mm]")) + "\n";
    }

    public InlineKeyboardMarkup getRankInlineKeyboardMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> refreshRankRow = new ArrayList<>();
        InlineKeyboardButton refreshRankButton = new InlineKeyboardButton();
        refreshRankButton.setText("Refresh");
        refreshRankButton.setCallbackData("REFRESH_RANK");
        refreshRankRow.add(refreshRankButton);

        List<InlineKeyboardButton> closeRankRow = new ArrayList<>();
        InlineKeyboardButton closeRankButton = new InlineKeyboardButton();
        closeRankButton.setText("Close");
        closeRankButton.setCallbackData("CLOSE_RANK");
        closeRankRow.add(closeRankButton);

        keyboard.add(refreshRankRow);
        keyboard.add(closeRankRow);
        markup.setKeyboard(keyboard);
        return markup;
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

        List<InlineKeyboardButton> closeProfileRow = new ArrayList<>();
        InlineKeyboardButton closeProfileButton = new InlineKeyboardButton();
        closeProfileButton.setText("Close");
        closeProfileButton.setCallbackData("CLOSE_PROFILE");
        closeProfileRow.add(closeProfileButton);

        keyboard.add(changeUsernameRow);
        keyboard.add(refreshEggsRow);
        keyboard.add(dropProfileRow);
        keyboard.add(closeProfileRow);
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
