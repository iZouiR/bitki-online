package self.izouir.bitkionline.commander;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.deleteMessage;
import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;

@Component
public class HelpCommander {
    private static final String HELP_MESSAGE = """
            At beta you enter the game with 3 random eggs
                            
            All the eggs have their characteristics:
                - Endurance (En) - Describes the amount of damage the egg could take until breaking
                - Luck (Lu) - Describes the amount of chance to damage enemy egg
                - Intelligence (In) - Describes the reduction of damage taken while attacking enemy egg
                      
            Types of battles:
                Match Making Battle - ranked battle with random opponent based on player's rank
                Private Battle - non-ranked battle with player's friend
                
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

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        if (callbackData.equals("HELP_CLOSE")) {
            deleteMessage(bot, chatId, messageId);
        }
    }

    public void help(DispatcherBot bot, Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(HELP_MESSAGE)
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendMessage(bot, message);
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText("Close");
        closeButton.setCallbackData("HELP_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
