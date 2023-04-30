package self.izouir.bitkionline.service.egg;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.EggNotFoundException;
import self.izouir.bitkionline.exception.ImageNotFoundException;
import self.izouir.bitkionline.repository.egg.EggRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;
import static self.izouir.bitkionline.util.BotMessageSender.sendSticker;
import static self.izouir.bitkionline.util.constants.MessageConstants.OBTAINING_EGG_MESSAGE;
import static self.izouir.bitkionline.util.constants.service.EggServiceConstants.*;

@Slf4j
@Service
public class EggService {
    private final EggRepository eggRepository;
    private final Random random;

    @Autowired
    public EggService(EggRepository eggRepository) {
        this.eggRepository = eggRepository;
        this.random = new Random();
    }

    public Egg findById(Long id) {
        return eggRepository.findById(id).orElseThrow(
                () -> new EggNotFoundException("Egg with id = " + id + " wasn't found"));
    }

    public List<Egg> findAllByOwner(Player owner) {
        return eggRepository.findAllByOwner(owner);
    }

    public List<Egg> findAllByOwnerWhereIsNotCracked(Player player) {
        List<Egg> inventory = eggRepository.findAllByOwner(player);
        return inventory.stream().filter(egg -> !egg.getIsCracked()).collect(Collectors.toList());
    }

    public void save(Egg egg) {
        eggRepository.save(egg);
    }

    @Transactional
    public void deleteAllByOwner(Player owner) {
        eggRepository.deleteAllByOwner(owner);
    }

    @Transactional
    public void unbindAllByOwner(Player owner) {
        eggRepository.unbindAllByOwner(owner);
    }

    public void generateStartInventory(DispatcherBot bot, Player player) {
        for (int i = 0; i < START_INVENTORY_SIZE; i++) {
            Egg egg = generateEgg();
            egg.setOwner(player);
            save(egg);
            sendSticker(bot, player.getChatId(), Path.of(egg.getImagePath()));
            sendMessage(bot, player.getChatId(), String.format(OBTAINING_EGG_MESSAGE, egg.getName(), generateStatsInfo(egg)));
        }
    }

    private Egg generateEgg() {
        Egg egg;
        int chance = random.nextInt(EGG_GENERATION_CHANCE);
        if (chance < WEAK_EGG_GENERATION_CHANCE) {
            egg = generateWeakEgg();
        } else if (chance < WEAK_EGG_GENERATION_CHANCE + STRONG_EGG_GENERATION_CHANCE) {
            egg = generateStrongEgg();
        } else {
            egg = generateHolyEgg();
        }
        egg.setName(generateName(egg));
        egg.setIsCracked(NEW_EGG_IS_CRACKED);
        egg.setCreatedAt(Timestamp.from(Instant.now()));
        return egg;
    }

    private Egg generateWeakEgg() {
        return Egg.builder()
                .type(EggType.WEAK)
                .endurance(random.nextInt(WEAK_EGG_MINIMUM_ENDURANCE, WEAK_EGG_MAXIMUM_ENDURANCE + 1))
                .power(random.nextInt(WEAK_EGG_MINIMUM_POWER, WEAK_EGG_MAXIMUM_POWER + 1))
                .luck(random.nextInt(WEAK_EGG_MINIMUM_LUCK, WEAK_EGG_MAXIMUM_LUCK + 1))
                .intelligence(random.nextInt(WEAK_EGG_MINIMUM_INTELLIGENCE, WEAK_EGG_MAXIMUM_INTELLIGENCE + 1))
                .imagePath(generateImagePath(EggType.WEAK))
                .build();
    }

    private Egg generateStrongEgg() {
        return Egg.builder()
                .type(EggType.STRONG)
                .endurance(random.nextInt(STRONG_EGG_MINIMUM_ENDURANCE, STRONG_EGG_MAXIMUM_ENDURANCE + 1))
                .power(random.nextInt(STRONG_EGG_MINIMUM_POWER, STRONG_EGG_MAXIMUM_POWER + 1))
                .luck(random.nextInt(STRONG_EGG_MINIMUM_LUCK, STRONG_EGG_MAXIMUM_LUCK + 1))
                .intelligence(random.nextInt(STRONG_EGG_MINIMUM_INTELLIGENCE, STRONG_EGG_MAXIMUM_INTELLIGENCE + 1))
                .imagePath(generateImagePath(EggType.STRONG))
                .build();
    }

    private Egg generateHolyEgg() {
        return Egg.builder()
                .type(EggType.HOLY)
                .endurance(random.nextInt(HOLY_EGG_MINIMUM_ENDURANCE, HOLY_EGG_MAXIMUM_ENDURANCE + 1))
                .power(random.nextInt(HOLY_EGG_MINIMUM_POWER, HOLY_EGG_MAXIMUM_POWER + 1))
                .luck(random.nextInt(HOLY_EGG_MINIMUM_LUCK, HOLY_EGG_MAXIMUM_LUCK + 1))
                .intelligence(random.nextInt(HOLY_EGG_MINIMUM_INTELLIGENCE, HOLY_EGG_MAXIMUM_INTELLIGENCE + 1))
                .imagePath(generateImagePath(EggType.HOLY))
                .build();
    }

    private String generateImagePath(EggType eggType) {
        Path typedEggImagePath = Path.of(BASE_EGG_IMAGE_PATH, eggType.toString().toLowerCase());
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

    private String generateName(Egg egg) {
        String name = egg.getImagePath();
        for (EggType eggType : EggType.values()) {
            name = name.replace(BASE_EGG_IMAGE_PATH + eggType.toString().toLowerCase() + "\\", "");
        }
        name = name.replace(".png", "");
        return name;
    }

    public String generateStatsInfo(Egg egg) {
        StringBuilder statsInfo = new StringBuilder();
        statsInfo.append(egg.getEndurance());
        if (egg.getEndurance() >= GREAT_ENDURANCE) {
            statsInfo.append("🛡 ");
        } else {
            statsInfo.append("❤️‍🩹 ");
        }
        statsInfo.append(egg.getPower());
        if (egg.getPower() >= GREAT_POWER) {
            statsInfo.append("💣 ");
        } else {
            statsInfo.append("💥 ");
        }
        statsInfo.append(egg.getLuck());
        if (egg.getLuck() >= GREAT_LUCK) {
            statsInfo.append("🍀 ");
        } else {
            statsInfo.append("☘️ ");
        }
        statsInfo.append(egg.getIntelligence());
        if (egg.getIntelligence() >= GREAT_INTELLIGENCE) {
            statsInfo.append("🪬");
        } else {
            statsInfo.append("🧿");
        }
        return statsInfo.toString();
    }
}
