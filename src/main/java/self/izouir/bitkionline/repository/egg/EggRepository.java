package self.izouir.bitkionline.repository.egg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;

import java.util.List;

@Repository
public interface EggRepository extends JpaRepository<Egg, Long> {
    List<Egg> findAllByOwner(Player owner);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Egg SET owner = null WHERE owner = :owner")
    void unbindAllByOwner(@Param("owner") Player owner);
}
