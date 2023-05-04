package self.izouir.bitkionline.util.constant;

import lombok.experimental.UtilityClass;

import static self.izouir.bitkionline.util.constant.service.PlayerServiceConstant.MAXIMUM_USERNAME_LENGTH;
import static self.izouir.bitkionline.util.constant.service.PlayerServiceConstant.MINIMUM_USERNAME_LENGTH;

@UtilityClass
public class MessageConstant {
    public static final String PLAYER_NOT_REGISTERED_MESSAGE = "You aren't registered - /start";
    public static final String PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE = "Before this finish registration";
    public static final String INCORRECT_USERNAME_FORMAT_MESSAGE = "Entered username has incorrect format, it must contains english/russian letters + digits + '_' and be between "
                                                                   + MINIMUM_USERNAME_LENGTH + " and " + MAXIMUM_USERNAME_LENGTH + " characters in length";
    public static final String USERNAME_ALREADY_EXISTS_MESSAGE = "Username %s already exists, try another variant";
    public static final String OBTAINING_EGG_MESSAGE = "You obtained \"%s\" egg (%s)";
    public static final String CRACKED_EGG_MESSAGE = "\"%s\" egg, cracked!";
    public static final String NOT_CRACKED_EGG_MESSAGE = "\"%s\" egg, not cracked";
    public static final String EMPTY_INVENTORY_MESSAGE = "You don't have any not cracked eggs";
}
