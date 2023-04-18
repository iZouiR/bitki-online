package self.izouir.bitkionline.service.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.PlayerNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerRepository;

import java.util.Optional;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player findByChatId(Long chatId) {
        Optional<Player> optional = playerRepository.findByChatId(chatId);
        return optional.orElseThrow(() -> new PlayerNotFoundException("Player with chatId = " + chatId + " was not found"));
    }

    public boolean existsByChatId(Long chatId) {
        return playerRepository.findByChatId(chatId).isPresent();
    }

    public boolean existsByUsername(String username) {
        return playerRepository.findByUsername(username).isPresent();
    }

    public void save(Player player) {
        playerRepository.save(player);
    }
}
