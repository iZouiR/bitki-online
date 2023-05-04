package self.izouir.bitkionline.util.constant.commander;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SupportCommanderConstant {
    public static final Integer MINIMUM_SUPPORT_MESSAGE_LENGTH = 8;
    public static final Integer MAXIMUM_SUPPORT_MESSAGE_LENGTH = 256;
    public static final String SUPPORT_MESSAGE_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZабвгдеёжзиклмнопрстуфхцчшщЪыьэюяАБВГДЕЁЖЗИКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ ,.0123456789";
    public static final String AWAIT_SUPPORT_MESSAGE_MESSAGE = "Enter a message for developers, it must contains english/russian letters + digits + spaces + ',' + '.' and be between "
                                                               + MINIMUM_SUPPORT_MESSAGE_LENGTH + " and " + MAXIMUM_SUPPORT_MESSAGE_LENGTH + " characters in length";
    public static final String INCORRECT_SUPPORT_MESSAGE_FORMAT_MESSAGE = "Entered message has incorrect format, it must contains english/russian letters + digits + spaces + ',' + '.' and be between "
                                                                          + MINIMUM_SUPPORT_MESSAGE_LENGTH + " and " + MAXIMUM_SUPPORT_MESSAGE_LENGTH + " characters in length";
    public static final String SUPPORT_MESSAGE_PUBLICATION_SUCCESS_MESSAGE = "Success, your message sent to developers \uD83D\uDC68\uD83C\uDFFF\u200D\uD83D\uDCBB";
}
