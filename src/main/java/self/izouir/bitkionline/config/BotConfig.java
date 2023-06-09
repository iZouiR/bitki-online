package self.izouir.bitkionline.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import self.izouir.bitkionline.bot.DispatcherBot;

@RequiredArgsConstructor
@Configuration
public class BotConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotConfig.class);
    private final DispatcherBot dispatcherBot;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(dispatcherBot);
        } catch (final TelegramApiException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
