package self.izouir.bitkionline.repository.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.battle.PlayerBattle;

@Repository
public interface PlayerBattleRepository extends JpaRepository<PlayerBattle, Long> {
    void deleteAllByFirstPlayer_IdOrSecondPlayer_Id(Long firstPlayerId, Long secondPlayerId);
}
