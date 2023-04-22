package self.izouir.bitkionline.entity.battle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import self.izouir.bitkionline.entity.egg.Egg;
import self.izouir.bitkionline.entity.player.Player;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "player_battles")
@Entity
public class PlayerBattle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "first_player_id")
    private Player firstPlayer;

    @ManyToOne
    @JoinColumn(name = "second_player_id")
    private Player secondPlayer;

    @ManyToOne
    @JoinColumn(name = "first_player_egg_id")
    private Egg firstPlayerEgg;

    @ManyToOne
    @JoinColumn(name = "second_player_egg_id")
    private Egg secondPlayerEgg;

    @Column(name = "is_first_player_winner")
    private Boolean isFirstPlayerWinner;
}
