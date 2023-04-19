package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import static self.izouir.bitkionline.commander.util.BotCommander.sendMessage;

@Component
public class MainMenuCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;
    private final EggService eggService;
    private final MatchMakingBattleService matchMakingBattleService;
    private final PrivateBattleService privateBattleService;
    private final ProfileCommander profileCommander;

    @Autowired
    public MainMenuCommander(PlayerService playerService,
                             PlayerBotService playerBotService,
                             EggService eggService,
                             MatchMakingBattleService matchMakingBattleService,
                             PrivateBattleService privateBattleService,
                             ProfileCommander profileCommander) {
        this.playerService = playerService;
        this.playerBotService = playerBotService;
        this.eggService = eggService;
        this.matchMakingBattleService = matchMakingBattleService;
        this.privateBattleService = privateBattleService;
        this.profileCommander = profileCommander;
    }

    public void start(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            sendMessage(bot, chatId, "Greetings, " + player.getUsername() + "!");
        } else {
            sendMessage(bot, chatId, "Sorry, it seems, you are not registered yet, please, enter your username");

            Player player = Player.builder()
                    .chatId(chatId)
                    .build();
            playerService.save(player);

            PlayerBot playerBot = PlayerBot.builder()
                    .playerId(player.getId())
                    .lastBotState(BotState.AWAIT_USERNAME)
                    .build();
            playerBotService.save(playerBot);
        }
    }

    public void play(DispatcherBot bot, Long chatId) {

    }

    public void rank(DispatcherBot bot, Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(profileCommander.getRankInfo(chatId))
                .build();
        message.setReplyMarkup(profileCommander.getRankInlineKeyboardMarkup());
        sendMessage(bot, message);
    }

    public void eggs(DispatcherBot bot, Long chatId) {

    }

    public void profile(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);

            SendMessage message = SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(profileCommander.getPlayerInfo(player))
                    .build();
            message.setReplyMarkup(profileCommander.getProfileInlineKeyboardMarkup());
            sendMessage(bot, message);
        } else {
            sendMessage(bot, chatId, "You are not authorized, fix - /start");
        }
    }

    public void help(DispatcherBot bot, Long chatId) {
        sendMessage(bot, chatId, """
                At beta testing you enter the game with 3 random eggs
                
                All the eggs have their characteristics:
                    - Endurance (Describe the amount of damage the egg could take without breaking)
                    - Luck (Describe the amount of chance to damage enemy egg in some ways)
                    - Intelligence (Describe the reduction of damage taken while attacking enemy egg)
                
                Rules of game:
                    1. The battle starts by choosing who would attack first - the coin flip
                    2. Then attacker choose one of 3 variants of attack
                        (higher damage - lower chance)
                        (medium damage - medium chance)
                        (lower damage - higher chance)
                    3. Then chance and damage applies, calculating endurance of both battling eggs
                    4. Turn goes to the next player and points 2-4 become cycled
                    5. One or both eggs are broken
                        (broken egg belongs defeated player)
                        (both eggs are broken - draw)
                    6. The rest of eggs' endurance saved and eggs could be repaired in future
                    7. Calculates awards of winner, punishments of looser
                
                Types of battles:
                    Match Making Battle - ranked battle with random opponent based on player's rank
                    Private Battle - non-ranked battle with player's friend
                """);
    }
}
