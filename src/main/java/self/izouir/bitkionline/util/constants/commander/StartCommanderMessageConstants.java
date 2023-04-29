package self.izouir.bitkionline.util.constants.commander;

import lombok.experimental.UtilityClass;

import static self.izouir.bitkionline.util.constants.service.PlayerServiceConstants.MAXIMUM_USERNAME_LENGTH;
import static self.izouir.bitkionline.util.constants.service.PlayerServiceConstants.MINIMUM_USERNAME_LENGTH;

@UtilityClass
public class StartCommanderMessageConstants {
    public static final String GREETINGS_MESSAGE = "Christ is risen, %s!";
    public static final String AWAIT_USERNAME_MESSAGE = "Enter your username, it must contains english/russian letters + digits + '_' and be between "
                                                        + MINIMUM_USERNAME_LENGTH + " and " + MAXIMUM_USERNAME_LENGTH + " characters in length";
    public static final String PLAYER_REGISTERED_MESSAGE = "You're now registered with username %s";
}
