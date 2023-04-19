package self.izouir.bitkionline.service.egg;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.repository.egg.EggRepository;

@Service
public class EggService {
    private final EggRepository eggRepository;

    @Autowired
    public EggService(EggRepository eggRepository) {
        this.eggRepository = eggRepository;
    }

    public void save(Egg egg) {
        eggRepository.save(egg);
    }

    @Transactional
    public void deleteAllByOwner(Player owner) {
        eggRepository.deleteAllByOwner(owner);
    }
}
