package self.izouir.bitkionline.service.battle;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.PlayerBattleNotFoundException;
import self.izouir.bitkionline.repository.battle.PlayerBattleRepository;

@RequiredArgsConstructor
@Service
public class PlayerBattleService {
    private final PlayerBattleRepository playerBattleRepository;

    public PlayerBattle findById(Long id) {
        return playerBattleRepository.findById(id).orElseThrow(
                () -> new PlayerBattleNotFoundException("Player battle with id = " + id + " wasn't found"));
    }

    public void save(PlayerBattle battle) {
        playerBattleRepository.save(battle);
    }

    @Transactional
    public void deleteById(Long id) {
        playerBattleRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllByPlayer(Player player) {
        playerBattleRepository.deleteAllByFirstPlayerOrSecondPlayer(player, player);
    }

    public PlayerBattle create(Player player, Player opponent) {
        PlayerBattle battle = PlayerBattle.builder()
                .firstPlayer(player)
                .secondPlayer(opponent)
                .build();
        save(battle);
        return battle;
    }

    public PlayerBattle createByFirstPlayer(Player firstPlayer) {
        PlayerBattle battle = PlayerBattle.builder()
                .firstPlayer(firstPlayer)
                .build();
        save(battle);
        return battle;
    }

    public void setSecondPlayer(PlayerBattle playerBattle, Player player) {
        playerBattle.setSecondPlayer(player);
        save(playerBattle);
    }

    public void setFirstPlayerEgg(PlayerBattle playerBattle, Egg egg) {
        playerBattle.setFirstPlayerEgg(egg);
        save(playerBattle);
    }

    public void setSecondPlayerEgg(PlayerBattle playerBattle, Egg egg) {
        playerBattle.setSecondPlayerEgg(egg);
        save(playerBattle);
    }

    public void setIsFirstPlayerWinner(PlayerBattle playerBattle, Boolean isFirstPlayerWinner) {
        playerBattle.setIsFirstPlayerWinner(isFirstPlayerWinner);
        save(playerBattle);
    }
}
