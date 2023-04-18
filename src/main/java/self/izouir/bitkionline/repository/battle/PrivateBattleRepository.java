package self.izouir.bitkionline.repository.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.battle.PrivateBattle;

@Repository
public interface PrivateBattleRepository extends JpaRepository<PrivateBattle, Long> {
}
