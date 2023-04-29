package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import static self.izouir.bitkionline.constants.BotMessageSenderConstants.*;
import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;

@Component
public class StartCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    @Autowired
    public StartCommander(PlayerService playerService,
                          PlayerBotService playerBotService) {
        this.playerService = playerService;
        this.playerBotService = playerBotService;
    }

    public void start(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                sendMessage(bot, chatId, String.format(GREETINGS_MESSAGE, player.getUsername()));
            } else {
                sendMessage(bot, chatId, PLAYER_NOT_REGISTERED_MESSAGE);
            }
        } else {
            sendMessage(bot, chatId, AWAIT_USERNAME_MESSAGE);
            startRegistration(chatId);
        }
    }

    private void startRegistration(Long chatId) {
        playerService.createNotRegisteredPlayer(chatId);
    }

    public boolean finishRegistration(DispatcherBot bot, Long chatId, String username) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
            if (playerBot.getLastState() == PlayerBotState.AWAIT_USERNAME) {
                if (playerService.notExistsByUsernameIgnoreCase(username)) {
                    playerService.registerPlayer(bot, player, username);
                    sendMessage(bot, chatId, String.format(PLAYER_REGISTERED_MESSAGE, username));
                } else {
                    sendMessage(bot, chatId, String.format(PLAYER_ALREADY_EXISTS_MESSAGE, username));
                }
                return true;
            }
        }
        return false;
    }
}
