package self.izouir.bitkionline.service.battle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.battle.PrivateBattle;
import self.izouir.bitkionline.exception.PrivateBattleNotFoundException;
import self.izouir.bitkionline.repository.battle.PrivateBattleRepository;

import java.util.Random;

@Slf4j
@Service
public class PrivateBattleService {
    private static final String LINK_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#_";
    private final PrivateBattleRepository privateBattleRepository;
    private final Random random;

    @Autowired
    public PrivateBattleService(PrivateBattleRepository privateBattleRepository) {
        this.random = new Random();
        this.privateBattleRepository = privateBattleRepository;
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

    public String generateLink() {
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
}
