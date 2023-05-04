package self.izouir.bitkionline.service.battle;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.repository.battle.MatchMakingBattleRepository;

@RequiredArgsConstructor
@Service
public class MatchMakingBattleService {
    private final PlayerBattleService playerBattleService;
    private final MatchMakingBattleRepository matchMakingBattleRepository;

    public void save(MatchMakingBattle matchMakingBattle) {
        matchMakingBattleRepository.save(matchMakingBattle);
    }

    @Transactional
    public void deleteAllByPlayer(Player player) {
        matchMakingBattleRepository.deleteAllByPlayerBattle_FirstPlayerOrPlayerBattle_SecondPlayer(player, player);
    }

    @Transactional
    public MatchMakingBattle create(Player player, Player opponent) {
        PlayerBattle playerBattle = playerBattleService.create(player, opponent);
        MatchMakingBattle matchMakingBattle = MatchMakingBattle.builder()
                .playerBattle(playerBattle)
                .build();
        save(matchMakingBattle);
        return matchMakingBattle;
    }
}
