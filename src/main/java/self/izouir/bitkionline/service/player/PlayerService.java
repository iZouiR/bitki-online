package self.izouir.bitkionline.service.player;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.PlayerNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerRepository;
import self.izouir.bitkionline.service.egg.EggService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static self.izouir.bitkionline.constants.PlayerServiceConstants.*;

@Service
public class PlayerService {
    private final EggService eggService;
    private final PlayerBotService playerBotService;
    private final PlayerRepository playerRepository;

    @Autowired
    public PlayerService(EggService eggService,
                         PlayerBotService playerBotService,
                         PlayerRepository playerRepository) {
        this.eggService = eggService;
        this.playerBotService = playerBotService;
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

    @Transactional
    public void createNotRegisteredPlayer(Long chatId) {
        Player player = Player.builder()
                .chatId(chatId)
                .username(String.format(NOT_REGISTERED_PLAYER_USERNAME, chatId))
                .rank(NOT_REGISTERED_PLAYER_RANK)
                .isPlaying(NOT_REGISTERED_PLAYER_IS_PLAYING)
                .build();
        save(player);
        playerBotService.createNotRegisteredPlayerBot(player);
    }

    @Transactional
    public void registerPlayer(DispatcherBot bot, Player player, String username) {
        player.setUsername(username);
        player.setRegisteredAt(Timestamp.from(Instant.now()));
        save(player);
        eggService.generateStartInventory(bot, player);
        playerBotService.registerPlayerBot(player);
    }
}
