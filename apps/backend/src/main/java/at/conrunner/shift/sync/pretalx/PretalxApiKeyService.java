package at.conrunner.shift.sync.pretalx;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PretalxApiKeyService {
//    private final PretalxApiKeyRepository pretalxApiKeyRepository;
//    private final PretalxApiKeyLoader pretalxApiKeyLoader;
//
//    public List<PretalxApiKeyDetailsDto> getAllKeys() {
//        pretalxApiKeyLoader.refreshApiKeys();
//        var apiKeys = pretalxApiKeyLoader.getActiveApiKeys();
//
//        return apiKeys.stream().map(PretalxApiKeyDetailsDto::of).toList();
//    }
//
//    @Transactional
//    public PretalxApiKeyDetailsDto addKey(PretalxApiKeyDto apiKeyDto) {
//        if (pretalxApiKeyRepository.existsById(apiKeyDto.getApiKey())) {
//            throw new ConflictException("API key already exists");
//        }
//
//        // check that host is valid URL
//        try {
//            new URL(apiKeyDto.getPretalxHost());
//        } catch (MalformedURLException e) {
//            throw new ValidationException("Pretalx host is not a valid URL");
//        }
//
//        var apiKey = PretalxApiKey.builder()
//            .apiKey(apiKeyDto.getApiKey())
//            .pretalxHost(apiKeyDto.getPretalxHost())
//            .build();
//
//        var keyDataOpt = pretalxApiKeyLoader.checkApiKey(apiKey);
//        if (keyDataOpt.isEmpty()) {
//            throw new ValidationException("API key is not valid or has no accessible events");
//        }
//
//        pretalxApiKeyRepository.save(apiKey);
//
//        return PretalxApiKeyDetailsDto.of(keyDataOpt.get());
//    }
//
//    @Transactional
//    public void removeKey(String apiKey) {
//        if (!pretalxApiKeyRepository.existsById(apiKey)) {
//            throw new NotFoundException("API key does not exist");
//        }
//
//        pretalxApiKeyRepository.deleteById(apiKey);
//        pretalxApiKeyLoader.refreshApiKeys();
//    }
}
