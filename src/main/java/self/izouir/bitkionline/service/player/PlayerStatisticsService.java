package self.izouir.bitkionline.service.player;

import org.springframework.stereotype.Service;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.egg.EggAttackType;
import self.izouir.bitkionline.entity.player.Player;
import self.izouir.bitkionline.entity.player.PlayerStatistics;
import self.izouir.bitkionline.exception.PlayerStatisticsNotFoundException;
import self.izouir.bitkionline.repository.player.PlayerStatisticsRepository;

import java.util.Optional;

@Service
public class PlayerStatisticsService {
    private final PlayerStatisticsRepository playerStatisticsRepository;

    public PlayerStatisticsService(PlayerStatisticsRepository playerStatisticsRepository) {
        this.playerStatisticsRepository = playerStatisticsRepository;
    }

    public PlayerStatistics findByPlayerId(Long playerId) {
        Optional<PlayerStatistics> optional = playerStatisticsRepository.findByPlayerId(playerId);
        return optional.orElseThrow(() -> new PlayerStatisticsNotFoundException("Player statistics with playerId = " + playerId + " was not found"));
    }

    public void save(PlayerStatistics playerStatistics) {
        playerStatisticsRepository.save(playerStatistics);
    }

    public void createPlayerStatistics(Player player) {
        PlayerStatistics playerStatistics = PlayerStatistics.builder()
                .playerId(player.getId())
                .totalDamageDealt(0L)
                .totalDamageTaken(0L)
                .totalRankPointsEarned(0L)
                .totalRankPointsLost(0L)
                .headAttackChosen(0L)
                .sideAttackChosen(0L)
                .assAttackChosen(0L)
                .headAttackSucceed(0L)
                .sideAttackSucceed(0L)
                .assAttackSucceed(0L)
                .totalBattlesPlayed(0L)
                .totalBattlesWon(0L)
                .totalEggsObtained(0L)
                .holyEggsObtained(0L)
                .strongEggsObtained(0L)
                .weakEggsObtained(0L)
                .build();
        save(playerStatistics);
    }

    public void applyDealtDamage(Player player, Integer damage) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        playerStatistics.setTotalDamageDealt(playerStatistics.getTotalDamageDealt() + damage);
        save(playerStatistics);
    }

    public void applyTakenDamage(Player player, Integer damage) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        playerStatistics.setTotalDamageTaken(playerStatistics.getTotalDamageTaken() + damage);
        save(playerStatistics);
    }

    public void applyRankDifference(Player player, int rankDifference) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        if (rankDifference > 0) {
            playerStatistics.setTotalRankPointsEarned(playerStatistics.getTotalRankPointsEarned() + rankDifference);
        }
        if (rankDifference < 0) {
            playerStatistics.setTotalRankPointsLost(playerStatistics.getTotalRankPointsLost() + Math.abs(rankDifference));
        }
        save(playerStatistics);
    }

    public void incrementTotalBattlesPlayed(Player player) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        playerStatistics.setTotalBattlesPlayed(playerStatistics.getTotalBattlesPlayed() + 1);
        save(playerStatistics);
    }

    public void incrementTotalBattlesWon(Player player) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        playerStatistics.setTotalBattlesWon(playerStatistics.getTotalBattlesWon() + 1);
        save(playerStatistics);
    }

    public void applyAttackChoice(Player player, EggAttackType attackType) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        switch (attackType) {
            case HEAD -> playerStatistics.setHeadAttackChosen(playerStatistics.getHeadAttackChosen() + 1);
            case SIDE -> playerStatistics.setSideAttackChosen(playerStatistics.getSideAttackChosen() + 1);
            case ASS -> playerStatistics.setAssAttackChosen(playerStatistics.getAssAttackChosen() + 1);
        }
        save(playerStatistics);
    }

    public void applyAttackSuccess(Player player, EggAttackType attackType) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        switch (attackType) {
            case HEAD -> playerStatistics.setHeadAttackSucceed(playerStatistics.getHeadAttackSucceed() + 1);
            case SIDE -> playerStatistics.setSideAttackSucceed(playerStatistics.getSideAttackSucceed() + 1);
            case ASS -> playerStatistics.setAssAttackSucceed(playerStatistics.getAssAttackSucceed() + 1);
        }
        save(playerStatistics);
    }

    public void incrementEggsObtained(Player player, Egg egg) {
        PlayerStatistics playerStatistics = findByPlayerId(player.getId());
        playerStatistics.setTotalEggsObtained(playerStatistics.getTotalEggsObtained() + 1);
        switch (egg.getType()) {
            case HOLY -> playerStatistics.setHolyEggsObtained(playerStatistics.getHolyEggsObtained() + 1);
            case STRONG -> playerStatistics.setStrongEggsObtained(playerStatistics.getStrongEggsObtained() + 1);
            case WEAK -> playerStatistics.setWeakEggsObtained(playerStatistics.getWeakEggsObtained() + 1);
        }
        save(playerStatistics);
    }
}
