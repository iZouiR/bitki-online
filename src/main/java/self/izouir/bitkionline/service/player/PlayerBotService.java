package self.izouir.bitkionline.service.player;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.repository.player.PlayerBotRepository;

@Service
public class PlayerBotService {
    private final PlayerBotRepository playerBotRepository;

    @Autowired
    public PlayerBotService(PlayerBotRepository playerBotRepository) {
        this.playerBotRepository = playerBotRepository;
    }
}
