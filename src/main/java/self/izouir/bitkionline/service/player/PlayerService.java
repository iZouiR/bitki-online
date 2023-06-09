package self.izouir.bitkionline.service.player;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import self.izouir.bitkionline.bot.DispatcherBot;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerBotState;
import self.izouir.bitkionline.exception.PlayerNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerRepository;
import self.izouir.bitkionline.service.battle.MatchMakingBattleService;
import self.izouir.bitkionline.service.battle.PlayerBattleService;
import self.izouir.bitkionline.service.battle.PrivateBattleService;
import self.izouir.bitkionline.service.egg.EggService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static self.izouir.bitkionline.util.BotMessageSender.sendMessage;
import static self.izouir.bitkionline.util.constant.service.PlayerServiceConstant.*;

@RequiredArgsConstructor
@Service
public class PlayerService {
    private final MatchMakingBattleService matchMakingBattleService;
    private final PlayerBattleService playerBattleService;
    private final PrivateBattleService privateBattleService;
    private final EggService eggService;
    private final PlayerBotService playerBotService;
    private final PlayerStatisticsService playerStatisticsService;
    private final PlayerRepository playerRepository;

    public Player findByChatId(final Long chatId) {
        final Optional<Player> optional = playerRepository.findByChatId(chatId);
        return optional.orElseThrow(() -> new PlayerNotFoundException("Player with chatId = " + chatId + " was not found"));
    }

    public List<Player> findAllOrderedByRankDesc() {
        return playerRepository.findAllOrderedByRankDesc();
    }

    public List<Player> findAllOrderedByRankDesc(final Integer limitCount) {
        return playerRepository.findAllOrderedByRankDesc(limitCount);
    }

    public boolean existsByChatId(final Long chatId) {
        return playerRepository.findByChatId(chatId).isPresent();
    }

    public boolean notExistsByUsernameIgnoreCase(final String username) {
        return playerRepository.findByUsernameIgnoreCase(username).isEmpty();
    }

    public void save(final Player player) {
        playerRepository.save(player);
    }

    @Transactional
    public void delete(final Player player) {
        playerRepository.delete(player);
    }

    public void applyIsPlaying(final Long chatId, final boolean isPlaying) {
        final Player player = findByChatId(chatId);
        player.setIsPlaying(isPlaying);
        save(player);
    }

    @Transactional
    public void createNotRegisteredPlayer(final Long chatId) {
        final Player player = Player.builder()
                .chatId(chatId)
                .username(String.format(NOT_REGISTERED_PLAYER_USERNAME, chatId))
                .rank(NOT_REGISTERED_PLAYER_RANK)
                .isPlaying(NOT_REGISTERED_PLAYER_IS_PLAYING)
                .build();
        save(player);
        playerBotService.createNotRegisteredPlayerBot(player);
        playerStatisticsService.createPlayerStatistics(player);
    }

    @Transactional
    public void registerPlayer(final DispatcherBot bot, final Player player, final String username) {
        player.setUsername(username);
        player.setRegisteredAt(Timestamp.from(Instant.now()));
        save(player);
        eggService.generateStartInventory(bot, player);
        playerBotService.registerPlayerBot(player);
    }

    public boolean isAccurateUsername(final String username) {
        if (username.length() >= MINIMUM_USERNAME_LENGTH && username.length() <= MAXIMUM_USERNAME_LENGTH) {
            for (final char c : username.toCharArray()) {
                if (USERNAME_ALPHABET.indexOf(c) == -1) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public String generateRankInfo(final Long chatId) {
        final StringBuilder rankInfo = new StringBuilder();
        rankInfo.append(generateLeadersRankInfo());
        if (existsByChatId(chatId)) {
            rankInfo.append(RANK_INFO_SEPARATOR);
            rankInfo.append(generatePlayerRankInfo(findByChatId(chatId)));
        }
        return rankInfo.toString();
    }

    private String generateLeadersRankInfo() {
        final StringBuilder leadersRankInfo = new StringBuilder();
        final List<Player> leaders = findAllOrderedByRankDesc(LEADERS_COUNT);
        if (!leaders.isEmpty()) {
            for (int i = 0; i < leaders.size(); i++) {
                if (i == 0) {
                    leadersRankInfo.append("\uD83E\uDD47");
                }
                if (i == 1) {
                    leadersRankInfo.append("\uD83E\uDD48");
                }
                if (i == 2) {
                    leadersRankInfo.append("\uD83E\uDD49");
                }
                final Player leader = leaders.get(i);
                leadersRankInfo.append(String.format(LEADER_RANK_INFO, leader.getUsername(), leader.getRank(),
                        playerStatisticsService.calculateWinRate(playerStatisticsService.findByPlayerId(leader.getId()))));
            }
        } else {
            leadersRankInfo.append(EMPTY_RANK_INFO);
        }
        return leadersRankInfo.toString();
    }

    private String generatePlayerRankInfo(final Player player) {
        final List<Player> allPlayers = findAllOrderedByRankDesc();
        final Long place = allPlayers.indexOf(player) + 1L;
        return String.format(PLAYER_RANK_INFO, place, player.getRank(),
                playerStatisticsService.calculateWinRate(playerStatisticsService.findByPlayerId(player.getId())));
    }

    public String generatePlayerProfileInfo(final Player player) {
        return String.format(PLAYER_PROFILE_INFO, player.getUsername(), player.getRank(),
                playerStatisticsService.calculateWinRate(playerStatisticsService.findByPlayerId(player.getId())),
                player.getRegisteredAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy [HH:mm]")));
    }

    @Transactional
    public void changeUsername(final Player player, final String username) {
        player.setUsername(username);
        save(player);
        playerBotService.applyLastState(player, PlayerBotState.NO_STATE);
    }

    @Transactional
    public void refreshEggs(final DispatcherBot bot, final Long chatId) {
        final Player player = findByChatId(chatId);
        eggService.unbindAllByOwner(player);
        eggService.generateStartInventory(bot, player);
    }

    @Transactional
    public void dropPlayerProfile(final Long chatId) {
        final Player player = findByChatId(chatId);
        matchMakingBattleService.deleteAllByPlayer(player);
        privateBattleService.deleteAllByPlayer(player);
        playerBattleService.deleteAllByPlayer(player);
        eggService.deleteAllByOwner(player);
        playerBotService.deleteByPlayerId(player.getId());
        delete(player);
    }

    @Transactional
    public void calculateEloRanks(final DispatcherBot bot, final Player winner, final Player looser) {
        int winnerPlayerRank = winner.getRank();
        int looserPlayerRank = looser.getRank();
        final double awaitFirstPlayerRankDifference = 1 / (1 + Math.pow(10, (looserPlayerRank - winnerPlayerRank) / ELO_COEFFICIENT));
        final double awaitSecondPlayerRankDifference = 1 / (1 + Math.pow(10, (winnerPlayerRank - looserPlayerRank) / ELO_COEFFICIENT));
        final int firstPlayerRankDifference = Math.toIntExact(Math.round((MAXIMUM_POINT_DIFFERENCE) * (WINNER_COEFFICIENT - awaitFirstPlayerRankDifference)));
        final int secondPlayerRankDifference = Math.toIntExact(Math.round((MAXIMUM_POINT_DIFFERENCE) * (LOOSER_COEFFICIENT - awaitSecondPlayerRankDifference)));
        winnerPlayerRank = winnerPlayerRank + firstPlayerRankDifference;
        looserPlayerRank = looserPlayerRank + secondPlayerRankDifference;
        if (looserPlayerRank < 0) {
            looserPlayerRank = 0;
        }
        winner.setRank(winnerPlayerRank);
        looser.setRank(looserPlayerRank);
        save(winner);
        save(looser);
        playerStatisticsService.applyRankDifference(winner, firstPlayerRankDifference);
        playerStatisticsService.applyRankDifference(looser, secondPlayerRankDifference);
        sendMessage(bot, winner.getChatId(), String.format(WINNER_POINTS_MESSAGE, firstPlayerRankDifference));
        sendMessage(bot, looser.getChatId(), String.format(LOOSER_POINTS_MESSAGE, secondPlayerRankDifference));
    }
}
