package self.izouir.bitkionline.util.constants.commander;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlayCommanderConstants {
    public static final Integer AWAIT_OPPONENT_SECONDS = 120;
    public static final String PRIVATE_BATTLE_LINK_MESSAGE_ENTITY_TYPE = "code";

    public static final String CHOOSING_BATTLE_TYPE_MESSAGE = "Choose the type of battle";
    public static final String CHOOSING_PRIVATE_BATTLE_TYPE_MESSAGE = "Choose the strategy";
    public static final String AWAIT_MATCH_MAKING_BATTLE_OPPONENT_MESSAGE = "Waiting for an opponent... ⏱";
    public static final String AWAIT_PRIVATE_BATTLE_OPPONENT_PRE_LINK_MESSAGE = "Tell the opponent this link - ";
    public static final String AWAIT_PRIVATE_BATTLE_OPPONENT_MESSAGE =
            AWAIT_PRIVATE_BATTLE_OPPONENT_PRE_LINK_MESSAGE + "%s\nIt will be alive for " + AWAIT_OPPONENT_SECONDS + " seconds";
    public static final String AWAIT_PRIVATE_BATTLE_LINK_MESSAGE = "Enter the private battle link to continue";
    public static final String PRIVATE_BATTLE_NOT_AVAILABLE_MESSAGE = "Private battle with link %s is unavailable";
    public static final String PRIVATE_BATTLE_NOT_FOUND_MESSAGE = "Private battle with link %s wasn't found";
    public static final String OPPONENT_NOT_FOUND_MESSAGE = "Opponent to play with wasn't found";
    public static final String PLAYER_ALREADY_PLAYING_MESSAGE = "You're already playing, try later";

    public static final String MATCH_MAKING_BUTTON_TEXT = "Match making";
    public static final String PRIVATE_BUTTON_TEXT = "Private";
    public static final String CREATE_GAME_BUTTON_TEXT = "Create game";
    public static final String JOIN_GAME_BUTTON_TEXT = "Join game";
}
