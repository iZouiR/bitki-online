package self.izouir.bitkionline.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import self.izouir.bitkionline.commander.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;

@RequiredArgsConstructor
@Component
public class DispatcherBot extends TelegramLongPollingBot {
    private static final ExecutorService EXECUTOR = Executors.newWorkStealingPool();
    @Value("${telegram.bot.username}")
    private String botUsername;
    @Value("${telegram.bot.token}")
    private String botToken;
    private final StartCommander startCommander;
    private final PlayCommander playCommander;
    private final BattleCommander battleCommander;
    private final RankCommander rankCommander;
    private final EggsCommander eggsCommander;
    private final ProfileCommander profileCommander;
    private final HelpCommander helpCommander;
    private final SupportCommander supportCommander;

    @Override
    public void onUpdateReceived(Update update) {
        EXECUTOR.execute(() -> {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                String command = update.getMessage().getText();

                switch (command) {
                    case "/start" -> startCommander.start(this, chatId);
                    case "/play" -> playCommander.play(this, chatId);
                    case "/rank" -> rankCommander.rank(this, chatId);
                    case "/eggs" -> eggsCommander.eggs(this, chatId);
                    case "/profile" -> profileCommander.profile(this, chatId);
                    case "/help" -> helpCommander.help(this, chatId);
                    case "/support" -> supportCommander.support(this, chatId);
                    default -> {
                        if (playCommander.connectToPrivateBattle(this, chatId, command)) {
                            return;
                        }
                        if (startCommander.finishRegistration(this, chatId, command)) {
                            return;
                        }
                        if (profileCommander.finishUsernameChange(this, chatId, command)) {
                            return;
                        }
                        if (supportCommander.publishSupportMessage(this, chatId, command)) {
                            return;
                        }
                        sendMessage(this, chatId, "Command not found");
                    }
                }
            } else if (update.hasCallbackQuery()) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
                String callbackData = update.getCallbackQuery().getData();

                playCommander.processCallbackQuery(this, chatId, messageId, callbackData);
                battleCommander.processCallbackQuery(this, chatId, messageId, callbackData);
                rankCommander.processCallbackQuery(this, chatId, messageId, callbackData);
                eggsCommander.processCallbackQuery(this, chatId, messageId, callbackData);
                profileCommander.processCallbackQuery(this, chatId, messageId, callbackData);
                helpCommander.processCallbackQuery(this, chatId, messageId, callbackData);
                supportCommander.processCallbackQuery(this, chatId, messageId, callbackData);
            }
        });
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
