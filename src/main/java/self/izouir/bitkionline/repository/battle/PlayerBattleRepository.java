package self.izouir.bitkionline.repository.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.player.Player;

@Repository
public interface PlayerBattleRepository extends JpaRepository<PlayerBattle, Long> {
    void deleteAllByFirstPlayerOrSecondPlayer(Player firstPlayer, Player secondPlayer);
}
