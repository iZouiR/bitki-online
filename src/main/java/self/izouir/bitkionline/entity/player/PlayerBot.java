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
@Table(name = "player_bots")
@Entity
public class PlayerBot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "player_id")
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_bot_state")
    private BotState lastBotState;

    @PositiveOrZero
    @Column(name = "last_inventory_index")
    private Integer lastInventoryIndex;

    @PositiveOrZero
    @Column(name = "last_inventory_size")
    private Integer lastInventorySize;
}
