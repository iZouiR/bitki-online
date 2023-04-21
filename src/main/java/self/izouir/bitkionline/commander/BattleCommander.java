package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;
import self.izouir.bitkionline.service.player.PlayerService;

@Component
public class BattleCommander {
    private final MatchMakingBattleService matchMakingBattleService;
    private final PrivateBattleService privateBattleService;
    private final PlayerService playerService;

    @Autowired
    public BattleCommander(MatchMakingBattleService matchMakingBattleService,
                           PrivateBattleService privateBattleService,
                           PlayerService playerService) {
        this.matchMakingBattleService = matchMakingBattleService;
        this.privateBattleService = privateBattleService;
        this.playerService = playerService;
    }

    public void startMatchMakingBattle(DispatcherBot bot, Long chatId, MatchMakingBattle matchMakingBattle) {
    }

    public void startPrivateBattle(DispatcherBot bot, Long chatId, PrivateBattle privateBattle) {
    }
}
