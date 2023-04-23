package self.izouir.bitkionline.commander;

import lombok.extern.slf4j.Slf4j;
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
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.service.battle.PlayerBattleService;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static self.izouir.bitkionline.util.BotMessageSender.*;

@Slf4j
@Component
public class BattleCommander {
    private static final Map<Long, Long> CHATS_TO_BATTLES = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> CHATS_TO_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<Long, Long> BATTLES_TO_ATTACKER_CHATS = new ConcurrentHashMap<>();
    private final PlayerBattleService playerBattleService;
    private final EggService eggService;
    private final PlayerService playerService;
    private final Random random;

    @Autowired
    public BattleCommander(PlayerBattleService playerBattleService,
                           EggService eggService,
                           PlayerService playerService) {
        this.playerBattleService = playerBattleService;
        this.eggService = eggService;
        this.playerService = playerService;
        this.random = new Random();
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        if (callbackData.startsWith("BATTLE_EGG_")) {
            Egg egg = eggService.findById(Long.parseLong(callbackData.substring("BATTLE_EGG_".length())));

            Player player = playerService.findByChatId(chatId);
            PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));

            try {
                if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                    battle.setFirstPlayerEgg(egg);
                    playerBattleService.save(battle);

                    sendEditMessageText(bot, chatId, messageId, "You made your choice, waiting for the opponent... ⏱");

                    while (battle.getSecondPlayerEgg() == null) {
                        Thread.sleep(1000);
                        if (battle.getIsFirstPlayerWinner() != null) {
                            deleteMessage(bot, chatId, messageId);
                            return;
                        }
                        battle = playerBattleService.findById(battle.getId());
                    }

                    CHATS_TO_MESSAGES.put(chatId, messageId);
                    sendEditMessageText(bot, chatId, messageId, "Battle rages within... \uD83C\uDF0B");

                    Player opponent = battle.getSecondPlayer();
                    Egg opponentEgg = battle.getSecondPlayerEgg();
                    SendSticker sticker = SendSticker.builder()
                            .chatId(String.valueOf(chatId))
                            .sticker(new InputFile(Path.of(opponentEgg.getImagePath()).toFile()))
                            .build();
                    sticker.setReplyMarkup(generateOpponentEggReplyMarkup(opponent, opponentEgg));
                    sendSticker(bot, sticker);
                }
                if (Objects.equals(player.getId(), battle.getSecondPlayer().getId())) {
                    battle.setSecondPlayerEgg(egg);
                    playerBattleService.save(battle);

                    sendEditMessageText(bot, chatId, messageId, "You made your choice, waiting for the opponent... ⏱");

                    while (battle.getFirstPlayerEgg() == null) {
                        Thread.sleep(1000);
                        if (battle.getIsFirstPlayerWinner() != null) {
                            deleteMessage(bot, chatId, messageId);
                            return;
                        }
                        battle = playerBattleService.findById(battle.getId());
                    }

                    CHATS_TO_MESSAGES.put(chatId, messageId);
                    sendEditMessageText(bot, chatId, messageId, "Battle rages within... \uD83C\uDF0B");

                    Player opponent = battle.getFirstPlayer();
                    Egg opponentEgg = battle.getFirstPlayerEgg();
                    SendSticker sticker = SendSticker.builder()
                            .chatId(String.valueOf(chatId))
                            .sticker(new InputFile(Path.of(opponentEgg.getImagePath()).toFile()))
                            .build();
                    sticker.setReplyMarkup(generateOpponentEggReplyMarkup(opponent, opponentEgg));
                    sendSticker(bot, sticker);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        if (callbackData.equals("BATTLE_OPPONENT_EGG_OK")) {
            deleteMessage(bot, chatId, messageId);
            messageId = CHATS_TO_MESSAGES.get(chatId);
            try {
                sendEditMessageText(bot, chatId, messageId, "Coin flip... \uD83E\uDE99");
                Thread.sleep(2500);

                for (int i = 3; i >= 0; i--) {
                    Thread.sleep(800);
                    sendEditMessageText(bot, chatId, messageId, i + "...");
                }

                Player player = playerService.findByChatId(chatId);
                Player opponent;
                PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
                if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                    opponent = battle.getSecondPlayer();
                } else {
                    opponent = battle.getFirstPlayer();
                }

                if (!BATTLES_TO_ATTACKER_CHATS.containsKey(battle.getId())) {
                    int chance = random.nextInt(100);
                    if (chance % 2 == 0) {
                        BATTLES_TO_ATTACKER_CHATS.put(battle.getId(), chatId);
                    } else {
                        BATTLES_TO_ATTACKER_CHATS.put(battle.getId(), opponent.getChatId());
                    }
                }

                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text("Stub")
                        .build();
                if (Objects.equals(chatId, BATTLES_TO_ATTACKER_CHATS.get(battle.getId()))) {
                    message.setText("Your turn to attack, choose the option! \uD83C\uDFB0");
                    message.setReplyMarkup(generateAttackerReplyMarkup(player));
                } else {
                    message.setText("Your turn to defend, pray to god... \uD83C\uDF18");
                }
                sendEditMessageText(bot, message);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        if (callbackData.startsWith("BATTLE_ATTACK_")) {
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

            String attackType = callbackData.substring("BATTLE_ATTACK_".length());
            Integer damage;
            Integer replyDamage;
            int chance;

            sendEditMessageText(bot, chatId, messageId,
                    player.getUsername() + " trying to " + attackType.toLowerCase() + " attack ⚔️");
            sendEditMessageText(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()),
                    player.getUsername() + " trying to " + attackType.toLowerCase() + " attack ⚔️");

            try {
                Thread.sleep(3000);
                for (int i = 3; i >= 0; i--) {
                    Thread.sleep(800);
                    sendEditMessageText(bot, chatId, messageId, i + "...");
                    sendEditMessageText(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()), i + "...");
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }

            switch (attackType) {
                case "HEAD" -> {
                    damage = Math.round(0.8f * egg.getPower() + 0.4f * egg.getEndurance());
                    replyDamage = Math.round(damage - 1.5f * egg.getIntelligence());
                    chance = Math.round(0.5f * generateChanceOfAttack(egg, opponentEgg));
                }
                case "SIDE" -> {
                    damage = Math.round(0.6f * egg.getPower() + 0.2f * egg.getEndurance());
                    replyDamage = Math.round(damage - 2.0f * egg.getIntelligence());
                    chance = Math.round(0.9f * generateChanceOfAttack(egg, opponentEgg));
                }
                case "ASS" -> {
                    damage = Math.round(0.5f * egg.getPower());
                    replyDamage = Math.round(damage - 3.0f * egg.getIntelligence());
                    chance = Math.round(1.2f * generateChanceOfAttack(egg, opponentEgg));
                }
                default -> {
                    damage = 0;
                    replyDamage = 0;
                    chance = 0;
                }
            }
            if (replyDamage < 0) {
                replyDamage = 0;
            }
            if ((random.nextInt(100) + 1) <= chance) {
                opponentEgg.setEndurance(opponentEgg.getEndurance() - damage);
                if (opponentEgg.getEndurance() <= 0) {
                    opponentEgg.setEndurance(0);
                    opponentEgg.setIsCracked(true);
                }
                eggService.save(opponentEgg);
                egg.setEndurance(egg.getEndurance() - replyDamage);
                if (egg.getEndurance() <= 0) {
                    egg.setEndurance(0);
                    egg.setIsCracked(true);
                }
                eggService.save(egg);
                sendEditMessageText(bot, chatId, messageId,
                        opponent.getUsername() + ": \"Lucky devil, " + player.getUsername()
                        + ", you dealt " + damage + " damage and had " + replyDamage + " as reply \uD83D\uDE08\"\n"
                        + "You - " + egg.getEndurance() + "❤️\u200D\uD83E\uDE79"
                        + " | Opponent - " + opponentEgg.getEndurance() + "❤️\u200D\uD83E\uDE79");
                sendEditMessageText(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()),
                        player.getUsername() + ": \"Does it hurt??!\uD83D\uDD2A\uD83E\uDE78\"\n"
                        + "You - " + opponentEgg.getEndurance() + "❤️\u200D\uD83E\uDE79"
                        + " | Opponent - " + egg.getEndurance() + "❤️\u200D\uD83E\uDE79"
                );
            } else {
                sendEditMessageText(bot, chatId, messageId,
                        opponent.getUsername() + ": \"I'M FUCKING INVINCIBLE!!! C'MON, TRY AND HIT ME! \uD83D\uDC79\"\n"
                        + "You - " + egg.getEndurance() + "❤️\u200D\uD83E\uDE79"
                        + " | Opponent - " + opponentEgg.getEndurance() + "❤️\u200D\uD83E\uDE79");
                sendEditMessageText(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()),
                        player.getUsername() + ": \"Making the mother of all omelettes here "
                        + opponent.getUsername() + ". Can't fret over every egg \uD83D\uDC7A\"\n"
                        + "You - " + opponentEgg.getEndurance() + "❤️\u200D\uD83E\uDE79"
                        + " | Opponent - " + egg.getEndurance() + "❤️\u200D\uD83E\uDE79");
            }

            try {
                Thread.sleep(5500);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }

            if (egg.getIsCracked() && opponentEgg.getIsCracked()) {
                deleteMessage(bot, chatId, messageId);
                deleteMessage(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()));
                stopBattleOnDraw(bot, battle);
            } else if (opponentEgg.getIsCracked()) {
                deleteMessage(bot, chatId, messageId);
                deleteMessage(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()));
                stopBattleOnDefeat(bot, chatId);
            } else if (egg.getIsCracked()) {
                deleteMessage(bot, chatId, messageId);
                deleteMessage(bot, opponent.getChatId(), CHATS_TO_MESSAGES.get(opponent.getChatId()));
                stopBattleOnDefeat(bot, opponent.getChatId());
            } else {
                BATTLES_TO_ATTACKER_CHATS.put(battle.getId(), opponent.getChatId());
                sendEditMessageText(bot, chatId, messageId, "Your turn to defend, pray to god... \uD83C\uDF18");
                EditMessageText opponentMessage = EditMessageText.builder()
                        .chatId(String.valueOf(opponent.getChatId()))
                        .messageId(CHATS_TO_MESSAGES.get(opponent.getChatId()))
                        .text("Your turn to attack, choose the option! \uD83C\uDFB0")
                        .build();
                opponentMessage.setReplyMarkup(generateAttackerReplyMarkup(opponent));
                sendEditMessageText(bot, opponentMessage);
            }
        }
    }

    public void startBattle(DispatcherBot bot, Long chatId, PlayerBattle battle) {
        CHATS_TO_BATTLES.put(chatId, battle.getId());

        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Choose egg for the fight, you have 45 seconds!")
                .build();
        message.setReplyMarkup(generateEggReplyMarkup(chatId));
        sendMessage(bot, message);

        try {
            Player player = playerService.findByChatId(chatId);
            if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
                int counter = 0;
                while (battle.getFirstPlayerEgg() == null) {
                    Thread.sleep(1000);
                    counter++;
                    if (counter >= 45) {
                        stopBattleOnDisconnection(bot, chatId);
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
                    if (counter >= 45) {
                        stopBattleOnDisconnection(bot, chatId);
                        return;
                    }
                    battle = playerBattleService.findById(battle.getId());
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private Integer generateChanceOfAttack(Egg attackerEgg, Egg defenderEgg) {
        return Math.round((100.0f * attackerEgg.getLuck()) / (attackerEgg.getLuck() + defenderEgg.getLuck()));
    }

    private void stopBattleOnDisconnection(DispatcherBot bot, Long chatId) {
        Player looser = playerService.findByChatId(chatId);
        Player opponent;
        PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
        if (Objects.equals(looser.getId(), battle.getFirstPlayer().getId())) {
            battle.setIsFirstPlayerWinner(false);
            opponent = battle.getSecondPlayer();
        } else {
            battle.setIsFirstPlayerWinner(true);
            opponent = battle.getFirstPlayer();
        }
        looser.setIsPlaying(false);
        opponent.setIsPlaying(false);

        playerService.save(looser);
        playerService.save(opponent);
        playerBattleService.save(battle);
        battle = playerBattleService.findById(battle.getId());

        CHATS_TO_BATTLES.remove(chatId);
        CHATS_TO_BATTLES.remove(opponent.getChatId());
        CHATS_TO_MESSAGES.remove(chatId);
        CHATS_TO_MESSAGES.remove(opponent.getChatId());
        BATTLES_TO_ATTACKER_CHATS.remove(battle.getId());

        sendMessage(bot, chatId, "You were disconnected \uD83C\uDF10");
        sendMessage(bot, battle.getSecondPlayer().getChatId(), "Your opponent was disconnected \uD83C\uDF10");

        calculateRanks(bot, battle);
    }

    private void stopBattleOnDefeat(DispatcherBot bot, Long chatId) {
        Player winner = playerService.findByChatId(chatId);
        Player opponent;
        PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
        if (Objects.equals(winner.getId(), battle.getFirstPlayer().getId())) {
            battle.setIsFirstPlayerWinner(true);
            opponent = battle.getSecondPlayer();
        } else {
            battle.setIsFirstPlayerWinner(false);
            opponent = battle.getFirstPlayer();
        }
        winner.setIsPlaying(false);
        opponent.setIsPlaying(false);

        playerService.save(winner);
        playerService.save(opponent);
        playerBattleService.save(battle);
        battle = playerBattleService.findById(battle.getId());

        CHATS_TO_BATTLES.remove(chatId);
        CHATS_TO_BATTLES.remove(opponent.getChatId());
        CHATS_TO_MESSAGES.remove(chatId);
        CHATS_TO_MESSAGES.remove(opponent.getChatId());
        BATTLES_TO_ATTACKER_CHATS.remove(battle.getId());

        sendMessage(bot, chatId,
                "The game ends with your triumph, " + opponent.getUsername() + " was defeated \uD83C\uDF7E");
        sendMessage(bot, opponent.getChatId(),
                "Out here only the strong survive, you were defeated ⚰️");
        calculateRanks(bot, battle);
    }

    private void stopBattleOnDraw(DispatcherBot bot, PlayerBattle battle) {
        Player firstPlayer = battle.getFirstPlayer();
        Player secondPlayer = battle.getSecondPlayer();

        firstPlayer.setIsPlaying(false);
        secondPlayer.setIsPlaying(false);

        playerService.save(firstPlayer);
        playerService.save(secondPlayer);
        playerBattleService.save(battle);

        CHATS_TO_BATTLES.remove(firstPlayer.getChatId());
        CHATS_TO_BATTLES.remove(secondPlayer.getChatId());
        CHATS_TO_MESSAGES.remove(firstPlayer.getChatId());
        CHATS_TO_MESSAGES.remove(secondPlayer.getChatId());
        BATTLES_TO_ATTACKER_CHATS.remove(battle.getId());

        sendMessage(bot, firstPlayer.getChatId(),
                """
                        When our guard is down
                        I think we'll both agree
                        That violence breeds violence
                        But in the end it has to be this way...
                        You played draw 🕊""");
        sendMessage(bot, secondPlayer.getChatId(),
                """
                        When our guard is down
                        I think we'll both agree
                        That violence breeds violence
                        But in the end it has to be this way...
                        You played draw 🕊""");
    }

    private void calculateRanks(DispatcherBot bot, PlayerBattle battle) {
        Player firstPlayer = battle.getFirstPlayer();
        Player secondPlayer = battle.getSecondPlayer();

        int firstPlayerRank = firstPlayer.getRank() + 1;
        int secondPlayerRank = secondPlayer.getRank() + 1;

        int difference = Math.abs(firstPlayerRank - secondPlayerRank);

        if (battle.getIsFirstPlayerWinner()) {
            if (firstPlayerRank > secondPlayerRank) {
                firstPlayer.setRank(firstPlayerRank + Math.round(difference * 5.0f / firstPlayerRank) + 5);
                sendMessage(bot, firstPlayer.getChatId(),
                        "You earned " + (Math.round(difference * 5.0f / firstPlayerRank) + 5) + " points \uD83C\uDF96");

                secondPlayer.setRank(secondPlayerRank - Math.round(difference * 3.0f / firstPlayerRank));
                if (secondPlayer.getRank() < 0) {
                    secondPlayer.setRank(0);
                }
                sendMessage(bot, secondPlayer.getChatId(),
                        "You lost " + Math.round(difference * 3.0f / firstPlayerRank) + " points \uD83C\uDFAD");
            } else {
                firstPlayer.setRank(firstPlayerRank + Math.round(difference * 10.0f / secondPlayerRank) + 5);
                sendMessage(bot, firstPlayer.getChatId(),
                        "You earned " + (Math.round(difference * 10.0f / secondPlayerRank) + 5) + " points \uD83C\uDF96");

                secondPlayer.setRank(secondPlayerRank - Math.round(difference * 6.0f / secondPlayerRank));
                if (secondPlayer.getRank() < 0) {
                    secondPlayer.setRank(0);
                }
                sendMessage(bot, secondPlayer.getChatId(),
                        "You lost " + Math.round(difference * 6.0f / secondPlayerRank) + " points \uD83C\uDFAD");
            }
        } else {
            if (secondPlayerRank > firstPlayerRank) {
                secondPlayer.setRank(secondPlayerRank + Math.round(difference * 5.0f / firstPlayerRank) + 5);
                sendMessage(bot, secondPlayer.getChatId(),
                        "You earned " + (Math.round(difference * 5.0f / firstPlayerRank) + 5) + " points \uD83C\uDF96");

                firstPlayer.setRank(firstPlayerRank - Math.round(difference * 3.0f / firstPlayerRank));
                if (firstPlayer.getRank() < 0) {
                    firstPlayer.setRank(0);
                }
                sendMessage(bot, firstPlayer.getChatId(),
                        "You lost " + Math.round(difference * 3.0f / firstPlayerRank) + " points \uD83C\uDFAD");
            } else {
                secondPlayer.setRank(secondPlayerRank + Math.round(difference * 10.0f / secondPlayerRank) + 5);
                sendMessage(bot, secondPlayer.getChatId(),
                        "You earned " + (Math.round(difference * 10.0f / secondPlayerRank) + 5) + " points \uD83C\uDF96");

                firstPlayer.setRank(firstPlayerRank - Math.round(difference * 6.0f / secondPlayerRank));
                if (firstPlayer.getRank() < 0) {
                    firstPlayer.setRank(0);
                }
                sendMessage(bot, firstPlayer.getChatId(),
                        "You lost " + Math.round(difference * 6.0f / secondPlayerRank) + " points \uD83C\uDFAD");
            }
        }
        playerService.save(firstPlayer);
        playerService.save(secondPlayer);
    }

    private InlineKeyboardMarkup generateEggReplyMarkup(Long chatId) {
        Player player = playerService.findByChatId(chatId);
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
            stringInfo.append(eggService.generateEggStatsInfo(egg));
            stringInfo.append(")");
            eggButton.setText(stringInfo.toString());
            eggButton.setCallbackData("BATTLE_EGG_" + egg.getId());
            eggRow.add(eggButton);
            keyboard.add(eggRow);
        }

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateOpponentEggReplyMarkup(Player opponent, Egg opponentEgg) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> opponentRow = new ArrayList<>();
        InlineKeyboardButton opponentButton = new InlineKeyboardButton();
        opponentButton.setText("You fight with " + opponentEgg.getName() + " egg of " + opponent.getUsername() + " ☠️");
        opponentButton.setCallbackData("IGNORE");
        opponentRow.add(opponentButton);

        List<InlineKeyboardButton> okRow = new ArrayList<>();
        InlineKeyboardButton okButton = new InlineKeyboardButton();
        okButton.setText("Ok, let's dance!");
        okButton.setCallbackData("BATTLE_OPPONENT_EGG_OK");
        okRow.add(okButton);

        keyboard.add(opponentRow);
        keyboard.add(okRow);
        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardMarkup generateAttackerReplyMarkup(Player attacker) {
        PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(attacker.getChatId()));
        Egg egg;
        Egg opponentEgg;
        if (Objects.equals(attacker.getId(), battle.getFirstPlayer().getId())) {
            egg = battle.getFirstPlayerEgg();
            opponentEgg = battle.getSecondPlayerEgg();
        } else {
            egg = battle.getSecondPlayerEgg();
            opponentEgg = battle.getFirstPlayerEgg();
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> hardAttackRow = new ArrayList<>();
        InlineKeyboardButton hardAttackButton = new InlineKeyboardButton();
        hardAttackButton.setText("Head (\uD83D\uDC80): " + Math.round(0.8f * egg.getPower() + 0.4f * egg.getEndurance())
                                 + " damage (" + Math.round(0.5f * generateChanceOfAttack(egg, opponentEgg)) + "%)");
        hardAttackButton.setCallbackData("BATTLE_ATTACK_HEAD");
        hardAttackRow.add(hardAttackButton);

        List<InlineKeyboardButton> mediumAttackRow = new ArrayList<>();
        InlineKeyboardButton mediumAttackButton = new InlineKeyboardButton();
        mediumAttackButton.setText("Side (\uD83D\uDC7B): " + Math.round(0.6f * egg.getPower() + 0.2f * egg.getEndurance())
                                   + " damage (" + Math.round(0.9f * generateChanceOfAttack(egg, opponentEgg)) + "%)");
        mediumAttackButton.setCallbackData("BATTLE_ATTACK_SIDE");
        mediumAttackRow.add(mediumAttackButton);

        List<InlineKeyboardButton> weakAttackRow = new ArrayList<>();
        InlineKeyboardButton weakAttackButton = new InlineKeyboardButton();
        weakAttackButton.setText("Ass (\uD83D\uDCA9): " + Math.round(0.5f * egg.getPower())
                                 + " damage (" + Math.round(1.2f * generateChanceOfAttack(egg, opponentEgg)) + "%)");
        weakAttackButton.setCallbackData("BATTLE_ATTACK_ASS");
        weakAttackRow.add(weakAttackButton);

        keyboard.add(hardAttackRow);
        keyboard.add(mediumAttackRow);
        keyboard.add(weakAttackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
