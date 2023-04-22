package self.izouir.bitkionline.service.battle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.repository.battle.MatchMakingBattleRepository;

@Slf4j
@Service
public class MatchMakingBattleService {
    private final MatchMakingBattleRepository matchMakingBattleRepository;
    private final PlayerBattleService playerBattleService;

    @Autowired
    public MatchMakingBattleService(MatchMakingBattleRepository matchMakingBattleRepository,
                                    PlayerBattleService playerBattleService) {
        this.matchMakingBattleRepository = matchMakingBattleRepository;
        this.playerBattleService = playerBattleService;
    }

    public void save(MatchMakingBattle matchMakingBattle) {
        matchMakingBattleRepository.save(matchMakingBattle);
    }

    public MatchMakingBattle generateMatchMakingBattle(Player player, Player opponent) {
        PlayerBattle battle = playerBattleService.generatePlayerBattle(player, opponent);
        playerBattleService.save(battle);
        return MatchMakingBattle.builder()
                .playerBattle(battle)
                .build();
    }

    public void awaitConnection() {
        try {
            int counter = 0;
            while (counter < 120) {
                Thread.sleep(1000);
                counter++;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
