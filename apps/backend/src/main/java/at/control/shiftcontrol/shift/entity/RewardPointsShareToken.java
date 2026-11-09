package at.control.shiftcontrol.shift.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reward_points_share_token")
public class RewardPointsShareToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @NotNull
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
