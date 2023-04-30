package self.izouir.bitkionline.entity.egg;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import self.izouir.bitkionline.entity.player.Player;

import java.sql.Timestamp;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "eggs")
@Entity
public class Egg {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private EggType type;

    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "is_cracked")
    private Boolean isCracked;

    @NotNull
    @PositiveOrZero
    @Column(name = "endurance")
    private Integer endurance;

    @NotNull
    @PositiveOrZero
    @Column(name = "power")
    private Integer power;

    @NotNull
    @PositiveOrZero
    @Column(name = "luck")
    private Integer luck;

    @NotNull
    @PositiveOrZero
    @Column(name = "intelligence")
    private Integer intelligence;

    @NotNull
    @Column(name = "image_path")
    private String imagePath;

    @NotNull
    @PastOrPresent
    @Column(name = "created_at")
    private Timestamp createdAt;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Player owner;
}
