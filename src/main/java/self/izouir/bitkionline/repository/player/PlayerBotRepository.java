package self.izouir.bitkionline.repository.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.player.PlayerBot;

@Repository
public interface PlayerBotRepository extends JpaRepository<PlayerBot, Long> {
}
