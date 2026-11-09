package at.control.shiftcontrol.shift.repo.userprofile;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import at.shiftcontrol.lib.entity.NotificationSettings;
import at.shiftcontrol.lib.type.NotificationChannel;
import at.shiftcontrol.lib.type.NotificationType;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    @Query("""
            select ns
            from NotificationSettings ns
            where ns.user.id = :userId
        """)
    Collection<NotificationSettings> findAllByUserId(String userId);

    @Query("""
            select ns.user.id
            from NotificationSettings ns
            join ns.channels c
            where ns.type = :notificationType
              and c = :notificationChannel
        """)
    Collection<String> findAllByNotificationTypeAndChannel(NotificationType notificationType, NotificationChannel notificationChannel);

    @Query("""
            select ns.user.id
            from NotificationSettings ns
            where ns.type = :notificationType
              and ns.user.id in :userIds
              and not exists (
                  select c
                  from ns.channels c
                  where c = :notificationChannel
              )
        """)
    Collection<String> findAllByNotificationTypeAndChannelDisabled(NotificationType notificationType, NotificationChannel notificationChannel, Collection<String> userIds);

    @Query("""
            select ns.user.id
            from NotificationSettings ns
            join ns.channels c
            where ns.type = :notificationType
              and c = :notificationChannel
              and ns.user.id in :userIds
        """)
    Collection<String> findAllByNotificationTypeAndChannelAndUserIds(NotificationType notificationType, NotificationChannel notificationChannel, Collection<String> userIds);
}

