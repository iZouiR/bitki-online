package self.izouir.bitkionline.util.constant.commander;

import lombok.experimental.UtilityClass;

import static self.izouir.bitkionline.util.constant.service.PlayerServiceConstant.MAXIMUM_USERNAME_LENGTH;
import static self.izouir.bitkionline.util.constant.service.PlayerServiceConstant.MINIMUM_USERNAME_LENGTH;

@UtilityClass
public class ProfileCommanderConstant {
    public static final String STATISTICS_MESSAGE = """
            Win rate - (%s%%) 🎲
            Head attack success rate - (%s%%) 🎰
            Side attack success rate - (%s%%) 🎰
            Ass attack success rate - (%s%%) 🎰
            Total damage dealt - %s 💥
            Total damage taken - %s 💥
            Total rank points earned - %s 🎖
            Total rank points lost - %s 🎖
            Total eggs obtained - %s 🔮
            Holy eggs obtained - %s 🔮
            Strong eggs obtained - %s 🔮
            Weak eggs obtained - %s 🔮
            """;
    public static final String AWAIT_NEW_USERNAME_MESSAGE = "Enter your new username, it must contains english/russian letters + digits + '_' and be between "
                                                            + MINIMUM_USERNAME_LENGTH + " and " + MAXIMUM_USERNAME_LENGTH + " characters in length";
    public static final String USERNAME_CHANGE_SUCCESS_MESSAGE = "Success, you were renamed to %s";
    public static final String EGGS_REFRESH_CONFIRMATION_MESSAGE = "Are you sure you want to refresh your eggs?";
    public static final String EGGS_REFRESH_SUCCESS_MESSAGE = "All your eggs were refreshed";
    public static final String PROFILE_DROP_CONFIRMATION_MESSAGE = "Are you sure you want to drop your profile?";
    public static final String PROFILE_DROP_SUCCESS_MESSAGE = "Your profile was dropped successfully";

    public static final String STATISTICS_BUTTON_TEXT = "Statistics";
    public static final String CHANGE_USERNAME_BUTTON_TEXT = "Change username";
    public static final String REFRESH_EGGS_BUTTON_TEXT = "Refresh eggs";
    public static final String DROP_PROFILE_BUTTON_TEXT = "Drop profile";
}
