package self.izouir.bitkionline.repository.egg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;

@Repository
public interface EggRepository extends JpaRepository<Egg, Long> {
    void deleteAllByOwner(Player owner);
}
