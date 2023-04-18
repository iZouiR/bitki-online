package self.izouir.bitkionline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
}
