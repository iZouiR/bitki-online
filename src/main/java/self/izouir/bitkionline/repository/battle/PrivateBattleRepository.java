package self.izouir.bitkionline.repository.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.entity.player.Player;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivateBattleRepository extends JpaRepository<PrivateBattle, Long> {
    Optional<PrivateBattle> findByLink(String link);

    List<PrivateBattle> findAllByPlayerBattle_FirstPlayerOrPlayerBattle_SecondPlayer(Player first, Player second);
}
