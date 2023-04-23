package self.izouir.bitkionline.service.player;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.PlayerNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerRepository;

import java.util.List;
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

    public List<Player> findAllOrderedByRankDesc() {
        return playerRepository.findAllOrderedByRankDesc();
    }

    public List<Player> findAllOrderedByRankDesc(Long limitCount) {
        return playerRepository.findAllOrderedByRankDesc(limitCount);
    }

    public boolean existsByChatId(Long chatId) {
        return playerRepository.findByChatId(chatId).isPresent();
    }

    public boolean notExistsByUsernameIgnoreCase(String username) {
        return playerRepository.findByUsernameIgnoreCase(username).isEmpty();
    }

    public void save(Player player) {
        playerRepository.save(player);
    }

    @Transactional
    public void delete(Player player) {
        playerRepository.delete(player);
    }
}
