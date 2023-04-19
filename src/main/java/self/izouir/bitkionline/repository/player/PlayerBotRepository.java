package self.izouir.bitkionline.repository.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.player.PlayerBot;

import java.util.Optional;

@Repository
public interface PlayerBotRepository extends JpaRepository<PlayerBot, Long> {
    Optional<PlayerBot> findByPlayerId(Long playerId);

    void deleteByPlayerId(Long playerId);
}
