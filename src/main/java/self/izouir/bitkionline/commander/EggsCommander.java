package self.izouir.bitkionline.commander;

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

@Component
public class EggsCommander {
    private final EggService eggService;
    private final PlayerService playerService;
    private final PlayerBotService playerBotService;

    public EggsCommander(EggService eggService,
                         PlayerService playerService,
                         PlayerBotService playerBotService) {
        this.eggService = eggService;
        this.playerService = playerService;
        this.playerBotService = playerBotService;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "EGGS_SWITCH_FORWARD" -> {
                deleteMessage(bot, chatId, messageId);
                incrementInventoryIndex(chatId);
                sendInventory(bot, chatId);
            }
            case "EGGS_SWITCH_BACKWARD" -> {
                deleteMessage(bot, chatId, messageId);
                decrementInventoryIndex(chatId);
                sendInventory(bot, chatId);
            }
            case "EGGS_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void eggs(DispatcherBot bot, Long chatId) {
        if (playerService.existsByChatId(chatId)) {
            sendInventory(bot, chatId);
        } else {
            sendMessage(bot, chatId, "You aren't authorized - /start");
        }
    }

    private void incrementInventoryIndex(Long chatId) {
        Player player = playerService.findByChatId(chatId);
        PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
        List<Egg> inventory = eggService.findAllByOwner(player);

        Integer inventoryIndex = playerBot.getLastInventoryIndex();
        inventoryIndex++;
        if (inventoryIndex > inventory.size() - 1) {
            inventoryIndex = 0;
        }
        playerBot.setLastInventoryIndex(inventoryIndex);
        playerBotService.save(playerBot);
    }

    private void decrementInventoryIndex(Long chatId) {
        Player player = playerService.findByChatId(chatId);
        PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
        List<Egg> inventory = eggService.findAllByOwner(player);

        Integer inventoryIndex = playerBot.getLastInventoryIndex();
        inventoryIndex--;
        if (inventoryIndex < 0) {
            inventoryIndex = inventory.size() - 1;
        }
        playerBot.setLastInventoryIndex(inventoryIndex);
        playerBotService.save(playerBot);
    }

    private void sendInventory(DispatcherBot bot, Long chatId) {
        Player player = playerService.findByChatId(chatId);
        PlayerBot playerBot = playerBotService.findByPlayerId(player.getId());
        List<Egg> inventory = eggService.findAllByOwner(player);
        if (inventory.size() != playerBot.getLastInventorySize()) {
            playerBot.setLastInventoryIndex(0);
            playerBot.setLastInventorySize(inventory.size());
            playerBotService.save(playerBot);
        }
        if (!inventory.isEmpty()) {
            Integer inventoryIndex = playerBot.getLastInventoryIndex();
            SendSticker sticker = SendSticker.builder()
                    .chatId(String.valueOf(chatId))
                    .sticker(new InputFile(Path.of(inventory.get(inventoryIndex).getImagePath()).toFile()))
                    .build();
            sticker.setReplyMarkup(generateReplyMarkup(inventory.get(inventoryIndex), inventoryIndex, inventory.size()));
            sendSticker(bot, sticker);
        } else {
            sendMessage(bot, chatId, "You don't have any eggs");
        }
    }

    private ReplyKeyboard generateReplyMarkup(Egg egg, Integer inventoryIndex, Integer inventorySize) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> eggDescriptionRow = new ArrayList<>();
        InlineKeyboardButton eggDescriptionButton = new InlineKeyboardButton();
        if (egg.getIsCracked()) {
            eggDescriptionButton.setText(egg.getType().toString().toLowerCase()
                                         + " egg, cracked! (" + (inventoryIndex + 1) + "/" + inventorySize + ")");
        } else {
            eggDescriptionButton.setText(egg.getType().toString().toLowerCase()
                                         + " egg, not cracked (" + (inventoryIndex + 1) + "/" + inventorySize + ")");
        }
        eggDescriptionButton.setCallbackData("IGNORE");
        eggDescriptionRow.add(eggDescriptionButton);

        List<InlineKeyboardButton> eggStatsRow = new ArrayList<>();
        InlineKeyboardButton eggStatsButton = new InlineKeyboardButton();
        eggStatsButton.setText("En : " + egg.getEndurance() +
                               " | Lu : " + egg.getLuck() +
                               " | In : " + egg.getIntelligence());
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
        eggsCloseButton.setText("Close");
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
