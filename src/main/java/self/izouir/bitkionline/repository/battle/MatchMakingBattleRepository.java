package self.izouir.bitkionline.repository.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.entity.player.Player;

import java.util.List;

@Repository
public interface MatchMakingBattleRepository extends JpaRepository<MatchMakingBattle, Long> {
    List<MatchMakingBattle> findAllByPlayerBattle_FirstPlayerOrPlayerBattle_SecondPlayer(Player first, Player second);
}
