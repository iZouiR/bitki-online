package self.izouir.bitkionline.service.battle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.repository.battle.MatchMakingBattleRepository;

@Service
public class MatchMakingBattleService {
    private final MatchMakingBattleRepository matchMakingBattleRepository;

    @Autowired
    public MatchMakingBattleService(MatchMakingBattleRepository matchMakingBattleRepository) {
        this.matchMakingBattleRepository = matchMakingBattleRepository;
    }
}
