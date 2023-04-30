package self.izouir.bitkionline.util.constants.commander;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BattleCommanderConstants {
    public static final Integer EGG_CHOICE_SECONDS = 30;
    public static final Integer ATTACK_CHOICE_SECONDS = 30;
    public static final Integer COUNT_DOWN_SECONDS = 3;
    public static final String COUNT_DOWN_MESSAGE = "%d...";
    public static final String COIN_FLIP_MESSAGE = "Coin flip... \uD83E\uDE99";
    public static final String EGG_CHOICE_MESSAGE = "Choose egg for the fight, you have " + EGG_CHOICE_SECONDS + " seconds! ⏱";
    public static final String EGG_CHOICE_SUCCESS_MESSAGE = "You made your choice, waiting for the opponent... ⏱";
    public static final String ATTACKER_MESSAGE = "Your turn to attack, you have " + ATTACK_CHOICE_SECONDS + " seconds! \uD83C\uDFB0";
    public static final String DEFENDER_MESSAGE = "Your turn to defend, pray to god... \uD83D\uDCE1";
    public static final String ATTACK_MESSAGE = "%s trying to attack %s of %s \uD83D\uDDE1";
    public static final String PLAYER_DISCONNECTION_MESSAGE = "%s was disconnected \uD83C\uDF10";
    public static final String WINNER_MESSAGE = "You did it! %s was defeated \uD83C\uDF7E";
    public static final String LOOSER_MESSAGE = "Sad to say, you were defeated by %s ⚰️";
    public static final String DRAW_MESSAGE = "You played draw, eggs were cracked, ranks remain with their current states \uD83D\uDD4A";
    public static final String OPPONENT_SURRENDER_MESSAGE = "%s surrendered, you're tough enough to win without a fight \uD83E\uDDB7";
    public static final String SURRENDER_MESSAGE = "You're a coward, defeated without battle \uD83E\uDD21";
    public static final String OPPONENT_INFO_BUTTON_TEXT = "%s (%d \uD83C\uDF96) ☠️";
    public static final String JOIN_FIGHT_BUTTON_TEXT = "Ok, let's dance!";
    public static final String SURRENDER_BUTTON_TEXT = "Surrender";
    public static final String HEAD_ATTACK_BUTTON_TEXT = "Head (\uD83D\uDC80): %s 💥 (%s%%)";
    public static final String SIDE_ATTACK_BUTTON_TEXT = "Side (\uD83D\uDC7B): %s 💥 (%s%%)";
    public static final String ASS_ATTACK_BUTTON_TEXT = "Ass (\uD83D\uDCA9): %s 💥 (%s%%)";

    public static final String ATTACK_SUCCESS_ATTACKER_MESSAGE = "%s : \"You got me! \uD83E\uDD2F\"\n";
    public static final String ATTACK_SUCCESS_DEFENDER_MESSAGE = "%s : \"Does it hurt?! \uD83D\uDE08\"\n";
    public static final String ATTACK_FAIL_ATTACKER_MESSAGE = "%s : \"I'M FUCKING INVINCIBLE!!! \uD83C\uDF83\"\n";
    public static final String ATTACK_FAIL_DEFENDER_MESSAGE = "%s : \"Lucky devil... \uD83D\uDC7F\"\n";
    public static final String PLAYER_EGGS_ENDURANCE_MESSAGE = "You - %d❤️\u200D\uD83E\uDE79(-%d) | Opponent - %d❤️\u200D\uD83E\uDE79(-%d)";
}
