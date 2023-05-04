package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static self.izouir.bitkionline.util.constant.commander.BattleCommanderConstant.*;

@RequiredArgsConstructor
@Component
public class BattleCommander {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattleCommander.class);
    private static final Map<Long, Integer> CHATS_TO_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> CHATS_TO_BATTLES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> BATTLES_TO_ATTACKER_CHATS = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final PlayerBattleService playerBattleService;
    private final EggService eggService;
    private final PlayerStatisticsService playerStatisticsService;
    private final PlayerService playerService;

    public void processCallbackQuery(final DispatcherBot bot, final Long chatId, final Integer messageId, final String callbackData) {
        if (callbackData.startsWith("BATTLE_EGG_CHOICE_")) {
            if (CHATS_TO_BATTLES.get(chatId) == null) {
                deleteMessage(bot, chatId, messageId);
                return;
            }
            CHATS_TO_MESSAGES.put(chatId, messageId);
            PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
            final Player player = playerService.findByChatId(chatId);
            final Egg egg = eggService.findById(Long.parseLong(callbackData.substring("BATTLE_EGG_CHOICE_".length())));
            if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                playerBattleService.applyFirstPlayerEgg(battle, egg);
            } else {
                playerBattleService.applySecondPlayerEgg(battle, egg);
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
            final PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
            final Player player = playerService.findByChatId(chatId);
            final Player opponent;
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

            final EggAttackType attackType = EggAttackType.valueOf(callbackData.substring("BATTLE_ATTACK_".length()));
            playerStatisticsService.applyAttackChoice(player, attackType);
            final Integer opponentMessageId = CHATS_TO_MESSAGES.get(opponent.getChatId());
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
            } catch (final InterruptedException e) {
                LOGGER.error(e.getMessage());
            }

            final Integer damage = eggService.generateDamage(egg, attackType);
            final Integer replyDamage = eggService.generateReplyDamage(egg, attackType);
            final Integer chance = eggService.generateChanceOfAttack(egg, opponentEgg, attackType);

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
            } catch (final InterruptedException e) {
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
                final EditMessageText opponentMessage = EditMessageText.builder()
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

    public void startBattle(final DispatcherBot bot, final Long chatId, final PlayerBattle battle) {
        CHATS_TO_BATTLES.put(chatId, battle.getId());
        final SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(EGG_CHOICE_MESSAGE)
                .build();
        message.setReplyMarkup(generateEggChoiceReplyMarkup(playerService.findByChatId(chatId)));
        sendMessage(bot, message);
        awaitEggChoice(bot, chatId, battle);
    }

    private void showOpponentInfo(final DispatcherBot bot, final Long chatId, final PlayerBattle battle) {
        final Player player = playerService.findByChatId(chatId);
        final Player opponent;
        final Egg opponentEgg;
        if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
            opponent = battle.getSecondPlayer();
            opponentEgg = battle.getSecondPlayerEgg();
        } else {
            opponent = battle.getFirstPlayer();
            opponentEgg = battle.getFirstPlayerEgg();
        }
        final SendSticker sticker = SendSticker.builder()
                .chatId(String.valueOf(chatId))
                .sticker(new InputFile(Path.of(opponentEgg.getImagePath()).toFile()))
                .build();
        sticker.setReplyMarkup(generateOpponentInfoReplyMarkup(opponent));
        sendSticker(bot, sticker);
    }

    private void chooseAttacker(final DispatcherBot bot, final Long chatId) {
        final PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
        final Player player = playerService.findByChatId(chatId);
        final Player opponent;
        if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
            opponent = battle.getSecondPlayer();
        } else {
            opponent = battle.getFirstPlayer();
        }
        final Integer messageId = CHATS_TO_MESSAGES.get(chatId);
        try {
            sendEditMessageText(bot, chatId, messageId, COIN_FLIP_MESSAGE);
            Thread.sleep(2500);
            for (int i = COUNT_DOWN_SECONDS; i > 0; i--) {
                Thread.sleep(1000);
                sendEditMessageText(bot, chatId, messageId, String.format(COUNT_DOWN_MESSAGE, i));
            }
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
        final EditMessageText message = EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text("Stub")
                .build();
        if (!BATTLES_TO_ATTACKER_CHATS.containsKey(battle.getId())) {
            final int chance = random.nextInt(2);
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

    private void awaitEggChoice(final DispatcherBot bot, final Long chatId, PlayerBattle battle) {
        final Player player = playerService.findByChatId(chatId);
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
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void awaitOpponentEggChoice(final DispatcherBot bot, final Long chatId, final Integer messageId, PlayerBattle battle) {
        final Player player = playerService.findByChatId(chatId);
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
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void awaitAttackChoice(final DispatcherBot bot, final Long chatId, PlayerBattle battle) {
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
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void stopBattle(final DispatcherBot bot, final PlayerBattle battle) {
        final Long firstPlayerChatId = battle.getFirstPlayer().getChatId();
        final Long secondPlayerChatId = battle.getSecondPlayer().getChatId();
        playerService.applyIsPlaying(firstPlayerChatId, false);
        playerService.applyIsPlaying(secondPlayerChatId, false);
        CHATS_TO_BATTLES.remove(firstPlayerChatId);
        CHATS_TO_BATTLES.remove(secondPlayerChatId);
        final Integer firstPlayerMessageId = CHATS_TO_MESSAGES.remove(firstPlayerChatId);
        if (firstPlayerMessageId != null) {
            deleteMessage(bot, firstPlayerChatId, firstPlayerMessageId);
        }
        final Integer secondPlayerMessageId = CHATS_TO_MESSAGES.remove(secondPlayerChatId);
        if (secondPlayerMessageId != null) {
            deleteMessage(bot, secondPlayerChatId, secondPlayerMessageId);
        }
        BATTLES_TO_ATTACKER_CHATS.remove(battle.getId());
        final Player firstPlayer = playerService.findByChatId(firstPlayerChatId);
        final Player secondPlayer = playerService.findByChatId(secondPlayerChatId);
        playerStatisticsService.incrementTotalBattlesPlayed(firstPlayer);
        playerStatisticsService.incrementTotalBattlesPlayed(secondPlayer);
    }

    private void stopBattleOnDisconnection(final DispatcherBot bot, final Long disconnectedPlayerChatId, PlayerBattle battle) {
        stopBattle(bot, battle);
        battle = playerBattleService.findById(battle.getId());
        final Player disconnectedPlayer = playerService.findByChatId(disconnectedPlayerChatId);
        final Player opponent;
        if (Objects.equals(disconnectedPlayer.getId(), battle.getFirstPlayer().getId())) {
            playerBattleService.applyIsFirstPlayerWinner(battle, false);
            opponent = battle.getSecondPlayer();
        } else {
            playerBattleService.applyIsFirstPlayerWinner(battle, true);
            opponent = battle.getFirstPlayer();
        }
        playerStatisticsService.incrementTotalBattlesWon(opponent);
        sendMessage(bot, disconnectedPlayerChatId, String.format(PLAYER_DISCONNECTION_MESSAGE, disconnectedPlayer.getUsername()));
        sendMessage(bot, opponent.getChatId(), String.format(PLAYER_DISCONNECTION_MESSAGE, disconnectedPlayer.getUsername()));
        playerService.calculateEloRanks(bot, opponent, disconnectedPlayer);
    }

    private void stopBattleOnDefeat(final DispatcherBot bot, final Long winnerChatId, PlayerBattle battle) {
        stopBattle(bot, battle);
        battle = playerBattleService.findById(battle.getId());
        final Player winner = playerService.findByChatId(winnerChatId);
        final Player looser;
        if (Objects.equals(winner.getId(), battle.getFirstPlayer().getId())) {
            playerBattleService.applyIsFirstPlayerWinner(battle, true);
            looser = battle.getSecondPlayer();
        } else {
            playerBattleService.applyIsFirstPlayerWinner(battle, false);
            looser = battle.getFirstPlayer();
        }
        playerStatisticsService.incrementTotalBattlesWon(winner);
        sendMessage(bot, winnerChatId, String.format(WINNER_MESSAGE, looser.getUsername()));
        sendMessage(bot, looser.getChatId(), String.format(LOOSER_MESSAGE, winner.getUsername()));
        playerService.calculateEloRanks(bot, winner, looser);
    }

    private void stopBattleOnDraw(final DispatcherBot bot, final PlayerBattle battle) {
        stopBattle(bot, battle);
        sendMessage(bot, battle.getFirstPlayer().getChatId(), DRAW_MESSAGE);
        sendMessage(bot, battle.getSecondPlayer().getChatId(), DRAW_MESSAGE);
    }

    private void stopBattleOnSurrender(final DispatcherBot bot, final Long looserChatId, PlayerBattle battle) {
        stopBattle(bot, battle);
        battle = playerBattleService.findById(battle.getId());
        final Player looser = playerService.findByChatId(looserChatId);
        final Player winner;
        if (Objects.equals(looser.getId(), battle.getFirstPlayer().getId())) {
            playerBattleService.applyIsFirstPlayerWinner(battle, true);
            winner = battle.getSecondPlayer();
        } else {
            playerBattleService.applyIsFirstPlayerWinner(battle, false);
            winner = battle.getFirstPlayer();
        }
        playerStatisticsService.incrementTotalBattlesWon(winner);
        sendMessage(bot, winner.getChatId(), String.format(OPPONENT_SURRENDER_MESSAGE, looser.getUsername()));
        sendMessage(bot, looserChatId, SURRENDER_MESSAGE);
        playerService.calculateEloRanks(bot, winner, looser);
    }

    private InlineKeyboardMarkup generateEggChoiceReplyMarkup(final Player player) {
        final List<Egg> notCrackedInventory = eggService.findAllByOwnerWhereIsNotCracked(player);

        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (final Egg egg : notCrackedInventory) {
            final List<InlineKeyboardButton> eggRow = new ArrayList<>();
            final InlineKeyboardButton eggButton = new InlineKeyboardButton();
            final StringBuilder stringInfo = new StringBuilder();
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

    private InlineKeyboardMarkup generateOpponentInfoReplyMarkup(final Player opponent) {
        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> opponentInfoRow = new ArrayList<>();
        final InlineKeyboardButton opponentInfoButton = new InlineKeyboardButton();
        opponentInfoButton.setText(String.format(OPPONENT_INFO_BUTTON_TEXT, opponent.getUsername(), opponent.getRank(),
                playerStatisticsService.calculateWinRate(playerStatisticsService.findByPlayerId(opponent.getId()))));
        opponentInfoButton.setCallbackData("IGNORE");
        opponentInfoRow.add(opponentInfoButton);

        final List<InlineKeyboardButton> joinFightRow = new ArrayList<>();
        final InlineKeyboardButton joinFightButton = new InlineKeyboardButton();
        joinFightButton.setText(JOIN_FIGHT_BUTTON_TEXT);
        joinFightButton.setCallbackData("BATTLE_JOIN_FIGHT");
        joinFightRow.add(joinFightButton);

        final List<InlineKeyboardButton> surrenderRow = new ArrayList<>();
        final InlineKeyboardButton surrenderButton = new InlineKeyboardButton();
        surrenderButton.setText(SURRENDER_BUTTON_TEXT);
        surrenderButton.setCallbackData("BATTLE_SURRENDER");
        surrenderRow.add(surrenderButton);

        keyboard.add(opponentInfoRow);
        keyboard.add(joinFightRow);
        keyboard.add(surrenderRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateAttackerReplyMarkup(final PlayerBattle battle) {
        final Egg attackerEgg;
        final Egg defenderEgg;
        if (Objects.equals(BATTLES_TO_ATTACKER_CHATS.get(battle.getId()), battle.getFirstPlayer().getChatId())) {
            attackerEgg = battle.getFirstPlayerEgg();
            defenderEgg = battle.getSecondPlayerEgg();
        } else {
            attackerEgg = battle.getSecondPlayerEgg();
            defenderEgg = battle.getFirstPlayerEgg();
        }

        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        final List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        final List<InlineKeyboardButton> headAttackRow = new ArrayList<>();
        final InlineKeyboardButton headAttackButton = new InlineKeyboardButton();
        headAttackButton.setText(String.format(HEAD_ATTACK_BUTTON_TEXT,
                eggService.generateDamage(attackerEgg, EggAttackType.HEAD),
                eggService.generateChanceOfAttack(attackerEgg, defenderEgg, EggAttackType.HEAD)));
        headAttackButton.setCallbackData("BATTLE_ATTACK_HEAD");
        headAttackRow.add(headAttackButton);

        final List<InlineKeyboardButton> sideAttackRow = new ArrayList<>();
        final InlineKeyboardButton sideAttackButton = new InlineKeyboardButton();
        sideAttackButton.setText(String.format(SIDE_ATTACK_BUTTON_TEXT,
                eggService.generateDamage(attackerEgg, EggAttackType.SIDE),
                eggService.generateChanceOfAttack(attackerEgg, defenderEgg, EggAttackType.SIDE)));
        sideAttackButton.setCallbackData("BATTLE_ATTACK_SIDE");
        sideAttackRow.add(sideAttackButton);

        final List<InlineKeyboardButton> assAttackRow = new ArrayList<>();
        final InlineKeyboardButton assAttackButton = new InlineKeyboardButton();
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
