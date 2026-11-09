package at.control.shiftcontrol.shift.entity;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import at.control.shiftcontrol.shift.lib.type.NotificationChannel;
import at.control.shiftcontrol.shift.lib.type.NotificationType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Volunteer user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @NotNull
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "notification_setting_channels",
        joinColumns = @JoinColumn(name = "setting_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Set<NotificationChannel> channels;

    @Override
    public String toString() {
        return "NotificationSettings{"
            + "id=" + id
            + "userId=" + user.getId()
            + "type=" + type
            + ", notificationChannels={" + channels
            + "}}";
    }
}
