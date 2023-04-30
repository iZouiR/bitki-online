package self.izouir.bitkionline.util.constants.commander;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HelpCommanderConstants {
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
