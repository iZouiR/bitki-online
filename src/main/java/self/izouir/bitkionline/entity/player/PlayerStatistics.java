package self.izouir.bitkionline.entity.player;

import jakarta.persistence.*;
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

    @Column(name = "total_damage_dealt")
    private Long totalDamageDealt;

    @Column(name = "total_damage_taken")
    private Long totalDamageTaken;

    @Column(name = "total_rank_points_earned")
    private Long totalRankPointsEarned;

    @Column(name = "total_rank_points_lost")
    private Long totalRankPointsLost;

    @Column(name = "head_attack_chosen")
    private Long headAttackChosen;

    @Column(name = "side_attack_chosen")
    private Long sideAttackChosen;

    @Column(name = "ass_attack_chosen")
    private Long assAttackChosen;

    @Column(name = "head_attack_succeed")
    private Long headAttackSucceed;

    @Column(name = "side_attack_succeed")
    private Long sideAttackSucceed;

    @Column(name = "ass_attack_succeed")
    private Long assAttackSucceed;

    @Column(name = "total_battles_played")
    private Long totalBattlesPlayed;

    @Column(name = "total_battles_won")
    private Long totalBattlesWon;

    @Column(name = "total_eggs_obtained")
    private Long totalEggsObtained;

    @Column(name = "holy_eggs_obtained")
    private Long holyEggsObtained;

    @Column(name = "strong_eggs_obtained")
    private Long strongEggsObtained;

    @Column(name = "weak_eggs_obtained")
    private Long weakEggsObtained;
}
