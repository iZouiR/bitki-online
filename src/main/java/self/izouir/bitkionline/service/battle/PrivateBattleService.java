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

import java.util.Random;

import static self.izouir.bitkionline.constants.PrivateBattleServiceConstants.LINK_ALPHABET;
import static self.izouir.bitkionline.constants.PrivateBattleServiceConstants.LINK_LENGTH;

@Slf4j
@Service
public class PrivateBattleService {
    private final PlayerBattleService playerBattleService;
    private final PrivateBattleRepository privateBattleRepository;
    private final Random random;

    @Autowired
    public PrivateBattleService(PlayerBattleService playerBattleService,
                                PrivateBattleRepository privateBattleRepository) {
        this.playerBattleService = playerBattleService;
        this.privateBattleRepository = privateBattleRepository;
        this.random = new Random();
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
    public void deleteById(Long id) {
        privateBattleRepository.deleteById(id);
    }

    @Transactional
    public void deleteByLink(String link) {
        PrivateBattle privateBattle = findByLink(link);
        playerBattleService.deleteById(privateBattle.getPlayerBattle().getId());
        deleteById(privateBattle.getId());
    }

    @Transactional
    public void deleteAllByPlayerId(Long playerId) {
        privateBattleRepository.deleteAllByPlayerBattle_FirstPlayer_IdOrPlayerBattle_SecondPlayer_Id(playerId, playerId);
    }

    @Transactional
    public PrivateBattle createByFirstPlayer(Player firstPlayer) {
        PlayerBattle playerBattle = playerBattleService.createByFirstPlayer(firstPlayer);
        PrivateBattle privateBattle = PrivateBattle.builder()
                .playerBattle(playerBattle)
                .link(generateLink())
                .build();
        save(privateBattle);
        return privateBattle;
    }

    public String generateLink() {
        StringBuilder link = new StringBuilder();
        for (int i = 0; i < LINK_LENGTH; i++) {
            link.append(LINK_ALPHABET.toCharArray()[random.nextInt(LINK_ALPHABET.length())]);
        }
        while (existsByLink(link.toString())) {
            link = new StringBuilder();
            for (int i = 0; i < LINK_LENGTH; i++) {
                link.append(LINK_ALPHABET.toCharArray()[random.nextInt(LINK_ALPHABET.length())]);
            }
        }
        return link.toString();
    }
}
