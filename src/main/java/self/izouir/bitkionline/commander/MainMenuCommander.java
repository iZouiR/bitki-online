package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;

@Component
public class MainMenuCommander {
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;
    private final EggService eggService;
    private final MatchMakingBattleService matchMakingBattleService;
    private final PrivateBattleService privateBattleService;

    @Autowired
    public MainMenuCommander(PlayerService playerService,
                             PlayerBotService playerBotService,
                             EggService eggService,
                             MatchMakingBattleService matchMakingBattleService,
                             PrivateBattleService privateBattleService) {
        this.playerService = playerService;
        this.playerBotService = playerBotService;
        this.eggService = eggService;
        this.matchMakingBattleService = matchMakingBattleService;
        this.privateBattleService = privateBattleService;
    }

    public void start(DispatcherBot bot, Update update) {

    }

    public void play(DispatcherBot bot, Update update) {

    }

    public void rank(DispatcherBot bot, Update update) {

    }

    public void eggs(DispatcherBot bot, Update update) {

    }

    public void profile(DispatcherBot bot, Update update) {

    }

    public void help(DispatcherBot bot, Update update) {

    }
}
