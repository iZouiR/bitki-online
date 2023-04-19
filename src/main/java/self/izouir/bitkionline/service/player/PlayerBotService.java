package self.izouir.bitkionline.service.player;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.exception.PlayerBotNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerBotRepository;

import java.util.Optional;

@Service
public class PlayerBotService {
    private final PlayerBotRepository playerBotRepository;

    @Autowired
    public PlayerBotService(PlayerBotRepository playerBotRepository) {
        this.playerBotRepository = playerBotRepository;
    }

    public PlayerBot findByPlayerId(Long playerId) {
        Optional<PlayerBot> optional = playerBotRepository.findByPlayerId(playerId);
        return optional.orElseThrow(() -> new PlayerBotNotFoundException("Player bot with playerId = " + playerId + " was not found"));
    }

    public boolean existsByPlayerId(Long playerId) {
        return playerBotRepository.findByPlayerId(playerId).isPresent();
    }

    public void save(PlayerBot playerBot) {
        playerBotRepository.save(playerBot);
    }

    @Transactional
    public void deleteByPlayerId(Long playerId) {
        playerBotRepository.deleteByPlayerId(playerId);
    }
}
