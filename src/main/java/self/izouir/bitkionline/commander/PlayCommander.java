package self.izouir.bitkionline.commander;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static self.izouir.bitkionline.util.BotMessageSender.*;

@Slf4j
@Component
public class PlayCommander {
    private static final Queue<Player> AWAIT_MATCH_MAKING_PLAYERS_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<Player, Integer> AWAIT_MATCH_MAKING_PLAYERS = new ConcurrentHashMap<>();
    private static final Map<Player, PrivateBattle> AWAIT_CONNECTION_PRIVATE_BATTLES = new ConcurrentHashMap<>();
    private static final Map<Player, Integer> AWAIT_LINK_PLAYERS = new ConcurrentHashMap<>();
    private final MatchMakingBattleService matchMakingBattleService;
    private final PrivateBattleService privateBattleService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;
    private final BattleCommander battleCommander;

    @Autowired
    public PlayCommander(MatchMakingBattleService matchMakingBattleService,
                         PrivateBattleService privateBattleService,
                         PlayerService playerService,
                         PlayerBotService playerBotService,
                         BattleCommander battleCommander) {
        this.matchMakingBattleService = matchMakingBattleService;
        this.privateBattleService = privateBattleService;
        this.playerService = playerService;
        this.playerBotService = playerBotService;
        this.battleCommander = battleCommander;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "PLAY_MATCH_MAKING" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    player.setIsPlaying(true);
                    playerService.save(player);
                    try {
                        if (!AWAIT_MATCH_MAKING_PLAYERS_QUEUE.isEmpty()) {
                            Player opponent = AWAIT_MATCH_MAKING_PLAYERS_QUEUE.remove();
                            if (!Objects.equals(player.getId(), opponent.getId())) {
                                MatchMakingBattle matchMakingBattle = MatchMakingBattle.builder()
                                        .firstPlayer(player)
                                        .secondPlayer(opponent)
                                        .build();
                                matchMakingBattleService.save(matchMakingBattle);
                                Integer opponentMessageId = AWAIT_MATCH_MAKING_PLAYERS.remove(opponent);
                                deleteMessage(bot, chatId, messageId);
                                deleteMessage(bot, opponent.getChatId(), opponentMessageId);
                                battleCommander.startMatchMakingBattle(bot, chatId, matchMakingBattle);
                                battleCommander.startMatchMakingBattle(bot, opponent.getChatId(), matchMakingBattle);
                                return;
                            }
                        }
                    } catch (NoSuchElementException e) {
                        log.error(e.getMessage());
                    }
                    if (AWAIT_MATCH_MAKING_PLAYERS_QUEUE.contains(player)) {
                        Integer cancelMessageId = AWAIT_MATCH_MAKING_PLAYERS.remove(player);
                        deleteMessage(bot, chatId, cancelMessageId);
                    } else {
                        AWAIT_MATCH_MAKING_PLAYERS_QUEUE.offer(player);
                    }
                    AWAIT_MATCH_MAKING_PLAYERS.put(player, messageId);
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Waiting for an opponent...")
                            .build();
                    message.setReplyMarkup(generatePlayMatchMakingReplyMarkup());
                    sendEditMessageText(bot, message);
                    matchMakingBattleService.awaitConnection();
                    if (AWAIT_MATCH_MAKING_PLAYERS_QUEUE.contains(player) || AWAIT_MATCH_MAKING_PLAYERS.containsKey(player)) {
                        EditMessageText cancelMessageId = EditMessageText.builder()
                                .chatId(String.valueOf(chatId))
                                .messageId(messageId)
                                .text("Opponent to play with wasn't found")
                                .build();
                        cancelMessageId.setReplyMarkup(generatePlayOpponentNotFoundReplyMarkup());
                        sendEditMessageText(bot, cancelMessageId);
                    }
                    AWAIT_MATCH_MAKING_PLAYERS_QUEUE.remove(player);
                    AWAIT_MATCH_MAKING_PLAYERS.remove(player);
                } else {
                    sendEditMessageText(bot, chatId, messageId, "You're already playing, try again later");
                }
            }
            case "PLAY_MATCH_MAKING_GO_BACK" -> {
                Player player = playerService.findByChatId(chatId);
                player.setIsPlaying(false);
                playerService.save(player);
                AWAIT_MATCH_MAKING_PLAYERS_QUEUE.remove(player);
                AWAIT_MATCH_MAKING_PLAYERS.remove(player);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Choose the way to play")
                        .build();
                message.setReplyMarkup(generatePlayReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_PRIVATE_BATTLE" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Choose the strategy")
                            .build();
                    message.setReplyMarkup(generatePlayPrivateBattleReplyMarkup());
                    sendEditMessageText(bot, message);
                } else {
                    sendEditMessageText(bot, chatId, messageId, "You're already playing, try again later");
                }
            }
            case "PLAY_PRIVATE_BATTLE_CREATE_GAME" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    player.setIsPlaying(true);
                    playerService.save(player);
                    PrivateBattle privateBattle = privateBattleService.generatePrivateBattle(player);
                    privateBattleService.save(privateBattle);
                    AWAIT_CONNECTION_PRIVATE_BATTLES.put(player, privateBattle);
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Tell the opponent this link - " + privateBattle.getLink() +
                                  "\nIt will be alive for 120 seconds")
                            .build();
                    message.setReplyMarkup(generatePlayPrivateBattleCreateGameReplyMarkup());
                    sendEditMessageText(bot, message);
                    if (!privateBattleService.awaitConnection(privateBattle)) {
                        AWAIT_CONNECTION_PRIVATE_BATTLES.remove(player);
                        EditMessageText cancelMessageId = EditMessageText.builder()
                                .chatId(String.valueOf(chatId))
                                .messageId(messageId)
                                .text("Opponent to play with wasn't found")
                                .build();
                        cancelMessageId.setReplyMarkup(generatePlayOpponentNotFoundReplyMarkup());
                        sendEditMessageText(bot, cancelMessageId);
                        return;
                    }
                    deleteMessage(bot, chatId, messageId);
                    battleCommander.startPrivateBattle(bot, chatId, privateBattle);
                } else {
                    sendEditMessageText(bot, chatId, messageId, "You're already playing, try again later");
                }
            }
            case "PLAY_PRIVATE_BATTLE_CREATE_GAME_GO_BACK" -> {
                Player player = playerService.findByChatId(chatId);
                player.setIsPlaying(false);
                playerService.save(player);
                if (AWAIT_CONNECTION_PRIVATE_BATTLES.containsKey(player)) {
                    privateBattleService.delete(AWAIT_CONNECTION_PRIVATE_BATTLES.remove(player));
                }
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Choose the strategy")
                        .build();
                message.setReplyMarkup(generatePlayPrivateBattleReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_PRIVATE_BATTLE_JOIN_GAME" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    player.setIsPlaying(true);
                    playerService.save(player);
                    PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                    playerBot.setLastBotState(BotState.AWAIT_LINK);
                    playerBotService.save(playerBot);
                    AWAIT_LINK_PLAYERS.put(player, messageId);
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Enter the private battle link to continue")
                            .build();
                    message.setReplyMarkup(generatePlayPrivateBattleJoinGameReplyMarkup());
                    sendEditMessageText(bot, message);
                    privateBattleService.awaitConnection();
                    if (AWAIT_LINK_PLAYERS.containsKey(player)) {
                        AWAIT_LINK_PLAYERS.remove(player);
                        player.setIsPlaying(false);
                        playerService.save(player);
                        EditMessageText cancelMessageId = EditMessageText.builder()
                                .chatId(String.valueOf(chatId))
                                .messageId(messageId)
                                .text("Opponent to play with wasn't found")
                                .build();
                        cancelMessageId.setReplyMarkup(generatePlayOpponentNotFoundReplyMarkup());
                        sendEditMessageText(bot, cancelMessageId);
                    }
                } else {
                    sendEditMessageText(bot, chatId, messageId, "You're already playing, try again later");
                }
            }
            case "PLAY_PRIVATE_BATTLE_JOIN_GAME_GO_BACK" -> {
                Player player = playerService.findByChatId(chatId);
                player.setIsPlaying(false);
                playerService.save(player);
                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                playerBot.setLastBotState(null);
                playerBotService.save(playerBot);
                AWAIT_LINK_PLAYERS.remove(player);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Choose the strategy")
                        .build();
                message.setReplyMarkup(generatePlayPrivateBattleReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_PRIVATE_BATTLE_GO_BACK", "PLAY_OPPONENT_NOT_FOUND_OK" -> {
                Player player = playerService.findByChatId(chatId);
                player.setIsPlaying(false);
                playerService.save(player);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Choose the way to play")
                        .build();
                message.setReplyMarkup(generatePlayReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void play(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (!player.getIsPlaying()) {
                SendMessage message = SendMessage.builder()
                        .chatId(String.valueOf(chatId))
                        .text("Choose the way to play")
                        .build();
                message.setReplyMarkup(generatePlayReplyMarkup());
                sendMessage(bot, message);
            } else {
                sendMessage(bot, chatId, "You're already playing, try later");
            }
        } else {
            sendMessage(bot, chatId, "You aren't authorized - /start");
        }
    }

    public boolean connect(DispatcherBot bot, Long chatId, String link) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastBotState() == BotState.AWAIT_LINK) {
                if (privateBattleService.existsByLink(link)) {
                    PrivateBattle privateBattle = privateBattleService.findByLink(link);
                    if (privateBattle.getSecondPlayer() == null) {
                        privateBattle.setSecondPlayer(player);
                        privateBattleService.save(privateBattle);
                        playerBot.setLastBotState(null);
                        playerBotService.save(playerBot);
                        Integer messageId = AWAIT_LINK_PLAYERS.get(player);
                        deleteMessage(bot, chatId, messageId);
                        AWAIT_LINK_PLAYERS.remove(player);
                        battleCommander.startPrivateBattle(bot, chatId, privateBattle);
                    } else {
                        sendMessage(bot, chatId, "Private battle with link " + link + " is unavailable");
                    }
                    return true;
                } else {
                    sendMessage(bot, chatId, "Private battle with link " + link + " wasn't found");
                }
            }
        }
        return false;
    }

    private InlineKeyboardMarkup generatePlayReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> matchMakingRow = new ArrayList<>();
        InlineKeyboardButton matchMakingButton = new InlineKeyboardButton();
        matchMakingButton.setText("Match Making");
        matchMakingButton.setCallbackData("PLAY_MATCH_MAKING");
        matchMakingRow.add(matchMakingButton);

        List<InlineKeyboardButton> privateBattleRow = new ArrayList<>();
        InlineKeyboardButton privateBattleButton = new InlineKeyboardButton();
        privateBattleButton.setText("Private Battle");
        privateBattleButton.setCallbackData("PLAY_PRIVATE_BATTLE");
        privateBattleRow.add(privateBattleButton);

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText("Close");
        closeButton.setCallbackData("PLAY_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(matchMakingRow);
        keyboard.add(privateBattleRow);
        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePlayOpponentNotFoundReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> okRow = new ArrayList<>();
        InlineKeyboardButton okButton = new InlineKeyboardButton();
        okButton.setText("OK");
        okButton.setCallbackData("PLAY_OPPONENT_NOT_FOUND_OK");
        okRow.add(okButton);

        keyboard.add(okRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePlayMatchMakingReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> goBackRow = new ArrayList<>();
        InlineKeyboardButton goBackButton = new InlineKeyboardButton();
        goBackButton.setText("Go back");
        goBackButton.setCallbackData("PLAY_MATCH_MAKING_GO_BACK");
        goBackRow.add(goBackButton);

        keyboard.add(goBackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePlayPrivateBattleReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> createGameRow = new ArrayList<>();
        InlineKeyboardButton createGameButton = new InlineKeyboardButton();
        createGameButton.setText("Create game");
        createGameButton.setCallbackData("PLAY_PRIVATE_BATTLE_CREATE_GAME");
        createGameRow.add(createGameButton);

        List<InlineKeyboardButton> joinGameRow = new ArrayList<>();
        InlineKeyboardButton joinGameButton = new InlineKeyboardButton();
        joinGameButton.setText("Join game");
        joinGameButton.setCallbackData("PLAY_PRIVATE_BATTLE_JOIN_GAME");
        joinGameRow.add(joinGameButton);

        List<InlineKeyboardButton> goBackRow = new ArrayList<>();
        InlineKeyboardButton goBackButton = new InlineKeyboardButton();
        goBackButton.setText("Go back");
        goBackButton.setCallbackData("PLAY_PRIVATE_BATTLE_GO_BACK");
        goBackRow.add(goBackButton);

        keyboard.add(createGameRow);
        keyboard.add(joinGameRow);
        keyboard.add(goBackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePlayPrivateBattleCreateGameReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> goBackRow = new ArrayList<>();
        InlineKeyboardButton goBackButton = new InlineKeyboardButton();
        goBackButton.setText("Go back");
        goBackButton.setCallbackData("PLAY_PRIVATE_BATTLE_CREATE_GAME_GO_BACK");
        goBackRow.add(goBackButton);

        keyboard.add(goBackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePlayPrivateBattleJoinGameReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> goBackRow = new ArrayList<>();
        InlineKeyboardButton goBackButton = new InlineKeyboardButton();
        goBackButton.setText("Go back");
        goBackButton.setCallbackData("PLAY_PRIVATE_BATTLE_JOIN_GAME_GO_BACK");
        goBackRow.add(goBackButton);

        keyboard.add(goBackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
