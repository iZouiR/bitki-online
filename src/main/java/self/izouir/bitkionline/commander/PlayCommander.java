package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static self.izouir.bitkionline.util.constant.MessageConstant.*;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.CANCEL_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.CLOSE_BUTTON_TEXT;
import static self.izouir.bitkionline.util.constant.commander.PlayCommanderConstant.*;

@RequiredArgsConstructor
@Component
public class PlayCommander {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayCommander.class);
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

    public void processCallbackQuery(final DispatcherBot bot, final Long chatId, final Integer messageId, final String callbackData) {
        switch (callbackData) {
            case "PLAY_MATCH_MAKING" -> {
                final Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    playerService.applyIsPlaying(chatId, true);
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
                playerService.applyIsPlaying(chatId, false);
            }
            case "PLAY_PRIVATE_BATTLE" -> {
                final Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    final EditMessageText message = EditMessageText.builder()
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
                final Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    playerService.applyIsPlaying(chatId, true);
                    final PrivateBattle privateBattle = privateBattleService.createByFirstPlayer(player);
                    PRIVATE_BATTLE_CHATS_TO_LINKS.put(chatId, privateBattle.getLink());
                    awaitPrivateBattleOpponent(bot, chatId, messageId, privateBattle.getLink());
                } else {
                    sendEditMessageText(bot, chatId, messageId, PLAYER_ALREADY_PLAYING_MESSAGE);
                }
            }
            case "PLAY_PRIVATE_BATTLE_CREATE_GAME_CANCEL" -> {
                cancelPrivateBattle(bot, chatId, messageId);
                playerService.applyIsPlaying(chatId, false);
            }
            case "PLAY_PRIVATE_BATTLE_JOIN_GAME" -> {
                final Player player = playerService.findByChatId(chatId);
                if (!player.getIsPlaying()) {
                    playerService.applyIsPlaying(chatId, true);
                    playerBotService.applyLastState(player, PlayerBotState.AWAIT_PRIVATE_BATTLE_LINK);
                    final EditMessageText message = EditMessageText.builder()
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
                final Player player = playerService.findByChatId(chatId);
                playerBotService.applyLastState(player, PlayerBotState.NO_STATE);
                final EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(CHOOSING_PRIVATE_BATTLE_TYPE_MESSAGE)
                        .build();
                message.setReplyMarkup(generatePrivateBattleReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "PLAY_PRIVATE_BATTLE_CANCEL", "PLAY_OPPONENT_NOT_FOUND_CANCEL" -> {
                playerService.applyIsPlaying(chatId, false);
                final EditMessageText message = EditMessageText.builder()
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

    public void play(final DispatcherBot bot, final Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            final Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                if (!player.getIsPlaying()) {
                    final List<Egg> notCrackedInventory = eggService.findAllByOwnerWhereIsNotCracked(player);
                    if (!notCrackedInventory.isEmpty()) {
                        final SendMessage message = SendMessage.builder()
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

    private void startMatchMakingBattle(final DispatcherBot bot, final Long chatId, final Integer messageId) {
        final Player player = playerService.findByChatId(chatId);
        final Player opponent = playerService.findByChatId(MATCH_MAKING_CHATS_QUEUE.poll());
        final MatchMakingBattle matchMakingBattle = matchMakingBattleService.create(player, opponent);
        final Integer opponentMessageId = MATCH_MAKING_CHATS_TO_MESSAGES.remove(opponent.getChatId());
        deleteMessage(bot, chatId, messageId);
        deleteMessage(bot, opponent.getChatId(), opponentMessageId);
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, chatId, matchMakingBattle.getPlayerBattle()));
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, opponent.getChatId(), matchMakingBattle.getPlayerBattle()));
    }

    private void awaitMatchMakingBattleOpponent(final DispatcherBot bot, final Long chatId, final Integer messageId) {
        MATCH_MAKING_CHATS_QUEUE.offer(chatId);
        MATCH_MAKING_CHATS_TO_MESSAGES.put(chatId, messageId);
        final EditMessageText message = EditMessageText.builder()
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
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void cancelMatchMakingBattle(final DispatcherBot bot, final Long chatId, final Integer messageId) {
        MATCH_MAKING_CHATS_QUEUE.remove(chatId);
        MATCH_MAKING_CHATS_TO_MESSAGES.remove(chatId);
        final EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(CHOOSING_BATTLE_TYPE_MESSAGE)
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendEditMessageText(bot, message);
    }

    private void cancelMatchMakingBattleOnTime(final DispatcherBot bot, final Long chatId, final Integer messageId) {
        MATCH_MAKING_CHATS_QUEUE.remove(chatId);
        MATCH_MAKING_CHATS_TO_MESSAGES.remove(chatId);
        final EditMessageText cancelMessage = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(OPPONENT_NOT_FOUND_MESSAGE)
                .build();
        cancelMessage.setReplyMarkup(generateOpponentNotFoundReplyMarkup());
        sendEditMessageText(bot, cancelMessage);
    }

    private void startPrivateBattle(final DispatcherBot bot, final Long chatId, final PlayerBattle playerBattle) {
        final Player player = playerService.findByChatId(chatId);
        playerBattleService.applySecondPlayer(playerBattle, player);
        playerBotService.applyLastState(player, PlayerBotState.NO_STATE);
        final Player opponent = playerBattle.getFirstPlayer();
        final Integer opponentMessageId = PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(opponent.getChatId());
        deleteMessage(bot, opponent.getChatId(), opponentMessageId);
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, chatId, playerBattle));
        EXECUTOR.execute(() -> battleCommander.startBattle(bot, opponent.getChatId(), playerBattle));
    }

    private void awaitPrivateBattleOpponent(final DispatcherBot bot, final Long chatId, final Integer messageId, final String privateBattleLink) {
        PRIVATE_BATTLE_CHATS_TO_MESSAGES.put(chatId, messageId);
        final MessageEntity linkEntity = MessageEntity.builder()
                .text(String.format(AWAIT_PRIVATE_BATTLE_OPPONENT_MESSAGE, privateBattleLink))
                .type(PRIVATE_BATTLE_LINK_MESSAGE_ENTITY_TYPE)
                .offset(AWAIT_PRIVATE_BATTLE_OPPONENT_PRE_LINK_MESSAGE.length())
                .length(privateBattleLink.length())
                .build();
        final EditMessageText message = EditMessageText.builder()
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
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void cancelPrivateBattle(final DispatcherBot bot, final Long chatId, final Integer messageId) {
        PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(chatId);
        final String privateBattleLink = PRIVATE_BATTLE_CHATS_TO_LINKS.remove(chatId);
        privateBattleService.deleteByLink(privateBattleLink);
        final EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(CHOOSING_PRIVATE_BATTLE_TYPE_MESSAGE)
                .build();
        message.setReplyMarkup(generatePrivateBattleReplyMarkup());
        sendEditMessageText(bot, message);
    }

    private void cancelPrivateBattleOnTime(final DispatcherBot bot, final Long chatId, final Integer messageId) {
        PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(chatId);
        final String privateBattleLink = PRIVATE_BATTLE_CHATS_TO_LINKS.remove(chatId);
        privateBattleService.deleteByLink(privateBattleLink);
        final EditMessageText cancelMessage = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(OPPONENT_NOT_FOUND_MESSAGE)
                .build();
        cancelMessage.setReplyMarkup(generateOpponentNotFoundReplyMarkup());
        sendEditMessageText(bot, cancelMessage);
    }

    public boolean connectToPrivateBattle(final DispatcherBot bot, final Long chatId, final String link) {
        if (playerService.existsByChatId(chatId)) {
            final Player player = playerService.findByChatId(chatId);
            final PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastState() == PlayerBotState.AWAIT_PRIVATE_BATTLE_LINK) {
                if (privateBattleService.existsByLink(link)) {
                    final PrivateBattle privateBattle = privateBattleService.findByLink(link);
                    final PlayerBattle playerBattle = privateBattle.getPlayerBattle();
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
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> matchMakingRow = new ArrayList<>();
        final InlineKeyboardButton matchMakingButton = new InlineKeyboardButton();
        matchMakingButton.setText(MATCH_MAKING_BUTTON_TEXT);
        matchMakingButton.setCallbackData("PLAY_MATCH_MAKING");
        matchMakingRow.add(matchMakingButton);

        final List<InlineKeyboardButton> privateBattleRow = new ArrayList<>();
        final InlineKeyboardButton privateBattleButton = new InlineKeyboardButton();
        privateBattleButton.setText(PRIVATE_BUTTON_TEXT);
        privateBattleButton.setCallbackData("PLAY_PRIVATE_BATTLE");
        privateBattleRow.add(privateBattleButton);

        final List<InlineKeyboardButton> closeRow = new ArrayList<>();
        final InlineKeyboardButton closeButton = new InlineKeyboardButton();
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
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        final InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_OPPONENT_NOT_FOUND_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateMatchMakingReplyMarkup() {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        final InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_MATCH_MAKING_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePrivateBattleReplyMarkup() {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> createGameRow = new ArrayList<>();
        final InlineKeyboardButton createGameButton = new InlineKeyboardButton();
        createGameButton.setText(CREATE_GAME_BUTTON_TEXT);
        createGameButton.setCallbackData("PLAY_PRIVATE_BATTLE_CREATE_GAME");
        createGameRow.add(createGameButton);

        final List<InlineKeyboardButton> joinGameRow = new ArrayList<>();
        final InlineKeyboardButton joinGameButton = new InlineKeyboardButton();
        joinGameButton.setText(JOIN_GAME_BUTTON_TEXT);
        joinGameButton.setCallbackData("PLAY_PRIVATE_BATTLE_JOIN_GAME");
        joinGameRow.add(joinGameButton);

        final List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        final InlineKeyboardButton cancelButton = new InlineKeyboardButton();
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
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        final InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_PRIVATE_BATTLE_CREATE_GAME_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generatePrivateBattleJoinGameReplyMarkup() {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        final InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText(CANCEL_BUTTON_TEXT);
        cancelButton.setCallbackData("PLAY_PRIVATE_BATTLE_JOIN_GAME_CANCEL");
        cancelRow.add(cancelButton);

        keyboard.add(cancelRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
