package self.izouir.bitkionline.commander;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PlayerBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static self.izouir.bitkionline.util.BotMessageSender.*;
import static self.izouir.bitkionline.util.constants.MessageConstants.*;
import static self.izouir.bitkionline.util.constants.ReplyMarkupConstants.CANCEL_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constants.ReplyMarkupConstants.CLOSE_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constants.commander.PlayCommanderConstants.*;

@Slf4j
@Component
public class PlayCommander {
    private static final ExecutorService EXECUTOR = Executors.newWorkStealingPool();
    private static final Queue<Long> MATCH_MAKING_CHATS_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<Long, Integer> MATCH_MAKING_CHATS_TO_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<Long, String> PRIVATE_BATTLE_CHATS_TO_LINKS = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> PRIVATE_BATTLE_CHATS_TO_MESSAGES = new ConcurrentHashMap<>();
    private final PlayerBattleService playerBattleService;
    private final MatchMakingBattleService matchMakingBattleService;
    private final PrivateBattleService privateBattleService;
    private final EggService eggService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;
    private final BattleCommander battleCommander;

    @Autowired
    public PlayCommander(PlayerBattleService playerBattleService,
                         MatchMakingBattleService matchMakingBattleService,
                         PrivateBattleService privateBattleService,
                         EggService eggService,
                         PlayerService playerService,
                         PlayerBotService playerBotService,
                         BattleCommander battleCommander) {
        this.playerBattleService = playerBattleService;
        this.matchMakingBattleService = matchMakingBattleService;
        this.privateBattleService = privateBattleService;
        this.eggService = eggService;
        this.playerService = playerService;
        this.playerBotService = playerBotService;
        this.battleCommander = battleCommander;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "PLAY_MATCH_MAKING" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    playerService.setIsPlaying(chatId, true);
                    if (!MATCH_MAKING_CHATS_QUEUE.isEmpty() && !MATCH_MAKING_CHATS_QUEUE.contains(chatId)) {
                        startMatchMakingBattle(bot, chatId, messageId);
                        return;
                    }
                    awaitMatchMakingBattleOpponent(bot, chatId, messageId);
                } else {
                    sendEditMessageText(bot, chatId, messageId, PLAYER_ALREADY_PLAYING_MESSAGE);
                }
            }
            case "PLAY_MATCH_MAKING_CANCEL" -> {
                cancelMatchMakingBattle(bot, chatId, messageId);
                playerService.setIsPlaying(chatId, false);
            }
            case "PLAY_PRIVATE_BATTLE" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text(CHOOSING_PRIVATE_BATTLE_TYPE_MESSAGE)
                            .build();
                    message.setReplyMarkup(generatePrivateBattleReplyMarkup());
                    sendEditMessageText(bot, message);
                } else {
                    sendEditMessageText(bot, chatId, messageId, PLAYER_ALREADY_PLAYING_MESSAGE);
                }
            }
            case "PLAY_PRIVATE_BATTLE_CREATE_GAME" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    playerService.setIsPlaying(chatId, true);
                    PrivateBattle privateBattle = privateBattleService.createByFirstPlayer(player);
                    PRIVATE_BATTLE_CHATS_TO_LINKS.put(chatId, privateBattle.getLink());
                    awaitPrivateBattleOpponent(bot, chatId, messageId, privateBattle.getLink());
                } else {
                    sendEditMessageText(bot, chatId, messageId, PLAYER_ALREADY_PLAYING_MESSAGE);
                }
            }
            case "PLAY_PRIVATE_BATTLE_CREATE_GAME_CANCEL" -> {
                cancelPrivateBattle(bot, chatId, messageId);
                playerService.setIsPlaying(chatId, false);
            }
            case "PLAY_PRIVATE_BATTLE_JOIN_GAME" -> {
                Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    playerService.setIsPlaying(chatId, true);
                    playerBotService.setLastState(player, PlayerBotState.AWAIT_PRIVATE_BATTLE_LINK);
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text(AWAIT_PRIVATE_BATTLE_LINK_MESSAGE)
                            .build();
                    message.setReplyMarkup(generatePrivateBattleJoinGameReplyMarkup());
                    sendEditMessageText(bot, message);
                } else {
                    sendEditMessageText(bot, chatId, messageId, PLAYER_ALREADY_PLAYING_MESSAGE);
                }
            }
            case "PLAY_PRIVATE_BATTLE_JOIN_GAME_CANCEL" -> {
                Player player = playerService.findByChatId(chatId);
                playerBotService.setLastState(player, PlayerBotState.NO_STATE);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(CHOOSING_PRIVATE_BATTLE_TYPE_MESSAGE)
                        .build();
                message.setReplyMarkup(generatePrivateBattleReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_PRIVATE_BATTLE_CANCEL", "PLAY_OPPONENT_NOT_FOUND_CANCEL" -> {
                playerService.setIsPlaying(chatId, false);
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(CHOOSING_BATTLE_TYPE_MESSAGE)
                        .build();
                message.setReplyMarkup(generateReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void play(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                if (!player.getIsPlaying()) {
                    List<Egg> notCrackedInventory = eggService.findAllByOwnerWhereIsNotCracked(player);
                    if (!notCrackedInventory.isEmpty()) {
                        SendMessage message = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text(CHOOSING_BATTLE_TYPE_MESSAGE)
                                .build();
                        message.setReplyMarkup(generateReplyMarkup());
                        sendMessage(bot, message);
                    } else {
                        sendMessage(bot, chatId, EMPTY_INVENTORY_MESSAGE);
                    }
                } else {
                    sendMessage(bot, chatId, PLAYER_ALREADY_PLAYING_MESSAGE);
                }
            } else {
                sendMessage(bot, chatId, PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE);
            }
        } else {
            sendMessage(bot, chatId, PLAYER_NOT_REGISTERED_MESSAGE);
        }
    }

    private void startMatchMakingBattle(DispatcherBot bot, Long chatId, Integer messageId) {
        Player player = playerService.findByChatId(chatId);
        Player opponent = playerService.findByChatId(MATCH_MAKING_CHATS_QUEUE.poll());
        MatchMakingBattle matchMakingBattle = matchMakingBattleService.create(player, opponent);
        Integer opponentMessageId = MATCH_MAKING_CHATS_TO_MESSAGES.remove(opponent.getChatId());
        deleteMessage(bot, chatId, messageId);
        deleteMessage(bot, opponent.getChatId(), opponentMessageId);
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, chatId, matchMakingBattle.getPlayerBattle()));
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, opponent.getChatId(), matchMakingBattle.getPlayerBattle()));
    }

    private void awaitMatchMakingBattleOpponent(DispatcherBot bot, Long chatId, Integer messageId) {
        MATCH_MAKING_CHATS_QUEUE.offer(chatId);
        MATCH_MAKING_CHATS_TO_MESSAGES.put(chatId, messageId);
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(AWAIT_MATCH_MAKING_BATTLE_OPPONENT_MESSAGE)
                .build();
        message.setReplyMarkup(generateMatchMakingReplyMarkup());
        sendEditMessageText(bot, message);
        try {
            int counter = 0;
            while (MATCH_MAKING_CHATS_QUEUE.contains(chatId) && MATCH_MAKING_CHATS_TO_MESSAGES.containsKey(chatId)) {
                Thread.sleep(1000);
                counter++;
                if (counter >= AWAIT_OPPONENT_SECONDS) {
                    cancelMatchMakingBattleOnTime(bot, chatId, messageId);
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private void cancelMatchMakingBattle(DispatcherBot bot, Long chatId, Integer messageId) {
        MATCH_MAKING_CHATS_QUEUE.remove(chatId);
        MATCH_MAKING_CHATS_TO_MESSAGES.remove(chatId);
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(CHOOSING_BATTLE_TYPE_MESSAGE)
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendEditMessageText(bot, message);
    }

    private void cancelMatchMakingBattleOnTime(DispatcherBot bot, Long chatId, Integer messageId) {
        MATCH_MAKING_CHATS_QUEUE.remove(chatId);
        MATCH_MAKING_CHATS_TO_MESSAGES.remove(chatId);
        EditMessageText cancelMessage = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(OPPONENT_NOT_FOUND_MESSAGE)
                .build();
        cancelMessage.setReplyMarkup(generateOpponentNotFoundReplyMarkup());
        sendEditMessageText(bot, cancelMessage);
    }

    private void startPrivateBattle(DispatcherBot bot, Long chatId, PlayerBattle playerBattle) {
        Player player = playerService.findByChatId(chatId);
        playerBattleService.setSecondPlayer(playerBattle, player);
        playerBotService.setLastState(player, PlayerBotState.NO_STATE);
        Player opponent = playerBattle.getFirstPlayer();
        Integer opponentMessageId = PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(opponent.getChatId());
        deleteMessage(bot, opponent.getChatId(), opponentMessageId);
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, chatId, playerBattle));
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, opponent.getChatId(), playerBattle));
    }

    private void awaitPrivateBattleOpponent(DispatcherBot bot, Long chatId, Integer messageId, String privateBattleLink) {
        PRIVATE_BATTLE_CHATS_TO_MESSAGES.put(chatId, messageId);
        MessageEntity linkEntity = MessageEntity.builder()
                .text(String.format(AWAIT_PRIVATE_BATTLE_OPPONENT_MESSAGE, privateBattleLink))
                .type(PRIVATE_BATTLE_LINK_MESSAGE_ENTITY_TYPE)
                .offset(AWAIT_PRIVATE_BATTLE_OPPONENT_PRE_LINK_MESSAGE.length())
                .length(privateBattleLink.length())
                .build();
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(String.format(AWAIT_PRIVATE_BATTLE_OPPONENT_MESSAGE, privateBattleLink))
                .entities(List.of(linkEntity))
                .build();
        message.setReplyMarkup(generatePrivateBattleCreateGameReplyMarkup());
        sendEditMessageText(bot, message);
        try {
            int counter = 0;
            while (PRIVATE_BATTLE_CHATS_TO_MESSAGES.containsKey(chatId)) {
                Thread.sleep(1000);
                counter++;
                if (counter >= AWAIT_OPPONENT_SECONDS) {
                    cancelPrivateBattleOnTime(bot, chatId, messageId);
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private void cancelPrivateBattle(DispatcherBot bot, Long chatId, Integer messageId) {
        PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(chatId);
        String privateBattleLink = PRIVATE_BATTLE_CHATS_TO_LINKS.remove(chatId);
        privateBattleService.deleteByLink(privateBattleLink);
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(CHOOSING_PRIVATE_BATTLE_TYPE_MESSAGE)
                .build();
        message.setReplyMarkup(generatePrivateBattleReplyMarkup());
        sendEditMessageText(bot, message);
    }

    private void cancelPrivateBattleOnTime(DispatcherBot bot, Long chatId, Integer messageId) {
        PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(chatId);
        String privateBattleLink = PRIVATE_BATTLE_CHATS_TO_LINKS.remove(chatId);
        privateBattleService.deleteByLink(privateBattleLink);
        EditMessageText cancelMessage = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(OPPONENT_NOT_FOUND_MESSAGE)
                .build();
        cancelMessage.setReplyMarkup(generateOpponentNotFoundReplyMarkup());
        sendEditMessageText(bot, cancelMessage);
    }

    public boolean connectToPrivateBattle(DispatcherBot bot, Long chatId, String link) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastState() == PlayerBotState.AWAIT_PRIVATE_BATTLE_LINK) {
                if (privateBattleService.existsByLink(link)) {
                    PrivateBattle privateBattle = privateBattleService.findByLink(link);
                    PlayerBattle playerBattle = privateBattle.getPlayerBattle();
                    if (playerBattle.getSecondPlayer() == null) {
                        startPrivateBattle(bot, chatId, playerBattle);
                    } else {
                        sendMessage(bot, chatId, String.format(PRIVATE_BATTLE_NOT_AVAILABLE_MESSAGE, link));
                    }
                } else {
                    sendMessage(bot, chatId, String.format(PRIVATE_BATTLE_NOT_FOUND_MESSAGE, link));
                }
                return true;
            }
        }
        return false;
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> matchMakingRow = new ArrayList<>();
        InlineKeyboardButton matchMakingButton = new InlineKeyboardButton();
        matchMakingButton.setText(MATCH_MAKING_BUTTON_TEXT);
        matchMakingButton.setCallbackData("PLAY_MATCH_MAKING");
        matchMakingRow.add(matchMakingButton);

        List<InlineKeyboardButton> privateBattleRow = new ArrayList<>();
        InlineKeyboardButton privateBattleButton = new InlineKeyboardButton();
        privateBattleButton.setText(PRIVATE_BUTTON_TEXT);
        privateBattleButton.setCallbackData("PLAY_PRIVATE_BATTLE");
        privateBattleRow.add(privateBattleButton);

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText(CLOSE_BUTTON_TEXT);
        closeButton.setCallbackData("PLAY_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(matchMakingRow);
        keyboard.add(privateBattleRow);
        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateOpponentNotFoundReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_OPPONENT_NOT_FOUND_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateMatchMakingReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_MATCH_MAKING_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePrivateBattleReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> createGameRow = new ArrayList<>();
        InlineKeyboardButton createGameButton = new InlineKeyboardButton();
        createGameButton.setText(CREATE_GAME_BUTTON_TEXT);
        createGameButton.setCallbackData("PLAY_PRIVATE_BATTLE_CREATE_GAME");
        createGameRow.add(createGameButton);

        List<InlineKeyboardButton> joinGameRow = new ArrayList<>();
        InlineKeyboardButton joinGameButton = new InlineKeyboardButton();
        joinGameButton.setText(JOIN_GAME_BUTTON_TEXT);
        joinGameButton.setCallbackData("PLAY_PRIVATE_BATTLE_JOIN_GAME");
        joinGameRow.add(joinGameButton);

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_PRIVATE_BATTLE_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(createGameRow);
        keyboard.add(joinGameRow);
        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePrivateBattleCreateGameReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_PRIVATE_BATTLE_CREATE_GAME_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePrivateBattleJoinGameReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_PRIVATE_BATTLE_JOIN_GAME_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
