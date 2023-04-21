package self.izouir.bitkionline.service.battle;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.PrivateBattleNotFoundException;
import self.izouir.bitkionline.repository.battle.PrivateBattleRepository;

import java.util.Random;

@Slf4j
@Service
public class PrivateBattleService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#_";
    private final PrivateBattleRepository privateBattleRepository;
    private final Random random;

    @Autowired
    public PrivateBattleService(PrivateBattleRepository privateBattleRepository) {
        this.random = new Random();
        this.privateBattleRepository = privateBattleRepository;
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
        privateBattleRepository.delete(privateBattle);
    }

    public PrivateBattle generatePrivateBattle(Player player) {
        return PrivateBattle.builder()
                .link(generateLink())
                .firstPlayer(player)
                .build();
    }

    private String generateLink() {
        StringBuilder link = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            link.append(ALPHABET.toCharArray()[random.nextInt(ALPHABET.length())]);
        }
        while (existsByLink(link.toString())) {
            link = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                link.append(ALPHABET.toCharArray()[random.nextInt(ALPHABET.length())]);
            }
        }
        return link.toString();
    }

    public boolean awaitConnection(PrivateBattle privateBattle) {
        Long id = privateBattle.getId();
        try {
            int counter = 0;
            while (privateBattle.getSecondPlayer() == null) {
                Thread.sleep(1000);
                counter++;
                if (counter >= 120) {
                    delete(privateBattle);
                    return false;
                }
                privateBattle = findById(id);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return true;
    }
}
