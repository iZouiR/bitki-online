package self.izouir.bitkionline.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BotMessageSenderConstants {
    public static final String GREETINGS_MESSAGE = "Christ is risen, %s!";
    public static final String AWAIT_USERNAME_MESSAGE = "Enter your username";
    public static final String AWAIT_NEW_USERNAME_MESSAGE = "Enter your new username";
    public static final String USERNAME_CHANGE_SUCCESS_MESSAGE = "Success, you're now named %s";
    public static final String PLAYER_NOT_REGISTERED_MESSAGE = "Finish registration before continuing";
    public static final String PLAYER_NOT_EXISTS_MESSAGE = "You aren't registered yet - /start";
    public static final String PLAYER_REGISTERED_MESSAGE = "You're now registered with username %s";
    public static final String PLAYER_ALREADY_EXISTS_MESSAGE = "Player with username %s already exists, try another variant";
    public static final String EGGS_REFRESH_CONFIRMATION_MESSAGE = "Are you sure you want to refresh your eggs?";
    public static final String EGGS_REFRESH_SUCCESS_MESSAGE = "All your eggs were refreshed";
    public static final String PROFILE_DROP_CONFIRMATION_MESSAGE = "Are you sure you want to drop your whole profile?";
    public static final String PROFILE_DROP_SUCCESS_MESSAGE = "Your profile was dropped successfully";

    public static final String OBTAINING_EGG_MESSAGE = "You obtained %s egg (%s)";
}
