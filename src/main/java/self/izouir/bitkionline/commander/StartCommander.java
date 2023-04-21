package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;

@Component
public class StartCommander {
    private final EggService eggService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    @Autowired
    public StartCommander(EggService eggService,
                          PlayerService playerService,
                          PlayerBotService playerBotService) {
        this.eggService = eggService;
        this.playerService = playerService;
        this.playerBotService = playerBotService;
    }

    public void start(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            sendMessage(bot, chatId, "Greetings, " + player.getUsername() + "!");
        } else {
            sendMessage(bot, chatId, "Looks like you aren't authorized yet, enter your username");
            createNewPlayerAwaitingUsername(chatId);
        }
    }

    private void createNewPlayerAwaitingUsername(Long chatId) {
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

    public boolean finishAuthorization(DispatcherBot bot, Long chatId, String username) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastBotState() == BotState.AWAIT_USERNAME) {
                if (playerService.notExistsByUsernameIgnoreCase(username)) {
                    player.setUsername(username);
                    player.setIsPlaying(false);
                    player.setRank(0);
                    player.setRegisteredAt(Timestamp.from(Instant.now()));
                    playerService.save(player);

                    sendMessage(bot, chatId, "Congratulations, you're now registered with username " + username + "!");

                    List<Egg> inventory = eggService.generateStartInventory(bot, player);
                    playerBot.setLastBotState(null);
                    playerBot.setLastInventoryIndex(0);
                    playerBot.setLastInventorySize(inventory.size());
                    playerBotService.save(playerBot);
                } else {
                    sendMessage(bot, chatId, "Player with username " + username + " already exists, try another variant");
                }
                return true;
            }
        }
        return false;
    }
}
