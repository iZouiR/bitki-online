package self.izouir.bitkionline.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import self.izouir.bitkionline.commander.MainMenuCommander;
import self.izouir.bitkionline.commander.ProfileCommander;

import static self.izouir.bitkionline.commander.util.BotCommander.sendMessage;

@Component
public class DispatcherBot extends TelegramLongPollingBot {
    @Value("${telegram.bot.username}")
    private String botUsername;
    @Value("${telegram.bot.token}")
    private String botToken;

    private final MainMenuCommander mainMenuCommander;
    private final ProfileCommander profileCommander;

    @Autowired
    public DispatcherBot(MainMenuCommander mainMenuCommander,
                         ProfileCommander profileCommander) {
        this.mainMenuCommander = mainMenuCommander;
        this.profileCommander = profileCommander;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String command = update.getMessage().getText();

            switch (command) {
                case "/start" -> mainMenuCommander.start(this, chatId);
                case "/play" -> mainMenuCommander.play(this, chatId);
                case "/rank" -> mainMenuCommander.rank(this, chatId);
                case "/eggs" -> mainMenuCommander.eggs(this, chatId);
                case "/profile" -> mainMenuCommander.profile(this, chatId);
                case "/help" -> mainMenuCommander.help(this, chatId);
                default -> {
                    if (profileCommander.register(this, chatId, command)) {
                        return;
                    }
                    if (profileCommander.changeUsername(this, chatId, command)) {
                        return;
                    }
                    sendMessage(this, chatId, "Command not found");
                }
            }
        } else if (update.hasCallbackQuery()) {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callbackData = update.getCallbackQuery().getData();

            profileCommander.processCallbackQuery(this, chatId, messageId, callbackData);
        }
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
