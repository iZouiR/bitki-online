package self.izouir.bitkionline.repository.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.player.PlayerStatistics;

import java.util.Optional;

@Repository
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
    Optional<PlayerStatistics> findByPlayerId(Long playerId);
}
