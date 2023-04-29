package self.izouir.bitkionline.repository.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.player.Player;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByChatId(Long chatId);

    Optional<Player> findByUsernameIgnoreCase(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("FROM Player ORDER BY rank DESC")
    List<Player> findAllOrderedByRankDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "SELECT * FROM players ORDER BY rank DESC LIMIT :limitCount", nativeQuery = true)
    List<Player> findAllOrderedByRankDesc(@Param("limitCount") Integer limitCount);
}
