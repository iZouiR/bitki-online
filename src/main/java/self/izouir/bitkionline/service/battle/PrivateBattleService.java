package self.izouir.bitkionline.service.battle;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.PlayerBattle;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.PrivateBattleNotFoundException;
import self.izouir.bitkionline.repository.battle.PrivateBattleRepository;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class PrivateBattleService {
    private static final String LINK_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#_";
    private final PrivateBattleRepository privateBattleRepository;
    private final PlayerBattleService playerBattleService;
    private final Random random;

    @Autowired
    public PrivateBattleService(PrivateBattleRepository privateBattleRepository,
                                PlayerBattleService playerBattleService) {
        this.random = new Random();
        this.privateBattleRepository = privateBattleRepository;
        this.playerBattleService = playerBattleService;
    }

    public PrivateBattle findById(Long id) {
        return privateBattleRepository.findById(id)
                .orElseThrow(() -> new PrivateBattleNotFoundException("Private battle with id = " + id + " wasn't found"));
    }

    public PrivateBattle findByLink(String link) {
        return privateBattleRepository.findByLink(link)
                .orElseThrow(() -> new PrivateBattleNotFoundException("Private battle with link = " + link + " wasn't found"));
    }

    public boolean existsByLink(String link) {
        return privateBattleRepository.findByLink(link).isPresent();
    }

    public void save(PrivateBattle privateBattle) {
        privateBattleRepository.save(privateBattle);
    }

    @Transactional
    public void delete(PrivateBattle privateBattle) {
        playerBattleService.delete(privateBattle.getPlayerBattle());
        privateBattleRepository.delete(privateBattle);
    }

    @Transactional
    public void deleteAllByPlayer(Player player) {
        List<PrivateBattle> privateBattles = privateBattleRepository.findAllByPlayerBattle_FirstPlayerOrPlayerBattle_SecondPlayer(player, player);
        for (PrivateBattle privateBattle : privateBattles) {
            PlayerBattle battle = privateBattle.getPlayerBattle();
            playerBattleService.delete(battle);
            delete(privateBattle);
        }
    }

    public PrivateBattle generatePrivateBattle(Player player) {
        PlayerBattle battle = playerBattleService.generatePlayerBattle(player);
        playerBattleService.save(battle);
        return PrivateBattle.builder()
                .playerBattle(battle)
                .link(generateLink())
                .build();
    }

    private String generateLink() {
        StringBuilder link = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            link.append(LINK_ALPHABET.toCharArray()[random.nextInt(LINK_ALPHABET.length())]);
        }
        while (existsByLink(link.toString())) {
            link = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                link.append(LINK_ALPHABET.toCharArray()[random.nextInt(LINK_ALPHABET.length())]);
            }
        }
        return link.toString();
    }

    public boolean awaitConnection(PrivateBattle privateBattle) {
        Long id = privateBattle.getId();
        PlayerBattle battle = privateBattle.getPlayerBattle();
        try {
            int counter = 0;
            while (battle.getSecondPlayer() == null) {
                Thread.sleep(1000);
                counter++;
                if (counter >= 120) {
                    delete(privateBattle);
                    return false;
                }
                privateBattle = findById(id);
                battle = privateBattle.getPlayerBattle();
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return true;
    }

    public void awaitConnection() {
        try {
            int counter = 0;
            while (counter < 120) {
                Thread.sleep(1000);
                counter++;
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
