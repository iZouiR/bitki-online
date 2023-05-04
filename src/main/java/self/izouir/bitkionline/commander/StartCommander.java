package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;
import static self.izouir.bitkionline.util.constant.MessageConstant.*;
import static self.izouir.bitkionline.util.constant.commander.StartCommanderConstant.*;

@RequiredArgsConstructor
@Component
public class StartCommander {
    private final HelpCommander helpCommander;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    public void start(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                sendMessage(bot, chatId, String.format(GREETINGS_MESSAGE, player.getUsername()));
            } else {
                sendMessage(bot, chatId, PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE);
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
                if (playerService.isAccurateUsername(username)) {
                    if (playerService.notExistsByUsernameIgnoreCase(username)) {
                        playerService.registerPlayer(bot, player, username);
                        sendMessage(bot, chatId, String.format(PLAYER_REGISTERED_MESSAGE, username));
                        helpCommander.help(bot, chatId);
                    } else {
                        sendMessage(bot, chatId, String.format(USERNAME_ALREADY_EXISTS_MESSAGE, username));
                    }
                } else {
                    sendMessage(bot, chatId, INCORRECT_USERNAME_FORMAT_MESSAGE);
                }
                return true;
            }
        }
        return false;
    }
}
