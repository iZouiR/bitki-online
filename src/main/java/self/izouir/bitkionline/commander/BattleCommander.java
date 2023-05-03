package self.izouir.bitkionline.commander;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.egg.EggAttackType;
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.service.battle.PlayerBattleService;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerService;
import self.izouir.bitkionline.service.player.PlayerStatisticsService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static self.izouir.bitkionline.util.BotMessageSender.*;
import static self.izouir.bitkionline.util.constants.commander.BattleCommanderConstants.*;

@Component
public class BattleCommander {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattleCommander.class);
    private static final Map<Long, Integer> CHATS_TO_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> CHATS_TO_BATTLES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> BATTLES_TO_ATTACKER_CHATS = new ConcurrentHashMap<>();
    private final PlayerBattleService playerBattleService;
    private final EggService eggService;
    private final PlayerStatisticsService playerStatisticsService;
    private final PlayerService playerService;
    private final Random random;

    @Autowired
    public BattleCommander(PlayerBattleService playerBattleService,
                           EggService eggService,
                           PlayerStatisticsService playerStatisticsService,
                           PlayerService playerService) {
        this.playerBattleService = playerBattleService;
        this.eggService = eggService;
        this.playerStatisticsService = playerStatisticsService;
        this.playerService = playerService;
        this.random = new Random();
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        if (callbackData.startsWith("BATTLE_EGG_CHOICE_")) {
            if (CHATS_TO_BATTLES.get(chatId) == null) {
                deleteMessage(bot, chatId, messageId);
                return;
            }
            CHATS_TO_MESSAGES.put(chatId, messageId);
            PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
            Player player = playerService.findByChatId(chatId);
            Egg egg = eggService.findById(Long.parseLong(callbackData.substring("BATTLE_EGG_CHOICE_".length())));
            if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                playerBattleService.setFirstPlayerEgg(battle, egg);
            } else {
                playerBattleService.setSecondPlayerEgg(battle, egg);
            }
            sendEditMessageText(bot, chatId, messageId, EGG_CHOICE_SUCCESS_MESSAGE);
            awaitOpponentEggChoice(bot, chatId, messageId, battle);
            if (CHATS_TO_BATTLES.get(chatId) != null) {
                battle = playerBattleService.findById(battle.getId());
                showOpponentInfo(bot, chatId, battle);
            }
        }
        if (callbackData.equals("BATTLE_JOIN_FIGHT")) {
            deleteMessage(bot, chatId, messageId);
            if (CHATS_TO_BATTLES.get(chatId) != null) {
                chooseAttacker(bot, chatId);
            }
        }
        if (callbackData.equals("BATTLE_SURRENDER")) {
            deleteMessage(bot, chatId, messageId);
            if (CHATS_TO_BATTLES.get(chatId) != null) {
                stopBattleOnSurrender(bot, chatId, playerBattleService.findById(CHATS_TO_BATTLES.get(chatId)));
            }
        }
        if (callbackData.startsWith("BATTLE_ATTACK_")) {
            if (CHATS_TO_BATTLES.get(chatId) == null) {
                deleteMessage(bot, chatId, messageId);
                return;
            }
            CHATS_TO_MESSAGES.put(chatId, messageId);
            PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
            Player player = playerService.findByChatId(chatId);
            Player opponent;
            Egg egg;
            Egg opponentEgg;
            if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                opponent = battle.getSecondPlayer();
                egg = battle.getFirstPlayerEgg();
                opponentEgg = battle.getSecondPlayerEgg();
            } else {
                opponent = battle.getFirstPlayer();
                egg = battle.getSecondPlayerEgg();
                opponentEgg = battle.getFirstPlayerEgg();
            }

            EggAttackType attackType = EggAttackType.valueOf(callbackData.substring("BATTLE_ATTACK_".length()));
            playerStatisticsService.applyAttackChoice(player, attackType);
            Integer opponentMessageId = CHATS_TO_MESSAGES.get(opponent.getChatId());
            sendEditMessageText(bot, chatId, messageId,
                    String.format(ATTACK_MESSAGE, player.getUsername(), attackType.toString().toLowerCase(), opponent.getUsername()));
            sendEditMessageText(bot, opponent.getChatId(), opponentMessageId,
                    String.format(ATTACK_MESSAGE, player.getUsername(), attackType.toString().toLowerCase(), opponent.getUsername()));

            try {
                Thread.sleep(3000);
                for (int i = COUNT_DOWN_SECONDS; i > 0; i--) {
                    Thread.sleep(1000);
                    sendEditMessageText(bot, chatId, messageId, String.format(COUNT_DOWN_MESSAGE, i));
                    sendEditMessageText(bot, opponent.getChatId(), opponentMessageId, String.format(COUNT_DOWN_MESSAGE, i));
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage());
            }

            Integer damage = eggService.generateDamage(egg, attackType);
            Integer replyDamage = eggService.generateReplyDamage(egg, attackType);
            Integer chance = eggService.generateChanceOfAttack(egg, opponentEgg, attackType);

            if ((random.nextInt(100) + 1) <= chance) {
                eggService.applyDamage(opponentEgg, damage);
                eggService.applyDamage(egg, replyDamage);
                egg = eggService.findById(egg.getId());
                opponentEgg = eggService.findById(opponentEgg.getId());
                playerStatisticsService.applyAttackSuccess(player, attackType);
                playerStatisticsService.applyDealtDamage(player, damage);
                playerStatisticsService.applyTakenDamage(opponent, damage);
                sendEditMessageText(bot, chatId, messageId,
                        String.format(ATTACK_SUCCESS_ATTACKER_MESSAGE, opponent.getUsername())
                        + String.format(PLAYER_EGGS_ENDURANCE_MESSAGE, egg.getEndurance(), replyDamage, opponentEgg.getEndurance(), damage));
                sendEditMessageText(bot, opponent.getChatId(), opponentMessageId,
                        String.format(ATTACK_SUCCESS_DEFENDER_MESSAGE, player.getUsername())
                        + String.format(PLAYER_EGGS_ENDURANCE_MESSAGE, opponentEgg.getEndurance(), damage, egg.getEndurance(), replyDamage));
            } else {
                sendEditMessageText(bot, chatId, messageId,
                        String.format(ATTACK_FAIL_ATTACKER_MESSAGE, opponent.getUsername())
                        + String.format(PLAYER_EGGS_ENDURANCE_MESSAGE, egg.getEndurance(), 0, opponentEgg.getEndurance(), 0));
                sendEditMessageText(bot, opponent.getChatId(), opponentMessageId,
                        String.format(ATTACK_FAIL_DEFENDER_MESSAGE, player.getUsername())
                        + String.format(PLAYER_EGGS_ENDURANCE_MESSAGE, opponentEgg.getEndurance(), 0, egg.getEndurance(), 0));
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage());
            }

            if (egg.getIsCracked() && opponentEgg.getIsCracked()) {
                stopBattleOnDraw(bot, battle);
            } else if (opponentEgg.getIsCracked()) {
                stopBattleOnDefeat(bot, chatId, battle);
            } else if (egg.getIsCracked()) {
                stopBattleOnDefeat(bot, opponent.getChatId(), battle);
            } else {
                BATTLES_TO_ATTACKER_CHATS.put(battle.getId(), opponent.getChatId());
                sendEditMessageText(bot, chatId, messageId, DEFENDER_MESSAGE);
                EditMessageText opponentMessage = EditMessageText.builder()
                        .chatId(String.valueOf(opponent.getChatId()))
                        .messageId(CHATS_TO_MESSAGES.get(opponent.getChatId()))
                        .text(ATTACKER_MESSAGE)
                        .build();
                opponentMessage.setReplyMarkup(generateAttackerReplyMarkup(battle));
                sendEditMessageText(bot, opponentMessage);
                awaitAttackChoice(bot, opponent.getChatId(), battle);
            }
        }
    }

    public void startBattle(DispatcherBot bot, Long chatId, PlayerBattle battle) {
        CHATS_TO_BATTLES.put(chatId, battle.getId());
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(EGG_CHOICE_MESSAGE)
                .build();
        message.setReplyMarkup(generateEggChoiceReplyMarkup(playerService.findByChatId(chatId)));
        sendMessage(bot, message);
        awaitEggChoice(bot, chatId, battle);
    }

    private void showOpponentInfo(DispatcherBot bot, Long chatId, PlayerBattle battle) {
        Player player = playerService.findByChatId(chatId);
        Player opponent;
        Egg opponentEgg;
        if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
            opponent = battle.getSecondPlayer();
            opponentEgg = battle.getSecondPlayerEgg();
        } else {
            opponent = battle.getFirstPlayer();
            opponentEgg = battle.getFirstPlayerEgg();
        }
        SendSticker sticker = SendSticker.builder()
                .chatId(String.valueOf(chatId))
                .sticker(new InputFile(Path.of(opponentEgg.getImagePath()).toFile()))
                .build();
        sticker.setReplyMarkup(generateOpponentInfoReplyMarkup(opponent));
        sendSticker(bot, sticker);
    }

    private void chooseAttacker(DispatcherBot bot, Long chatId) {
        PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
        Player player = playerService.findByChatId(chatId);
        Player opponent;
        if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
            opponent = battle.getSecondPlayer();
        } else {
            opponent = battle.getFirstPlayer();
        }
        Integer messageId = CHATS_TO_MESSAGES.get(chatId);
        try {
            sendEditMessageText(bot, chatId, messageId, COIN_FLIP_MESSAGE);
            Thread.sleep(2500);
            for (int i = COUNT_DOWN_SECONDS; i > 0; i--) {
                Thread.sleep(1000);
                sendEditMessageText(bot, chatId, messageId, String.format(COUNT_DOWN_MESSAGE, i));
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text("Stub")
                .build();
        if (!BATTLES_TO_ATTACKER_CHATS.containsKey(battle.getId())) {
            int chance = random.nextInt(2);
            if (chance == 0) {
                BATTLES_TO_ATTACKER_CHATS.put(battle.getId(), chatId);
                message.setText(ATTACKER_MESSAGE);
                message.setReplyMarkup(generateAttackerReplyMarkup(battle));
            } else {
                BATTLES_TO_ATTACKER_CHATS.put(battle.getId(), opponent.getChatId());
                message.setText(DEFENDER_MESSAGE);
            }
        } else {
            if (Objects.equals(BATTLES_TO_ATTACKER_CHATS.get(battle.getId()), chatId)) {
                message.setText(ATTACKER_MESSAGE);
                message.setReplyMarkup(generateAttackerReplyMarkup(battle));
            } else {
                message.setText(DEFENDER_MESSAGE);
            }
        }
        sendEditMessageText(bot, message);
    }

    private void awaitEggChoice(DispatcherBot bot, Long chatId, PlayerBattle battle) {
        Player player = playerService.findByChatId(chatId);
        try {
            if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                int counter = 0;
                while (battle.getFirstPlayerEgg() == null) {
                    Thread.sleep(1000);
                    counter++;
                    if (counter >= EGG_CHOICE_SECONDS) {
                        stopBattleOnDisconnection(bot, chatId, battle);
                        return;
                    }
                    battle = playerBattleService.findById(battle.getId());
                }
            }
            if (Objects.equals(player.getId(), battle.getSecondPlayer().getId())) {
                int counter = 0;
                while (battle.getSecondPlayerEgg() == null) {
                    Thread.sleep(1000);
                    counter++;
                    if (counter >= EGG_CHOICE_SECONDS) {
                        stopBattleOnDisconnection(bot, chatId, battle);
                        return;
                    }
                    battle = playerBattleService.findById(battle.getId());
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void awaitOpponentEggChoice(DispatcherBot bot, Long chatId, Integer messageId, PlayerBattle battle) {
        Player player = playerService.findByChatId(chatId);
        try {
            if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                while (battle.getSecondPlayerEgg() == null) {
                    Thread.sleep(1000);
                    if (battle.getIsFirstPlayerWinner() != null) {
                        deleteMessage(bot, chatId, messageId);
                        return;
                    }
                    battle = playerBattleService.findById(battle.getId());
                }
            }
            if (Objects.equals(player.getId(), battle.getSecondPlayer().getId())) {
                while (battle.getFirstPlayerEgg() == null) {
                    Thread.sleep(1000);
                    if (battle.getIsFirstPlayerWinner() != null) {
                        deleteMessage(bot, chatId, messageId);
                        return;
                    }
                    battle = playerBattleService.findById(battle.getId());
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void awaitAttackChoice(DispatcherBot bot, Long chatId, PlayerBattle battle) {
        try {
            int counter = 0;
            while (Objects.equals(BATTLES_TO_ATTACKER_CHATS.get(battle.getId()), chatId)) {
                Thread.sleep(1000);
                counter++;
                if (counter >= ATTACK_CHOICE_SECONDS) {
                    stopBattleOnDisconnection(bot, chatId, battle);
                    return;
                }
                battle = playerBattleService.findById(battle.getId());
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void stopBattle(DispatcherBot bot, PlayerBattle battle) {
        Long firstPlayerChatId = battle.getFirstPlayer().getChatId();
        Long secondPlayerChatId = battle.getSecondPlayer().getChatId();
        playerService.setIsPlaying(firstPlayerChatId, false);
        playerService.setIsPlaying(secondPlayerChatId, false);
        CHATS_TO_BATTLES.remove(firstPlayerChatId);
        CHATS_TO_BATTLES.remove(secondPlayerChatId);
        Integer firstPlayerMessageId = CHATS_TO_MESSAGES.remove(firstPlayerChatId);
        if (firstPlayerMessageId != null) {
            deleteMessage(bot, firstPlayerChatId, firstPlayerMessageId);
        }
        Integer secondPlayerMessageId = CHATS_TO_MESSAGES.remove(secondPlayerChatId);
        if (secondPlayerMessageId != null) {
            deleteMessage(bot, secondPlayerChatId, secondPlayerMessageId);
        }
        BATTLES_TO_ATTACKER_CHATS.remove(battle.getId());
        Player firstPlayer = playerService.findByChatId(firstPlayerChatId);
        Player secondPlayer = playerService.findByChatId(secondPlayerChatId);
        playerStatisticsService.incrementTotalBattlesPlayed(firstPlayer);
        playerStatisticsService.incrementTotalBattlesPlayed(secondPlayer);
    }

    private void stopBattleOnDisconnection(DispatcherBot bot, Long disconnectedPlayerChatId, PlayerBattle battle) {
        stopBattle(bot, battle);
        battle = playerBattleService.findById(battle.getId());
        Player disconnectedPlayer = playerService.findByChatId(disconnectedPlayerChatId);
        Player opponent;
        if (Objects.equals(disconnectedPlayer.getId(), battle.getFirstPlayer().getId())) {
            playerBattleService.setIsFirstPlayerWinner(battle, false);
            opponent = battle.getSecondPlayer();
        } else {
            playerBattleService.setIsFirstPlayerWinner(battle, true);
            opponent = battle.getFirstPlayer();
        }
        playerStatisticsService.incrementTotalBattlesWon(opponent);
        sendMessage(bot, disconnectedPlayerChatId, String.format(PLAYER_DISCONNECTION_MESSAGE, disconnectedPlayer.getUsername()));
        sendMessage(bot, opponent.getChatId(), String.format(PLAYER_DISCONNECTION_MESSAGE, disconnectedPlayer.getUsername()));
        playerService.calculateEloRanks(bot, opponent, disconnectedPlayer);
    }

    private void stopBattleOnDefeat(DispatcherBot bot, Long winnerChatId, PlayerBattle battle) {
        stopBattle(bot, battle);
        battle = playerBattleService.findById(battle.getId());
        Player winner = playerService.findByChatId(winnerChatId);
        Player looser;
        if (Objects.equals(winner.getId(), battle.getFirstPlayer().getId())) {
            playerBattleService.setIsFirstPlayerWinner(battle, true);
            looser = battle.getSecondPlayer();
        } else {
            playerBattleService.setIsFirstPlayerWinner(battle, false);
            looser = battle.getFirstPlayer();
        }
        playerStatisticsService.incrementTotalBattlesWon(winner);
        sendMessage(bot, winnerChatId, String.format(WINNER_MESSAGE, looser.getUsername()));
        sendMessage(bot, looser.getChatId(), String.format(LOOSER_MESSAGE, winner.getUsername()));
        playerService.calculateEloRanks(bot, winner, looser);
    }

    private void stopBattleOnDraw(DispatcherBot bot, PlayerBattle battle) {
        stopBattle(bot, battle);
        sendMessage(bot, battle.getFirstPlayer().getChatId(), DRAW_MESSAGE);
        sendMessage(bot, battle.getSecondPlayer().getChatId(), DRAW_MESSAGE);
    }

    private void stopBattleOnSurrender(DispatcherBot bot, Long looserChatId, PlayerBattle battle) {
        stopBattle(bot, battle);
        battle = playerBattleService.findById(battle.getId());
        Player looser = playerService.findByChatId(looserChatId);
        Player winner;
        if (Objects.equals(looser.getId(), battle.getFirstPlayer().getId())) {
            playerBattleService.setIsFirstPlayerWinner(battle, true);
            winner = battle.getSecondPlayer();
        } else {
            playerBattleService.setIsFirstPlayerWinner(battle, false);
            winner = battle.getFirstPlayer();
        }
        playerStatisticsService.incrementTotalBattlesWon(winner);
        sendMessage(bot, winner.getChatId(), String.format(OPPONENT_SURRENDER_MESSAGE, looser.getUsername()));
        sendMessage(bot, looserChatId, SURRENDER_MESSAGE);
        playerService.calculateEloRanks(bot, winner, looser);
    }

    private InlineKeyboardMarkup generateEggChoiceReplyMarkup(Player player) {
        List<Egg> notCrackedInventory = eggService.findAllByOwnerWhereIsNotCracked(player);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Egg egg : notCrackedInventory) {
            List<InlineKeyboardButton> eggRow = new ArrayList<>();
            InlineKeyboardButton eggButton = new InlineKeyboardButton();
            StringBuilder stringInfo = new StringBuilder();
            if (egg.getType() == EggType.HOLY) {
                stringInfo.append("\uD83C\uDF1F");
            } else if (egg.getType() == EggType.STRONG) {
                stringInfo.append("⭐️");
            } else {
                stringInfo.append("✨");
            }
            stringInfo.append(egg.getName());
            stringInfo.append(" (");
            stringInfo.append(eggService.generateStatsInfo(egg));
            stringInfo.append(")");
            eggButton.setText(stringInfo.toString());
            eggButton.setCallbackData("BATTLE_EGG_CHOICE_" + egg.getId());
            eggRow.add(eggButton);
            keyboard.add(eggRow);
        }

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateOpponentInfoReplyMarkup(Player opponent) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> opponentInfoRow = new ArrayList<>();
        InlineKeyboardButton opponentInfoButton = new InlineKeyboardButton();
        opponentInfoButton.setText(String.format(OPPONENT_INFO_BUTTON_TEXT, opponent.getUsername(), opponent.getRank(),
                playerStatisticsService.calculateWinRate(playerStatisticsService.findByPlayerId(opponent.getId()))));
        opponentInfoButton.setCallbackData("IGNORE");
        opponentInfoRow.add(opponentInfoButton);

        List<InlineKeyboardButton> joinFightRow = new ArrayList<>();
        InlineKeyboardButton joinFightButton = new InlineKeyboardButton();
        joinFightButton.setText(JOIN_FIGHT_BUTTON_TEXT);
        joinFightButton.setCallbackData("BATTLE_JOIN_FIGHT");
        joinFightRow.add(joinFightButton);

        List<InlineKeyboardButton> surrenderRow = new ArrayList<>();
        InlineKeyboardButton surrenderButton = new InlineKeyboardButton();
        surrenderButton.setText(SURRENDER_BUTTON_TEXT);
        surrenderButton.setCallbackData("BATTLE_SURRENDER");
        surrenderRow.add(surrenderButton);

        keyboard.add(opponentInfoRow);
        keyboard.add(joinFightRow);
        keyboard.add(surrenderRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateAttackerReplyMarkup(PlayerBattle battle) {
        Egg attackerEgg;
        Egg defenderEgg;
        if (Objects.equals(BATTLES_TO_ATTACKER_CHATS.get(battle.getId()), battle.getFirstPlayer().getChatId())) {
            attackerEgg = battle.getFirstPlayerEgg();
            defenderEgg = battle.getSecondPlayerEgg();
        } else {
            attackerEgg = battle.getSecondPlayerEgg();
            defenderEgg = battle.getFirstPlayerEgg();
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> headAttackRow = new ArrayList<>();
        InlineKeyboardButton headAttackButton = new InlineKeyboardButton();
        headAttackButton.setText(String.format(HEAD_ATTACK_BUTTON_TEXT,
                eggService.generateDamage(attackerEgg, EggAttackType.HEAD),
                eggService.generateChanceOfAttack(attackerEgg, defenderEgg, EggAttackType.HEAD)));
        headAttackButton.setCallbackData("BATTLE_ATTACK_HEAD");
        headAttackRow.add(headAttackButton);

        List<InlineKeyboardButton> sideAttackRow = new ArrayList<>();
        InlineKeyboardButton sideAttackButton = new InlineKeyboardButton();
        sideAttackButton.setText(String.format(SIDE_ATTACK_BUTTON_TEXT,
                eggService.generateDamage(attackerEgg, EggAttackType.SIDE),
                eggService.generateChanceOfAttack(attackerEgg, defenderEgg, EggAttackType.SIDE)));
        sideAttackButton.setCallbackData("BATTLE_ATTACK_SIDE");
        sideAttackRow.add(sideAttackButton);

        List<InlineKeyboardButton> assAttackRow = new ArrayList<>();
        InlineKeyboardButton assAttackButton = new InlineKeyboardButton();
        assAttackButton.setText(String.format(ASS_ATTACK_BUTTON_TEXT,
                eggService.generateDamage(attackerEgg, EggAttackType.ASS),
                eggService.generateChanceOfAttack(attackerEgg, defenderEgg, EggAttackType.ASS)));
        assAttackButton.setCallbackData("BATTLE_ATTACK_ASS");
        assAttackRow.add(assAttackButton);

        keyboard.add(headAttackRow);
        keyboard.add(sideAttackRow);
        keyboard.add(assAttackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
