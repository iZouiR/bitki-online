package self.izouir.bitkionline.service.player;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.exception.PlayerBotNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerBotRepository;
import self.izouir.bitkionline.service.egg.EggService;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PlayerBotService {
    private final EggService eggService;
    private final PlayerBotRepository playerBotRepository;

    public PlayerBot findByPlayerId(Long playerId) {
        Optional<PlayerBot> optional = playerBotRepository.findByPlayerId(playerId);
        return optional.orElseThrow(() -> new PlayerBotNotFoundException("Player bot with playerId = " + playerId + " was not found"));
    }

    public void save(PlayerBot playerBot) {
        playerBotRepository.save(playerBot);
    }

    @Transactional
    public void deleteByPlayerId(Long playerId) {
        playerBotRepository.deleteByPlayerId(playerId);
    }

    public void setLastState(Player player, PlayerBotState state) {
        PlayerBot playerBot = findByPlayerId(player.getId());
        playerBot.setLastState(state);
        save(playerBot);
    }

    public void createNotRegisteredPlayerBot(Player player) {
        PlayerBot playerBot = PlayerBot.builder()
                .playerId(player.getId())
                .lastState(PlayerBotState.AWAIT_USERNAME)
                .build();
        save(playerBot);
    }

    public void registerPlayerBot(Player player) {
        List<Egg> playerInventory = eggService.findAllByOwner(player);
        PlayerBot playerBot = findByPlayerId(player.getId());
        playerBot.setLastState(PlayerBotState.NO_STATE);
        playerBot.setLastInventoryIndex(0);
        playerBot.setLastInventorySize(playerInventory.size());
        save(playerBot);
    }

    public void incrementLastInventoryIndex(Player player) {
        List<Egg> playerInventory = eggService.findAllByOwner(player);
        PlayerBot playerBot = findByPlayerId(player.getId());
        Integer inventoryIndex = playerBot.getLastInventoryIndex();
        inventoryIndex++;
        if (inventoryIndex > playerInventory.size() - 1) {
            inventoryIndex = 0;
        }
        playerBot.setLastInventoryIndex(inventoryIndex);
        save(playerBot);
    }

    public void decrementLastInventoryIndex(Player player) {
        List<Egg> playerInventory = eggService.findAllByOwner(player);
        PlayerBot playerBot = findByPlayerId(player.getId());
        Integer inventoryIndex = playerBot.getLastInventoryIndex();
        inventoryIndex--;
        if (inventoryIndex < 0) {
            inventoryIndex = playerInventory.size() - 1;
        }
        playerBot.setLastInventoryIndex(inventoryIndex);
        save(playerBot);
    }
}
