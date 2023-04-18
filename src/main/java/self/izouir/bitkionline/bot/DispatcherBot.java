package self.izouir.bitkionline.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import self.izouir.bitkionline.commander.MainMenuCommander;

import static self.izouir.bitkionline.commander.BotCommander.sendMessage;

@Component
public class DispatcherBot extends TelegramLongPollingBot {
    @Value("${telegram.bot.username}")
    private String botUsername;
    @Value("${telegram.bot.token}")
    private String botToken;

    private final MainMenuCommander mainMenuCommander;

    @Autowired
    public DispatcherBot(MainMenuCommander mainMenuCommander) {
        this.mainMenuCommander = mainMenuCommander;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = update.getMessage().getChatId();
        String command = update.getMessage().getText();

        switch (command) {
            case "/start" -> mainMenuCommander.start(this, update);
            case "/play" -> mainMenuCommander.play(this, update);
            case "/rank" -> mainMenuCommander.rank(this, update);
            case "/eggs" -> mainMenuCommander.eggs(this, update);
            case "/profile" -> mainMenuCommander.profile(this, update);
            case "/help" -> mainMenuCommander.help(this, update);
            default -> sendMessage(this, chatId, "Command not found");
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
