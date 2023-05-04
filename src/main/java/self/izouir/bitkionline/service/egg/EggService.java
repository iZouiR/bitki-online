package self.izouir.bitkionline.service.egg;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.egg.EggAttackType;
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.exception.EggNotFoundException;
import self.izouir.bitkionline.exception.ImageNotFoundException;
import self.izouir.bitkionline.repository.egg.EggRepository;
import self.izouir.bitkionline.service.player.PlayerStatisticsService;

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
import static self.izouir.bitkionline.util.constant.MessageConstant.OBTAINING_EGG_MESSAGE;
import static self.izouir.bitkionline.util.constant.service.EggServiceConstant.*;

@RequiredArgsConstructor
@Service
public class EggService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EggService.class);
    private final PlayerStatisticsService playerStatisticsService;
    private final EggRepository eggRepository;
    private final Random random = new Random();

    public Egg findById(final Long id) {
        return eggRepository.findById(id).orElseThrow(
                () -> new EggNotFoundException("Egg with id = " + id + " wasn't found"));
    }

    public List<Egg> findAllByOwner(final Player owner) {
        return eggRepository.findAllByOwner(owner);
    }

    public List<Egg> findAllByOwnerWhereIsNotCracked(final Player player) {
        final List<Egg> inventory = eggRepository.findAllByOwner(player);
        return inventory.stream().filter(egg -> !egg.getIsCracked()).collect(Collectors.toList());
    }

    public void save(final Egg egg) {
        eggRepository.save(egg);
    }

    @Transactional
    public void deleteAllByOwner(final Player owner) {
        eggRepository.deleteAllByOwner(owner);
    }

    @Transactional
    public void unbindAllByOwner(final Player owner) {
        eggRepository.unbindAllByOwner(owner);
    }

    public void generateStartInventory(final DispatcherBot bot, final Player player) {
        for (int i = 0; i < START_INVENTORY_SIZE; i++) {
            final Egg egg = generateEgg();
            egg.setOwner(player);
            save(egg);
            playerStatisticsService.incrementEggsObtained(player, egg);
            sendSticker(bot, player.getChatId(), Path.of(egg.getImagePath()));
            sendMessage(bot, player.getChatId(), String.format(OBTAINING_EGG_MESSAGE, egg.getName(), generateStatsInfo(egg)));
        }
    }

    private Egg generateEgg() {
        final Egg egg;
        final int chance = random.nextInt(100);
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

    private String generateImagePath(final EggType eggType) {
        final Path typedEggImagePath = Path.of(BASE_EGG_IMAGE_PATH, eggType.toString().toLowerCase());
        try (final Stream<Path> eggImagePathStream = Files.list(typedEggImagePath)) {
            final List<String> eggImagePaths = eggImagePathStream
                    .map(Path::toString)
                    .toList();
            if (!eggImagePaths.isEmpty()) {
                return eggImagePaths.get(random.nextInt(eggImagePaths.size()));
            }
        } catch (final IOException e) {
            LOGGER.error(e.getMessage());
        }
        throw new ImageNotFoundException("Egg images for egg type " + eggType.toString().toLowerCase() + " weren't found");
    }

    private String generateName(final Egg egg) {
        String name = egg.getImagePath();
        for (final EggType eggType : EggType.values()) {
            name = name.replace(BASE_EGG_IMAGE_PATH + eggType.toString().toLowerCase() + "/", "");
        }
        name = name.replace(".png", "");
        return name;
    }

    public String generateStatsInfo(final Egg egg) {
        final StringBuilder statsInfo = new StringBuilder();
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

    public Integer generateDamage(final Egg egg, final EggAttackType attackType) {
        double damage = egg.getPower();
        switch (attackType) {
            case HEAD -> {
                damage += HEAD_ATTACK_ENDURANCE_POWER_COEFFICIENT * egg.getEndurance();
                damage *= HEAD_ATTACK_POWER_COEFFICIENT;
            }
            case SIDE -> {
                damage += SIDE_ATTACK_ENDURANCE_POWER_COEFFICIENT * egg.getEndurance();
                damage *= SIDE_ATTACK_POWER_COEFFICIENT;
            }
            case ASS -> {
                damage += ASS_ATTACK_ENDURANCE_POWER_COEFFICIENT * egg.getEndurance();
                damage *= ASS_ATTACK_POWER_COEFFICIENT;
            }
        }
        return Math.toIntExact(Math.round(damage));
    }

    public Integer generateReplyDamage(final Egg egg, final EggAttackType attackType) {
        double replyDamage = egg.getPower() + MINIMUM_REPLY_DAMAGE;
        switch (attackType) {
            case HEAD ->
                    replyDamage -= HEAD_ATTACK_REPLY_DAMAGE_PAY_OFF_INTELLIGENCE_COEFFICIENT * egg.getIntelligence();
            case SIDE ->
                    replyDamage -= SIDE_ATTACK_REPLY_DAMAGE_PAY_OFF_INTELLIGENCE_COEFFICIENT * egg.getIntelligence();
            case ASS -> replyDamage -= ASS_ATTACK_REPLY_DAMAGE_PAY_OFF_INTELLIGENCE_COEFFICIENT * egg.getIntelligence();
        }
        if (replyDamage < MINIMUM_REPLY_DAMAGE) {
            replyDamage = MINIMUM_REPLY_DAMAGE;
        }
        return Math.toIntExact(Math.round(replyDamage));
    }

    public Integer generateChanceOfAttack(final Egg attackerEgg, final Egg defenderEgg, final EggAttackType attackType) {
        double chance = 100.0 * attackerEgg.getLuck() / (attackerEgg.getLuck() + defenderEgg.getLuck());
        switch (attackType) {
            case HEAD -> chance *= HEAD_ATTACK_CHANCE_COEFFICIENT;
            case SIDE -> chance *= SIDE_ATTACK_CHANCE_COEFFICIENT;
            case ASS -> chance *= ASS_ATTACK_CHANCE_COEFFICIENT;
        }
        chance += MINIMUM_ATTACK_CHANCE;
        if (chance > 100) {
            chance = 100;
        }
        return Math.toIntExact(Math.round(chance));
    }

    public void applyDamage(final Egg egg, final Integer damage) {
        egg.setEndurance(egg.getEndurance() - damage);
        if (egg.getEndurance() <= 0) {
            egg.setEndurance(0);
            egg.setIsCracked(true);
        }
        save(egg);
    }
}
