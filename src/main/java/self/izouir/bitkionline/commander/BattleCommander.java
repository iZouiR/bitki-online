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

                    sendEditMessageText(bot, chatId, messageId, "You made your choice, waiting for the opponent...");

                    while (battle.getSecondPlayerEgg() == null) {
                        Thread.sleep(1000);
                        if (battle.getIsFirstPlayerWinner() != null) {
                            deleteMessage(bot, chatId, messageId);
                            return;
                        }
                        battle = playerBattleService.findById(battle.getId());
                    }

                    CHATS_TO_MESSAGES.put(chatId, messageId);
                    sendEditMessageText(bot, chatId, messageId, "Battle rages within...");

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

                    sendEditMessageText(bot, chatId, messageId, "You made your choice, waiting for the opponent...");

                    while (battle.getFirstPlayerEgg() == null) {
                        Thread.sleep(1000);
                        if (battle.getIsFirstPlayerWinner() != null) {
                            deleteMessage(bot, chatId, messageId);
                            return;
                        }
                        battle = playerBattleService.findById(battle.getId());
                    }

                    CHATS_TO_MESSAGES.put(chatId, messageId);
                    sendEditMessageText(bot, chatId, messageId, "Battle rages within...");

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
                sendEditMessageText(bot, chatId, messageId, "Toss a coin...");
                Thread.sleep(2000);

                for (int i = 3; i > 0; i--) {
                    Thread.sleep(1000);
                    sendEditMessageText(bot, chatId, messageId, String.valueOf(i));
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
                    message.setText("Your turn to attack, choose the option!");
                    message.setReplyMarkup(generateAttackerReplyMarkup(player));
                } else {
                    message.setText("Your turn to defend, pray to god...");
                }
                sendEditMessageText(bot, message);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        if (callbackData.startsWith("BATTLE_ATTACK_")) {
            String attackType = callbackData.substring("BATTLE_ATTACK_".length());
            switch (attackType) {
                case "HARD" -> {

                }
                case "MEDIUM" -> {

                }
                case "WEAK" -> {

                }
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
        Player player = playerService.findByChatId(chatId);
        PlayerBattle battle = playerBattleService.findById(CHATS_TO_BATTLES.get(chatId));
        if (Objects.equals(player.getId(), battle.getFirstPlayer().getId())) {
            battle.setIsFirstPlayerWinner(false);

            Player opponent = battle.getSecondPlayer();
            player.setIsPlaying(false);
            opponent.setIsPlaying(false);

            playerService.save(player);
            playerService.save(opponent);

            CHATS_TO_BATTLES.remove(chatId);
            CHATS_TO_BATTLES.remove(opponent.getChatId());
            CHATS_TO_MESSAGES.remove(chatId);
            CHATS_TO_MESSAGES.remove(opponent.getChatId());
            BATTLES_TO_ATTACKER_CHATS.remove(battle.getId());

            sendMessage(bot, chatId, "You were disconnected");
            sendMessage(bot, battle.getSecondPlayer().getChatId(), "Your opponent was disconnected");
        }
        if (Objects.equals(player.getId(), battle.getSecondPlayer().getId())) {
            battle.setIsFirstPlayerWinner(true);

            Player opponent = battle.getFirstPlayer();
            player.setIsPlaying(false);
            opponent.setIsPlaying(false);

            playerService.save(player);
            playerService.save(opponent);

            CHATS_TO_BATTLES.remove(chatId);
            CHATS_TO_BATTLES.remove(opponent.getChatId());
            CHATS_TO_MESSAGES.remove(chatId);
            CHATS_TO_MESSAGES.remove(opponent.getChatId());

            sendMessage(bot, chatId, "You were disconnected");
            sendMessage(bot, battle.getFirstPlayer().getChatId(), "Your opponent was disconnected");
        }
        playerBattleService.save(battle);

        calculateRanks(bot, battle);
    }

    private void calculateRanks(DispatcherBot bot, PlayerBattle battle) {
        Player firstPlayer = battle.getFirstPlayer();
        Player secondPlayer = battle.getSecondPlayer();

        Integer firstPlayerRank = firstPlayer.getRank();
        Integer secondPlayerRank = secondPlayer.getRank();

        int difference = Math.abs(firstPlayerRank - secondPlayerRank);
        Integer minimum = Math.toIntExact(Math.round(0.2 * difference)) + 5;
        Integer maximum = Math.toIntExact(Math.round(0.4 * difference)) + 5;

        if (battle.getIsFirstPlayerWinner()) {
            if (firstPlayerRank > secondPlayerRank) {
                firstPlayer.setRank(firstPlayerRank + minimum);
                sendMessage(bot, firstPlayer.getChatId(), "You earned " + minimum + " points");

                secondPlayer.setRank(secondPlayerRank - maximum);
                if (secondPlayer.getRank() < 0) {
                    secondPlayer.setRank(0);
                }
                sendMessage(bot, secondPlayer.getChatId(), "You lost " + maximum + " points");
            } else {
                firstPlayer.setRank(firstPlayerRank + maximum);
                sendMessage(bot, firstPlayer.getChatId(), "You earned " + maximum + " points");

                secondPlayer.setRank(secondPlayerRank - minimum);
                if (secondPlayer.getRank() < 0) {
                    secondPlayer.setRank(0);
                }
                sendMessage(bot, secondPlayer.getChatId(), "You lost " + minimum + " points");
            }
        } else {
            if (secondPlayerRank > firstPlayerRank) {
                secondPlayer.setRank(secondPlayerRank + minimum);
                sendMessage(bot, secondPlayer.getChatId(), "You earned " + minimum + " points");

                firstPlayer.setRank(firstPlayerRank - maximum);
                if (firstPlayer.getRank() < 0) {
                    firstPlayer.setRank(0);
                }
                sendMessage(bot, firstPlayer.getChatId(), "You lost " + maximum + " points");
            } else {
                secondPlayer.setRank(secondPlayerRank + maximum);
                sendMessage(bot, secondPlayer.getChatId(), "You earned " + maximum + " points");

                firstPlayer.setRank(firstPlayerRank - minimum);
                if (firstPlayer.getRank() < 0) {
                    firstPlayer.setRank(0);
                }
                sendMessage(bot, firstPlayer.getChatId(), "You lost " + minimum + " points");
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
            eggButton.setText(egg.getName() + " (" + eggService.generateEggStatsInfo(egg) + ")");
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
        opponentButton.setText("You fight with " + opponentEgg.getName() + " egg of " + opponent.getUsername());
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
        hardAttackButton.setText("Hard: " + Math.round(0.8 * egg.getPower())
                                 + " damage (" + Math.round(0.6 * generateChanceOfAttack(egg, opponentEgg)) + "%)");
        hardAttackButton.setCallbackData("BATTLE_ATTACK_HARD");
        hardAttackRow.add(hardAttackButton);

        List<InlineKeyboardButton> mediumAttackRow = new ArrayList<>();
        InlineKeyboardButton mediumAttackButton = new InlineKeyboardButton();
        mediumAttackButton.setText("Medium: " + Math.round(0.6 * egg.getPower())
                                   + " damage (" + Math.round(0.8 * generateChanceOfAttack(egg, opponentEgg)) + "%)");
        mediumAttackButton.setCallbackData("BATTLE_ATTACK_MEDIUM");
        mediumAttackRow.add(mediumAttackButton);

        List<InlineKeyboardButton> weakAttackRow = new ArrayList<>();
        InlineKeyboardButton weakAttackButton = new InlineKeyboardButton();
        weakAttackButton.setText("Weak: " + Math.round(0.4 * egg.getPower())
                                 + " damage (" + generateChanceOfAttack(egg, opponentEgg) + "%)");
        weakAttackButton.setCallbackData("BATTLE_ATTACK_WEAK");
        weakAttackRow.add(weakAttackButton);

        keyboard.add(hardAttackRow);
        keyboard.add(mediumAttackRow);
        keyboard.add(weakAttackRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
