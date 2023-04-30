package self.izouir.bitkionline.repository.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.player.Player;

@Repository
public interface MatchMakingBattleRepository extends JpaRepository<MatchMakingBattle, Long> {
    void deleteAllByPlayerBattle_FirstPlayerOrPlayerBattle_SecondPlayer(Player firstPlayer, Player secondPlayer);
}
