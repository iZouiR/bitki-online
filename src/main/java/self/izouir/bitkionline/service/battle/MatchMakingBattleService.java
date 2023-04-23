package self.izouir.bitkionline.service.battle;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.repository.battle.MatchMakingBattleRepository;

import java.util.List;

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

    @Transactional
    public void delete(MatchMakingBattle matchMakingBattle) {
        matchMakingBattleRepository.delete(matchMakingBattle);
    }

    @Transactional
    public void deleteAllByPlayer(Player player) {
        List<MatchMakingBattle> matchMakingBattles = matchMakingBattleRepository.findAllByPlayerBattle_FirstPlayerOrPlayerBattle_SecondPlayer(player, player);
        for (MatchMakingBattle matchMakingBattle : matchMakingBattles) {
            PlayerBattle battle = matchMakingBattle.getPlayerBattle();
            playerBattleService.delete(battle);
            delete(matchMakingBattle);
        }
    }
}
