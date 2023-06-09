package self.izouir.bitkionline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.SupportMessage;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.repository.SupportMessageRepository;
import self.izouir.bitkionline.service.player.PlayerBotService;

import java.sql.Timestamp;
import java.time.Instant;

import static self.izouir.bitkionline.util.constant.commander.SupportCommanderConstant.*;

@RequiredArgsConstructor
@Service
public class SupportMessageService {
    private final PlayerBotService playerBotService;
    private final SupportMessageRepository supportMessageRepository;

    public void save(final SupportMessage supportMessage) {
        supportMessageRepository.save(supportMessage);
    }

    public boolean isAccurateSupportMessage(final String message) {
        if (message.length() >= MINIMUM_SUPPORT_MESSAGE_LENGTH && message.length() <= MAXIMUM_SUPPORT_MESSAGE_LENGTH) {
            for (final char c : message.toCharArray()) {
                if (SUPPORT_MESSAGE_ALPHABET.indexOf(c) == -1) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void publishSupportMessage(final Player player, final String message) {
        final SupportMessage supportMessage = SupportMessage.builder()
                .chatId(player.getChatId())
                .message(message)
                .sentAt(Timestamp.from(Instant.now()))
                .build();
        save(supportMessage);
        playerBotService.applyLastState(player, PlayerBotState.NO_STATE);
    }
}
