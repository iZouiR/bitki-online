package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.service.egg.EggService;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;

import static self.izouir.bitkionline.commander.util.BotCommander.sendMessage;
import static self.izouir.bitkionline.commander.util.BotCommander.sendSticker;
import static self.izouir.bitkionline.commander.util.ImageCommander.generateEggImagePath;

@Component
public class EggCommander {
    private final Random generator;
    private final EggService eggService;

    @Autowired
    public EggCommander(EggService eggService) {
        this.generator = new Random();
        this.eggService = eggService;
    }

    public void generateStarterEggs(DispatcherBot bot, Long chatId, Player player) {
        for (int i = 0; i < 3; i++) {
            Egg egg = generateEgg();
            egg.setOwner(player);
            eggService.save(egg);
            sendSticker(bot, chatId, Path.of(egg.getImagePath()));
            sendMessage(bot, chatId, "You obtained " + egg.getType().toString().toLowerCase() + " egg");
        }
    }

    public void deleteAllPlayersEggs(Player player) {
        eggService.deleteAllByOwner(player);
    }

    public Egg generateEgg() {
        Egg egg;

        int random = generator.nextInt(100);
        if (random < 80) {
            egg = generateWeakEgg();
        } else if (random < 95) {
            egg = generateStrongEgg();
        } else {
            egg = generateHolyEgg();
        }

        return egg;
    }
    // endurance 5-10
    // luck 0-3
    // intelligence 0-3
    private Egg generateWeakEgg() {
        Egg egg = Egg.builder()
                .type(EggType.WEAK)
                .build();

        long endurance = generator.nextLong(6L) + 5L;
        long luck = generator.nextLong(4L);
        long intelligence = generator.nextLong(4L);

        egg.setEndurance(endurance);
        egg.setLuck(luck);
        egg.setIntelligence(intelligence);

        egg.setImagePath(generateEggImagePath(EggType.WEAK));

        egg.setCreatedAt(Timestamp.from(Instant.now()));

        return egg;
    }
    // endurance 8-18
    // luck 1-5
    // intelligence 1-5
    private Egg generateStrongEgg() {
        Egg egg = Egg.builder()
                .type(EggType.STRONG)
                .build();

        long endurance = generator.nextLong(11L) + 8L;
        long luck = generator.nextLong(5L) + 1L;
        long intelligence = generator.nextLong(5L) + 1L;

        egg.setEndurance(endurance);
        egg.setLuck(luck);
        egg.setIntelligence(intelligence);

        egg.setImagePath(generateEggImagePath(EggType.STRONG));

        egg.setCreatedAt(Timestamp.from(Instant.now()));

        return egg;
    }
    // endurance 12-24
    // luck 3-7
    // intelligence 3-7
    private Egg generateHolyEgg() {
        Egg egg = Egg.builder()
                .type(EggType.HOLY)
                .build();

        long endurance = generator.nextLong(13L) + 12L;
        long luck = generator.nextLong(5L) + 3L;
        long intelligence = generator.nextLong(5L) + 3L;

        egg.setEndurance(endurance);
        egg.setLuck(luck);
        egg.setIntelligence(intelligence);

        egg.setImagePath(generateEggImagePath(EggType.HOLY));

        egg.setCreatedAt(Timestamp.from(Instant.now()));

        return egg;
    }
}
