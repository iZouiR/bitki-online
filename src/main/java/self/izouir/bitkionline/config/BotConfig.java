package self.izouir.bitkionline.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import self.izouir.bitkionline.bot.DispatcherBot;

@Slf4j
@Configuration
public class BotConfig {
    private final DispatcherBot dispatcherBot;

    @Autowired
    public BotConfig(DispatcherBot dispatcherBot) {
        this.dispatcherBot = dispatcherBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(dispatcherBot);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
