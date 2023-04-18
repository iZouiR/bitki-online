package self.izouir.bitkionline.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Set;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "players")
@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "username")
    private String username;

    @Column(name = "rank")
    private Long rank;

    @Column(name = "registered_at")
    private Timestamp registeredAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Set<Egg> ownedEggs;
}
