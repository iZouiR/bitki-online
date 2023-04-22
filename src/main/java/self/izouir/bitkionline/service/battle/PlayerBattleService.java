package self.izouir.bitkionline.service.battle;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.repository.battle.PlayerBattleRepository;

@Service
public class PlayerBattleService {
    private final PlayerBattleRepository playerBattleRepository;

    @Autowired
    public PlayerBattleService(PlayerBattleRepository playerBattleRepository) {
        this.playerBattleRepository = playerBattleRepository;
    }

    public void save(PlayerBattle battle) {
        playerBattleRepository.save(battle);
    }

    @Transactional
    public void delete(PlayerBattle playerBattle) {
        playerBattleRepository.delete(playerBattle);
    }

    public PlayerBattle generatePlayerBattle(Player player) {
        return PlayerBattle.builder()
                .firstPlayer(player)
                .build();
    }

    public PlayerBattle generatePlayerBattle(Player player, Player opponent) {
        return PlayerBattle.builder()
                .firstPlayer(player)
                .secondPlayer(opponent)
                .build();
    }
}
