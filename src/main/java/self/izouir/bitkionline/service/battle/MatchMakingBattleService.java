package self.izouir.bitkionline.service.battle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.MatchMakingBattle;
import self.izouir.bitkionline.repository.battle.MatchMakingBattleRepository;

@Slf4j
@Service
public class MatchMakingBattleService {
    private final MatchMakingBattleRepository matchMakingBattleRepository;

    @Autowired
    public MatchMakingBattleService(MatchMakingBattleRepository matchMakingBattleRepository) {
        this.matchMakingBattleRepository = matchMakingBattleRepository;
    }

    public void save(MatchMakingBattle matchMakingBattle) {
        matchMakingBattleRepository.save(matchMakingBattle);
    }
}
