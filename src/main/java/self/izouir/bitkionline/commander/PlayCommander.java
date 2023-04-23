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
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PlayerBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static self.izouir.bitkionline.util.BotMessageSender.*;

@Slf4j
@Component
public class PlayCommander {
    private static final ExecutorService EXECUTOR = Executors.newWorkStealingPool();
    private static final Queue<Long> MATCH_MAKING_CHATS_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<Long, Integer> MATCH_MAKING_CHATS_TO_MESSAGES = new ConcurrentHashMap<>();
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
                    player.setIsPlaying(true);
                    playerService.save(player);

                    if (!MATCH_MAKING_CHATS_QUEUE.isEmpty() && !MATCH_MAKING_CHATS_QUEUE.contains(chatId)) {
                        Player opponent = playerService.findByChatId(MATCH_MAKING_CHATS_QUEUE.poll());
                        Integer opponentMessageId = MATCH_MAKING_CHATS_TO_MESSAGES.remove(opponent.getChatId());

                        PlayerBattle battle = PlayerBattle.builder()
                                .firstPlayer(player)
                                .secondPlayer(opponent)
                                .build();
                        playerBattleService.save(battle);

                        MatchMakingBattle matchMakingBattle = MatchMakingBattle.builder()
                                .playerBattle(battle)
                                .build();
                        matchMakingBattleService.save(matchMakingBattle);

                        deleteMessage(bot, chatId, messageId);
                        deleteMessage(bot, opponent.getChatId(), opponentMessageId);

                        EXECUTOR.execute(() -> battleCommander.startBattle(bot, chatId, battle));
                        EXECUTOR.execute(() -> battleCommander.startBattle(bot, opponent.getChatId(), battle));
                        return;
                    }

                    if (!MATCH_MAKING_CHATS_QUEUE.contains(chatId)) {
                        MATCH_MAKING_CHATS_QUEUE.offer(chatId);
                        MATCH_MAKING_CHATS_TO_MESSAGES.put(chatId, messageId);
                    }
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Waiting for an opponent...")
                            .build();
                    message.setReplyMarkup(generatePlayMatchMakingReplyMarkup());
                    sendEditMessageText(bot, message);

                    try {
                        int counter = 0;
                        while (MATCH_MAKING_CHATS_QUEUE.contains(chatId) && MATCH_MAKING_CHATS_TO_MESSAGES.containsKey(chatId)) {
                            Thread.sleep(1000);
                            counter++;
                            if (counter >= 120) {
                                EditMessageText cancelMessage = EditMessageText.builder()
                                        .chatId(String.valueOf(chatId))
                                        .messageId(messageId)
                                        .text("Opponent to play with wasn't found")
                                        .build();
                                cancelMessage.setReplyMarkup(generatePlayOpponentNotFoundReplyMarkup());
                                sendEditMessageText(bot, cancelMessage);
                                MATCH_MAKING_CHATS_QUEUE.remove(chatId);
                                MATCH_MAKING_CHATS_TO_MESSAGES.remove(chatId);
                            }
                        }
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                } else {
                    sendEditMessageText(bot, chatId, messageId, "You're already playing, try again later");
                }
            }
            case "PLAY_MATCH_MAKING_GO_BACK" -> {
                Player player = playerService.findByChatId(chatId);
                player.setIsPlaying(false);
                playerService.save(player);

                MATCH_MAKING_CHATS_QUEUE.remove(chatId);
                MATCH_MAKING_CHATS_TO_MESSAGES.remove(chatId);

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

                    PlayerBattle battle = PlayerBattle.builder()
                            .firstPlayer(player)
                            .build();
                    playerBattleService.save(battle);

                    PrivateBattle privateBattle = PrivateBattle.builder()
                            .playerBattle(battle)
                            .link(privateBattleService.generateLink())
                            .build();
                    privateBattleService.save(privateBattle);

                    PRIVATE_BATTLE_CHATS_TO_MESSAGES.put(chatId, messageId);
                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Tell the opponent this link - " + privateBattle.getLink() + "\n" +
                                  "It will be alive for 120 seconds")
                            .build();
                    message.setReplyMarkup(generatePlayPrivateBattleCreateGameReplyMarkup());
                    sendEditMessageText(bot, message);

                    try {
                        int counter = 0;
                        while (PRIVATE_BATTLE_CHATS_TO_MESSAGES.containsKey(chatId)) {
                            Thread.sleep(1000);
                            counter++;
                            if (counter >= 120) {
                                EditMessageText cancelMessage = EditMessageText.builder()
                                        .chatId(String.valueOf(chatId))
                                        .messageId(messageId)
                                        .text("Opponent to play with wasn't found")
                                        .build();
                                cancelMessage.setReplyMarkup(generatePlayOpponentNotFoundReplyMarkup());
                                sendEditMessageText(bot, cancelMessage);
                                PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(chatId);
                            }
                        }
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                } else {
                    sendEditMessageText(bot, chatId, messageId, "You're already playing, try again later");
                }
            }
            case "PLAY_PRIVATE_BATTLE_CREATE_GAME_GO_BACK" -> {
                Player player = playerService.findByChatId(chatId);
                player.setIsPlaying(false);
                playerService.save(player);

                PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(chatId);

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
                    playerBot.setLastBotState(BotState.AWAIT_PRIVATE_BATTLE_LINK);
                    playerBotService.save(playerBot);

                    EditMessageText message = EditMessageText.builder()
                            .chatId(String.valueOf(chatId))
                            .messageId(messageId)
                            .text("Enter the private battle link to continue")
                            .build();
                    message.setReplyMarkup(generatePlayPrivateBattleJoinGameReplyMarkup());
                    sendEditMessageText(bot, message);
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
            if (player.getRegisteredAt() != null) {
                if (!player.getIsPlaying()) {
                    List<Egg> notCrackedInventory = eggService.findAllByOwnerWhereIsNotCracked(player);
                    if (!notCrackedInventory.isEmpty()) {
                        SendMessage message = SendMessage.builder()
                                .chatId(String.valueOf(chatId))
                                .text("Choose the way to play")
                                .build();
                        message.setReplyMarkup(generatePlayReplyMarkup());
                        sendMessage(bot, message);
                    } else {
                        sendMessage(bot, chatId, "You don't have eggs to play, try refreshing them");
                    }
                } else {
                    sendMessage(bot, chatId, "You're already playing, try later");
                }
            } else {
                sendMessage(bot, chatId, "Finish registration before continuing");
            }
        } else {
            sendMessage(bot, chatId, "You aren't authorized - /start");
        }
    }

    public boolean connect(DispatcherBot bot, Long chatId, String link) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastBotState() == BotState.AWAIT_PRIVATE_BATTLE_LINK) {
                if (privateBattleService.existsByLink(link)) {
                    PrivateBattle privateBattle = privateBattleService.findByLink(link);
                    PlayerBattle battle = privateBattle.getPlayerBattle();
                    if (battle.getSecondPlayer() == null) {
                        battle.setSecondPlayer(player);
                        playerBattleService.save(battle);

                        playerBot.setLastBotState(null);
                        playerBotService.save(playerBot);

                        Player opponent = battle.getFirstPlayer();
                        Integer opponentMessageId = PRIVATE_BATTLE_CHATS_TO_MESSAGES.remove(opponent.getChatId());

                        deleteMessage(bot, chatId, opponentMessageId);

                        EXECUTOR.execute(() -> battleCommander.startBattle(bot, chatId, battle));
                        EXECUTOR.execute(() -> battleCommander.startBattle(bot, opponent.getChatId(), battle));
                    } else {
                        sendMessage(bot, chatId, "Private battle with link " + link + " is unavailable");
                    }
                } else {
                    sendMessage(bot, chatId, "Private battle with link " + link + " wasn't found");
                }
                return true;
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
