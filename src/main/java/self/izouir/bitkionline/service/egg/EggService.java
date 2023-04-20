package self.izouir.bitkionline.service.egg;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.ImageNotFoundException;
import self.izouir.bitkionline.repository.egg.EggRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;
import static self.izouir.bitkionline.util.BotMessageSender.sendSticker;

@Slf4j
@Service
public class EggService {
    private static final Path BASE_EGG_IMAGE_PATH = Path.of("src/main/resources/image/egg");
    private final EggRepository eggRepository;
    private final Random random;

    @Autowired
    public EggService(EggRepository eggRepository) {
        this.eggRepository = eggRepository;
        this.random = new Random();
    }

    public List<Egg> findAllByOwner(Player owner) {
        return eggRepository.findAllByOwner(owner);
    }

    public void save(Egg egg) {
        eggRepository.save(egg);
    }

    @Transactional
    public void deleteAllByOwner(Player owner) {
        eggRepository.deleteAllByOwner(owner);
    }

    public List<Egg> generateStartInventory(DispatcherBot bot, Player player) {
        List<Egg> inventory = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Egg egg = generateEgg();
            egg.setOwner(player);
            save(egg);
            inventory.add(egg);

            sendSticker(bot, player.getChatId(), Path.of(egg.getImagePath()));
            sendMessage(bot, player.getChatId(), "You obtained " + egg.getType().toString().toLowerCase() + " egg");
        }
        return inventory;
    }

    private Egg generateEgg() {
        Egg egg;
        int chance = random.nextInt(100);
        if (chance < 80) {
            egg = generateWeakEgg();
        } else if (chance < 95) {
            egg = generateStrongEgg();
        } else {
            egg = generateHolyEgg();
        }
        egg.setIsCracked(false);
        egg.setCreatedAt(Timestamp.from(Instant.now()));
        return egg;
    }

    private Egg generateWeakEgg() {
        Integer endurance = random.nextInt(6) + 5;
        Integer luck = random.nextInt(3) + 1;
        Integer intelligence = random.nextInt(4);

        return Egg.builder()
                .type(EggType.WEAK)
                .endurance(endurance)
                .luck(luck)
                .intelligence(intelligence)
                .imagePath(generateEggImagePath(EggType.WEAK))
                .build();
    }

    private Egg generateStrongEgg() {
        Integer endurance = random.nextInt(9) + 8;
        Integer luck = random.nextInt(4) + 2;
        Integer intelligence = random.nextInt(5) + 1;

        return Egg.builder()
                .type(EggType.STRONG)
                .endurance(endurance)
                .luck(luck)
                .intelligence(intelligence)
                .imagePath(generateEggImagePath(EggType.STRONG))
                .build();
    }

    private Egg generateHolyEgg() {
        Integer endurance = random.nextInt(13) + 12;
        Integer luck = random.nextInt(6) + 4;
        Integer intelligence = random.nextInt(7) + 3;

        return Egg.builder()
                .type(EggType.HOLY)
                .endurance(endurance)
                .luck(luck)
                .intelligence(intelligence)
                .imagePath(generateEggImagePath(EggType.HOLY))
                .build();
    }

    private String generateEggImagePath(EggType eggType) {
        Path typedEggImagePath = Path.of(BASE_EGG_IMAGE_PATH.toString(), "/", eggType.toString().toLowerCase());
        try (Stream<Path> eggImagePathStream = Files.list(typedEggImagePath)) {
            List<String> eggImagePaths = eggImagePathStream
                    .map(Path::toString)
                    .toList();
            if (!eggImagePaths.isEmpty()) {
                return eggImagePaths.get(random.nextInt(eggImagePaths.size()));
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        throw new ImageNotFoundException("Egg images for egg type " + eggType.toString().toLowerCase() + " weren't found");
    }
}
