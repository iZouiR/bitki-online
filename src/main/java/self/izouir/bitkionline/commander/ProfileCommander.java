package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.BotState;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.sql.Timestamp;
import java.time.Instant;

import static self.izouir.bitkionline.commander.BotCommander.sendMessage;

@Component
public class ProfileCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    @Autowired
    public ProfileCommander(PlayerService playerService,
                            PlayerBotService playerBotService) {
        this.playerService = playerService;
        this.playerBotService = playerBotService;
    }

    // register player when entering username
    // * true, if expected operation was registration
    // * false, if expected operation was not registration
    public boolean register(DispatcherBot dispatcherBot, Update update, String username) {
        Long chatId = update.getMessage().getChatId();

        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (playerBotService.existsByPlayerId(player.getId())) {
                PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
                if (playerBot.getLastBotState() == BotState.AWAIT_USERNAME) {
                    if (!playerService.existsByUsernameIgnoreCase(username)) {
                        player.setUsername(username);
                        player.setRank(0L);
                        player.setRegisteredAt(Timestamp.from(Instant.now()));
                        playerService.save(player);

                        playerBot.setLastBotState(null);
                        playerBotService.save(playerBot);

                        sendMessage(dispatcherBot, chatId, "Congratulations, you are now registered with username " + username + "!");
                    } else {
                        sendMessage(dispatcherBot, chatId, "Player with username " + username + " already exists, try another variant");
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
