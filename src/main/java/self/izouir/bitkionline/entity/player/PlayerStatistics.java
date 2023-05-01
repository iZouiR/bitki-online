package self.izouir.bitkionline.entity.player;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "player_statistics")
@Entity
public class PlayerStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "player_id")
    private Long playerId;

    @PositiveOrZero
    @Column(name = "total_damage_dealt")
    private Long totalDamageDealt;

    @PositiveOrZero
    @Column(name = "total_damage_taken")
    private Long totalDamageTaken;

    @PositiveOrZero
    @Column(name = "total_rank_points_earned")
    private Long totalRankPointsEarned;

    @PositiveOrZero
    @Column(name = "total_rank_points_lost")
    private Long totalRankPointsLost;

    @PositiveOrZero
    @Column(name = "head_attack_chosen")
    private Long headAttackChosen;

    @PositiveOrZero
    @Column(name = "side_attack_chosen")
    private Long sideAttackChosen;

    @PositiveOrZero
    @Column(name = "ass_attack_chosen")
    private Long assAttackChosen;

    @PositiveOrZero
    @Column(name = "head_attack_succeed")
    private Long headAttackSucceed;

    @PositiveOrZero
    @Column(name = "side_attack_succeed")
    private Long sideAttackSucceed;

    @PositiveOrZero
    @Column(name = "ass_attack_succeed")
    private Long assAttackSucceed;

    @PositiveOrZero
    @Column(name = "total_battles_played")
    private Long totalBattlesPlayed;

    @PositiveOrZero
    @Column(name = "total_battles_won")
    private Long totalBattlesWon;

    @PositiveOrZero
    @Column(name = "total_eggs_obtained")
    private Long totalEggsObtained;

    @PositiveOrZero
    @Column(name = "holy_eggs_obtained")
    private Long holyEggsObtained;

    @PositiveOrZero
    @Column(name = "strong_eggs_obtained")
    private Long strongEggsObtained;

    @PositiveOrZero
    @Column(name = "weak_eggs_obtained")
    private Long weakEggsObtained;
}
