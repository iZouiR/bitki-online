package self.izouir.bitkionline.repository.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.player.Player;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByChatId(Long chatId);
    Optional<Player> findByUsernameIgnoreCase(String username);
}
