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

    public PlayerBattle findById(final Long id) {
        return playerBattleRepository.findById(id).orElseThrow(
                () -> new PlayerBattleNotFoundException("Player battle with id = " + id + " wasn't found"));
    }

    public void save(final PlayerBattle battle) {
        playerBattleRepository.save(battle);
    }

    @Transactional
    public void deleteById(final Long id) {
        playerBattleRepository.deleteById(id);
    }

    @Transactional
    public void deleteAllByPlayer(final Player player) {
        playerBattleRepository.deleteAllByFirstPlayerOrSecondPlayer(player, player);
    }

    public PlayerBattle create(final Player player, final Player opponent) {
        final PlayerBattle battle = PlayerBattle.builder()
                .firstPlayer(player)
                .secondPlayer(opponent)
                .build();
        save(battle);
        return battle;
    }

    public PlayerBattle createByFirstPlayer(final Player firstPlayer) {
        final PlayerBattle battle = PlayerBattle.builder()
                .firstPlayer(firstPlayer)
                .build();
        save(battle);
        return battle;
    }

    public void applySecondPlayer(final PlayerBattle playerBattle, final Player player) {
        playerBattle.setSecondPlayer(player);
        save(playerBattle);
    }

    public void applyFirstPlayerEgg(final PlayerBattle playerBattle, final Egg egg) {
        playerBattle.setFirstPlayerEgg(egg);
        save(playerBattle);
    }

    public void applySecondPlayerEgg(final PlayerBattle playerBattle, final Egg egg) {
        playerBattle.setSecondPlayerEgg(egg);
        save(playerBattle);
    }

    public void applyIsFirstPlayerWinner(final PlayerBattle playerBattle, final Boolean isFirstPlayerWinner) {
        playerBattle.setIsFirstPlayerWinner(isFirstPlayerWinner);
        save(playerBattle);
    }
}
