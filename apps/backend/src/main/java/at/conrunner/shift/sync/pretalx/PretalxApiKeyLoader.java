package at.conrunner.shift.sync.pretalx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//import at.control.shiftcontrol.shift.entity.PretalxApiKey;
//import at.control.shiftcontrol.shift.lib.event.events.PretalxApiKeyInvalidEvent;
//import at.control.shiftcontrol.platform.exception.PretalxApiKeyInvalidException;
//import at.shiftcontrol.pretalxclient.model.EventList;
//import at.control.shiftcontrol.shift.repo.pretalx.PretalxApiKeyRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PretalxApiKeyLoader {
//    private final PretalxApiSupplier pretalxApiSupplier;
//    private final PretalxApiKeyRepository pretalxApiKeyRepository;
//    private final ApplicationEventPublisher eventPublisher;
//
//    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//    // Event slug to API key cache
//    private final Map<String, PretalxApiKey> apiKeyCache = new HashMap<>();
//    private final Set<PretalxApiKeyData> activeApiKeys = new HashSet<>();
//
//    public void refreshApiKeys() {
//        lock.writeLock().lock();
//        try {
//            refreshApiKeysInternal();
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    private void refreshApiKeysInternal() {
//        activeApiKeys.clear();
//        apiKeyCache.clear();
//
//        var apiKeys = pretalxApiKeyRepository.findAll();
//        for (var apiKey : apiKeys) {
//            var keyDataOpt = checkApiKeyInternal(apiKey);
//
//            if (keyDataOpt.isPresent()) {
//                var keyData = keyDataOpt.get();
//                activeApiKeys.add(keyData);
//                //Update cache
//                keyData.getEventSlugs().forEach(eventSlug -> apiKeyCache.put(eventSlug, apiKey));
//            }
//        }
//    }
//
//    public Optional<PretalxApiKeyData> checkApiKey(PretalxApiKey apiKey) {
//        lock.writeLock().lock();
//        try {
//            var keyDataOpt = checkApiKeyInternal(apiKey);
//            if (keyDataOpt.isPresent()) {
//                var keyData = keyDataOpt.get();
//                //Update active keys and cache
//                activeApiKeys.add(keyData);
//                keyData.getEventSlugs().forEach(eventSlug -> apiKeyCache.put(eventSlug, apiKey));
//            }
//            return keyDataOpt;
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    private Optional<PretalxApiKeyData> checkApiKeyInternal(PretalxApiKey apiKey) {
//        var eventsApi = pretalxApiSupplier.eventsApi(apiKey);
//        var roomsApi = pretalxApiSupplier.roomsApi(apiKey);
//        try {
//            var allEvents = eventsApi.apiEventsList(null, null, null);
//
//            //Verify access to rooms endpoint for each event
//            var accessibleEvents = allEvents.stream().filter(event -> {
//                try {
//                    roomsApi.roomsList(event.getSlug(), 1, null, 0, null);
//                    return true;
//                } catch (RestClientResponseException e) {
//                    if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
//                        || e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
//                        return false;
//                    } else {
//                        throw new PretalxApiKeyInvalidException("Error while verifying Pretalx API key: " + e.getMessage(), e);
//                    }
//                } catch (RestClientException e) {
//                    throw new PretalxApiKeyInvalidException("Error while verifying Pretalx API key: " + e.getMessage(), e);
//                }
//            }).toList();
//
//            if (accessibleEvents.isEmpty()) {
//                removeInvalidApiKey(apiKey);
//                return Optional.empty();
//            }
//
//            return Optional.of(new PretalxApiKeyData(
//                apiKey.getApiKey(),
//                apiKey.getPretalxHost(),
//                accessibleEvents.stream().map(EventList::getSlug).toList()));
//        } catch (RestClientResponseException e) {
//            //Remove invalid API key
//            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
//                removeInvalidApiKey(apiKey);
//                return Optional.empty();
//            } else {
//                throw new PretalxApiKeyInvalidException("Error while verifying Pretalx API key: " + e.getMessage(), e);
//            }
//        } catch (RestClientException e) {
//            throw new PretalxApiKeyInvalidException("Error while verifying Pretalx API key: " + e.getMessage(), e);
//        }
//    }
//
//    private void removeInvalidApiKey(PretalxApiKey apiKey) {
//        pretalxApiKeyRepository.delete(apiKey);
//        eventPublisher.publishEvent(new PretalxApiKeyInvalidEvent(apiKey.getApiKey()));
//        log.info("Removed invalid Pretalx API key: {}", apiKey.getApiKey());
//    }
//
//    public Set<String> accessibleEventSlugs() {
//        lock.readLock().lock();
//        try {
//            return new HashSet<>(apiKeyCache.keySet());
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public Set<PretalxApiKeyData> getActiveApiKeys() {
//        lock.readLock().lock();
//        if (activeApiKeys.isEmpty()) {
//            lock.readLock().unlock();
//            refreshApiKeys();
//            lock.readLock().lock();
//        }
//
//        try {
//            return new HashSet<>(activeApiKeys);
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public Optional<PretalxApiKey> findApiKeyForEventSlug(String eventSlug) {
//        lock.readLock().lock();
//        try {
//            return Optional.ofNullable(apiKeyCache.get(eventSlug));
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    public PretalxApiKey getApiKeyForEventSlug(String eventSlug) {
//        return findApiKeyForEventSlug(eventSlug).orElseThrow(() ->
//            PretalxMissingApiKeyException.forEventSlug(eventSlug));
//    }
}
