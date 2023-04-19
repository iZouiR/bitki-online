package self.izouir.bitkionline.commander.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import self.izouir.bitkionline.entity.egg.EggType;
import self.izouir.bitkionline.exception.ImageNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

@Slf4j
@UtilityClass
public class ImageCommander {
    private static final Path BASE_EGG_IMAGE_PATH = Path.of("src/main/resources/image/egg");
    private static final Random GENERATOR = new Random();

    public static String generateEggImagePath(EggType eggType) {
        Path typedEggImagePath = Path.of(BASE_EGG_IMAGE_PATH.toString(), "/", eggType.toString().toLowerCase());
        try (Stream<Path> eggImagePathsStream = Files.list(typedEggImagePath)) {
            List<String> eggImagePaths = eggImagePathsStream
                    .map(Path::toString)
                    .toList();
            if (!eggImagePaths.isEmpty()) {
                int random = GENERATOR.nextInt(eggImagePaths.size());
                return eggImagePaths.get(random);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        throw new ImageNotFoundException("Egg images for egg type " + eggType.toString().toLowerCase() + " were not found");
    }
}
