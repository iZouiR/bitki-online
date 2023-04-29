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
    public static final String CRACKED_EGG_MESSAGE = "%s egg, cracked!";
    public static final String NOT_CRACKED_EGG_MESSAGE = "%s egg, not cracked";
    public static final String EMPTY_INVENTORY_MESSAGE = "You don't have any eggs";

    public static final String HELP_MESSAGE = """
            At beta you enter the game with 3 random eggs
                            
            All the eggs have their characteristics:
                - Endurance (❤️‍🩹/🛡) - Describes the max amount of damage the egg could take, influences Power (💥/💣)
                - Power (💥/💣) - Describes the amount of damage the egg could deal
                - Luck (☘️/🍀) - Describes the amount of chance to damage enemy's egg
                - Intelligence (🧿/🪬) - Describes the reduction of reply damage taken while attacking enemy egg
                      
            Types of battles:
                Match Making Battle - battle with random opponent
                Private Battle - battle via link
                
            Rules of game:
                1. The battle starts by choosing who would attack first - the coin flip
                2. Then attacker choose one of 3 variants of attack
                    (higher damage - lower chance)
                    (medium damage - medium chance)
                    (lower damage - higher chance)
                3. Then chance and damage applies, calculating endurance of both battling eggs
                4. Turn goes to the next player and points 2-4 become cycled
                5. One or both eggs are broken
                    (one broken egg belongs to defeated player)
                    (both eggs are broken - draw)
                6. Calculates awards of winner, punishments of looser
            """;
}
