package self.izouir.bitkionline.service.egg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.repository.egg.EggRepository;

@Service
public class EggService {
    private final EggRepository eggRepository;

    @Autowired
    public EggService(EggRepository eggRepository) {
        this.eggRepository = eggRepository;
    }
}
