package at.control.shiftcontrol.shift.dao;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import at.shiftcontrol.lib.entity.NotificationSettings;
import at.shiftcontrol.lib.type.NotificationChannel;
import at.shiftcontrol.lib.type.NotificationType;
import at.shiftcontrol.shiftservice.repo.userprofile.NotificationSettingsRepository;

@RequiredArgsConstructor
@Component
public class NotificationSettingsDaoImpl implements NotificationSettingsDao {
    private final NotificationSettingsRepository repository;

    @Override
    public @NonNull String getName() {
        return "NotificationSettings";
    }

    @Override
    public @NonNull Optional<NotificationSettings> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public NotificationSettings save(NotificationSettings entity) {
        return repository.save(entity);
    }

    @Override
    public Collection<NotificationSettings> saveAll(Collection<NotificationSettings> entities) {
        return repository.saveAll(entities);
    }

    @Override
    public void delete(NotificationSettings entity) {
        repository.delete(entity);
    }

    @Override
    public Collection<NotificationSettings> findAllByUserId(String userId) {
        return repository.findAllByUserId(userId);
    }

    @Override
    public Collection<String> findAllByNotificationTypeAndChannel(
        NotificationType notificationType, NotificationChannel notificationChannel) {
        return repository.findAllByNotificationTypeAndChannel(notificationType, notificationChannel);
    }

    @Override
    public Collection<String> findAllByNotificationTypeAndChannelDisabled(
        NotificationType notificationType, NotificationChannel notificationChannel, Collection<String> userIds) {
        return repository.findAllByNotificationTypeAndChannelDisabled(notificationType, notificationChannel, userIds);
    }

    @Override
    public Collection<String> findAllByNotificationTypeAndChannelAndUserIds(
        NotificationType notificationType, NotificationChannel notificationChannel, Collection<String> userIds) {
        return repository.findAllByNotificationTypeAndChannelAndUserIds(notificationType, notificationChannel, userIds);
    }
}
