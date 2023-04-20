package self.izouir.bitkionline.entity.egg;

import jakarta.persistence.*;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private EggType type;

    @Column(name = "is_cracked")
    private Boolean isCracked;

    @Column(name = "endurance")
    private Integer endurance;

    @Column(name = "luck")
    private Integer luck;

    @Column(name = "intelligence")
    private Integer intelligence;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private Player owner;
}
