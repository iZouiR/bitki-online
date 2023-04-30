package self.izouir.bitkionline.util.constants.service;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlayerServiceConstants {
    public static final Integer MINIMUM_USERNAME_LENGTH = 4;
    public static final Integer MAXIMUM_USERNAME_LENGTH = 16;
    public static final String USERNAME_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZабвгдеёжзиклмнопрстуфхцчшщЪыьэюяАБВГДЕЁЖЗИКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ_0123456789";
    public static final String NOT_REGISTERED_PLAYER_USERNAME = "new_player_%s";
    public static final Integer NOT_REGISTERED_PLAYER_RANK = 10;
    public static final Boolean NOT_REGISTERED_PLAYER_IS_PLAYING = false;
    public static final Integer LEADERS_COUNT = 10;
    public static final String LEADER_RANK_INFO = "%s - %d \uD83C\uDF96\n";
    public static final String PLAYER_RANK_INFO = "You are top-%d player with %d \uD83C\uDF96";
    public static final String EMPTY_RANK_INFO = "There are no players, come back later";
    public static final String RANK_INFO_SEPARATOR = "--------------------------------------------------------------\n";
    public static final String PLAYER_PROFILE_INFO = """
            Username: %s
            Rank: %d \uD83C\uDF96
            Registered: %s""";
    public static final Double ELO_COEFFICIENT = 400d;
    public static final Integer MINIMUM_POINT_DIFFERENCE = 4;
    public static final Integer MAXIMUM_POINT_DIFFERENCE = 16;
    public static final Double WINNER_COEFFICIENT = 1d;
    public static final Double LOOSER_COEFFICIENT = 0d;
    public static final String WINNER_POINTS_MESSAGE = "You earned %d \uD83C\uDF96";
    public static final String LOOSER_POINTS_MESSAGE = "You lost %d \uD83C\uDF96";
}
