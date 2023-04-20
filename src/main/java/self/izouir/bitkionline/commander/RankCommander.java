package self.izouir.bitkionline.commander;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.service.player.PlayerService;

import java.util.ArrayList;
import java.util.List;

import static self.izouir.bitkionline.util.BotMessageSender.*;

@Component
public class RankCommander {
    private final PlayerService playerService;

    @Autowired
    public RankCommander(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void processCallbackQuery(DispatcherBot bot, Long chatId, Integer messageId, String callbackData) {
        switch (callbackData) {
            case "RANK_REFRESH" -> {
                EditMessageText message = EditMessageText.builder()
                        .chatId(String.valueOf(chatId))
                        .messageId(messageId)
                        .text(generateRankInfo(chatId))
                        .build();
                message.setReplyMarkup(generateReplyMarkup());
                sendEditMessageText(bot, message);
            }
            case "RANK_CLOSE" -> deleteMessage(bot, chatId, messageId);
        }
    }

    public void rank(DispatcherBot bot, Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(generateRankInfo(chatId))
                .build();
        message.setReplyMarkup(generateReplyMarkup());
        sendMessage(bot, message);
    }

    private String generateRankInfo(Long chatId) {
        StringBuilder rankInfo = new StringBuilder();
        rankInfo.append(generateTopPlayersRankInfo());
        if (playerService.existsByChatId(chatId)) {
            Player player = playerService.findByChatId(chatId);
            rankInfo.append("--------------------------------------------------------------\n");
            rankInfo.append(generatePlayerRankInfo(player));
        }
        return rankInfo.toString();
    }

    private String generateTopPlayersRankInfo() {
        StringBuilder topPlayersRankInfo = new StringBuilder();
        List<Player> topPlayers = playerService.findAllOrderedByRankDesc(3L);
        if (!topPlayers.isEmpty()) {
            for (int i = 0; i < topPlayers.size(); i++) {
                if (i == 0) {
                    topPlayersRankInfo.append("\uD83E\uDD47");
                }
                if (i == 1) {
                    topPlayersRankInfo.append("\uD83E\uDD48");
                }
                if (i == 2) {
                    topPlayersRankInfo.append("\uD83E\uDD49");
                }
                topPlayersRankInfo.append(topPlayers.get(i).getUsername());
                topPlayersRankInfo.append(" - ");
                topPlayersRankInfo.append(topPlayers.get(i).getRank());
                topPlayersRankInfo.append("\n");
            }
        } else {
            topPlayersRankInfo.append("Looks like there are no players at all, come back later");
        }
        return topPlayersRankInfo.toString();
    }

    private String generatePlayerRankInfo(Player player) {
        StringBuilder playerRankInfo = new StringBuilder();
        List<Player> allPlayers = playerService.findAllOrderedByRankDesc();
        Long place = allPlayers.indexOf(player) + 1L;
        playerRankInfo.append("You are top-");
        playerRankInfo.append(place);
        if (place == 1) {
            playerRankInfo.append(" (\uD83E\uDD47)");
        }
        if (place == 2) {
            playerRankInfo.append(" (\uD83E\uDD48)");
        }
        if (place == 3) {
            playerRankInfo.append(" (\uD83E\uDD49)");
        }
        playerRankInfo.append(" player with ");
        playerRankInfo.append(player.getRank());
        playerRankInfo.append(" points");
        return playerRankInfo.toString();
    }

    private InlineKeyboardMarkup generateReplyMarkup() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> refreshRow = new ArrayList<>();
        InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText("Refresh");
        refreshButton.setCallbackData("RANK_REFRESH");
        refreshRow.add(refreshButton);

        List<InlineKeyboardButton> closeRow = new ArrayList<>();
        InlineKeyboardButton closeButton = new InlineKeyboardButton();
        closeButton.setText("Close");
        closeButton.setCallbackData("RANK_CLOSE");
        closeRow.add(closeButton);

        keyboard.add(refreshRow);
        keyboard.add(closeRow);
        markup.setKeyboard(keyboard);
        return markup;
    }
}
