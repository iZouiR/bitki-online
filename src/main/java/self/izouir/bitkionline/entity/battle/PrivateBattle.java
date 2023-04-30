package self.izouir.bitkionline.entity.battle;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "private_battles")
@Entity
public class PrivateBattle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "player_battle_id")
    private PlayerBattle playerBattle;

    @NotBlank
    @Column(name = "link")
    private String link;
}
