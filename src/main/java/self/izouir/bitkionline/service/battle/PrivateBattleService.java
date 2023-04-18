package self.izouir.bitkionline.service.battle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.repository.battle.PrivateBattleRepository;

@Service
public class PrivateBattleService {
    private final PrivateBattleRepository privateBattleRepository;

    @Autowired
    public PrivateBattleService(PrivateBattleRepository privateBattleRepository) {
        this.privateBattleRepository = privateBattleRepository;
    }
}
