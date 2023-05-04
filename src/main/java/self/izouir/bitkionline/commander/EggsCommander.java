package self.izouir.bitkionline.commander;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBot;
import self.izouir.bitkionline.service.egg.EggService;
import self.izouir.bitkionline.service.player.PlayerBotService;
import self.izouir.bitkionline.service.player.PlayerService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.*;
import static self.izouir.bitkionline.util.constant.MessageConstant.*;
import static self.izouir.bitkionline.util.constant.ReplyMarkupConstant.CLOSE_BUTTON_TEXT;

@RequiredArgsConstructor
@Component
public class EggsCommander {
    private final EggService eggService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "EGGS_SWITCH_FORWARD" -> {
                deleteMessage(bot, chatId, messageId);
                playerBotService.incrementLastInventoryIndex(playerService.findByChatId(chatId));
                sendInventory(bot, chatId);
            }
            case "EGGS_SWITCH_BACKWARD" -> {
                deleteMessage(bot, chatId, messageId);
                playerBotService.decrementLastInventoryIndex(playerService.findByChatId(chatId));
                sendInventory(bot, chatId);
            }
            case "EGGS_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void eggs(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            if (player.getRegisteredAt() != null) {
                sendInventory(bot, chatId);
            } else {
                sendMessage(bot, chatId, PLAYER_DID_NOT_FINISH_REGISTRATION_MESSAGE);
            }
        } else {
            sendMessage(bot, chatId, PLAYER_NOT_REGISTERED_MESSAGE);
        }
    }

    private void sendInventory(DispatcherBot bot, Long chatId) {
        Player player = playerService.findByChatId(chatId);
        PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
        List<Egg> playerInventory = eggService.findAllByOwner(player);
        if (playerInventory.size() != playerBot.getLastInventorySize()) {
            playerBot.setLastInventoryIndex(0);
            playerBot.setLastInventorySize(playerInventory.size());
            playerBotService.save(playerBot);
        }
        if (!playerInventory.isEmpty()) {
            Integer inventoryIndex = playerBot.getLastInventoryIndex();
            SendSticker sticker = SendSticker.builder()
                    .chatId(String.valueOf(chatId))
                    .sticker(new InputFile(Path.of(playerInventory.get(inventoryIndex).getImagePath()).toFile()))
                    .build();
            sticker.setReplyMarkup(generateReplyMarkup(playerInventory.get(inventoryIndex)));
            sendSticker(bot, sticker);
        } else {
            sendMessage(bot, chatId, EMPTY_INVENTORY_MESSAGE);
        }
    }

    private ReplyKeyboard generateReplyMarkup(Egg egg) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> eggDescriptionRow = new ArrayList<>();
        InlineKeyboardButton eggDescriptionButton = new InlineKeyboardButton();
        if (egg.getIsCracked()) {
            eggDescriptionButton.setText(String.format(CRACKED_EGG_MESSAGE, egg.getName()));
        } else {
            eggDescriptionButton.setText(String.format(NOT_CRACKED_EGG_MESSAGE, egg.getName()));
        }
        eggDescriptionButton.setCallbackData("IGNORE");
        eggDescriptionRow.add(eggDescriptionButton);

        List<InlineKeyboardButton> eggStatsRow = new ArrayList<>();
        InlineKeyboardButton eggStatsButton = new InlineKeyboardButton();
        eggStatsButton.setText(eggService.generateStatsInfo(egg));
        eggStatsButton.setCallbackData("IGNORE");
        eggStatsRow.add(eggStatsButton);

        List<InlineKeyboardButton> switchRow = new ArrayList<>();
        InlineKeyboardButton switchBackwardButton = new InlineKeyboardButton();
        switchBackwardButton.setText("◀️");
        switchBackwardButton.setCallbackData("EGGS_SWITCH_BACKWARD");
        switchRow.add(switchBackwardButton);
        InlineKeyboardButton switchForwardButton = new InlineKeyboardButton();
        switchForwardButton.setText("▶️");
        switchForwardButton.setCallbackData("EGGS_SWITCH_FORWARD");
        switchRow.add(switchForwardButton);

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton eggsCloseButton = new InlineKeyboardButton();
        eggsCloseButton.setText(CLOSE_BUTTON_TEXT);
        eggsCloseButton.setCallbackData("EGGS_CLOSE");
        closeRow.add(eggsCloseButton);

        keyboard.add(eggDescriptionRow);
        keyboard.add(eggStatsRow);
        keyboard.add(switchRow);
        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
