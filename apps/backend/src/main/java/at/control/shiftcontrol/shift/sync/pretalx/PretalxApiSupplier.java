package at.control.shiftcontrol.shift.sync.pretalx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import at.shiftcontrol.lib.entity.PretalxApiKey;
import at.shiftcontrol.pretalxclient.api.EventsApi;
import at.shiftcontrol.pretalxclient.api.RoomsApi;
import at.shiftcontrol.pretalxclient.api.SlotsApi;
import at.shiftcontrol.pretalxclient.api.SubmissionsApi;
import at.shiftcontrol.pretalxclient.invoker.ApiClient;

@RequiredArgsConstructor
@Service
public class PretalxApiSupplier {
    private final RestClient.Builder restClientBuilder;

    @Value("${spring.application.version:unknown}")
    private String appVersion;

    public EventsApi eventsApi(PretalxApiKey apiKey) {
        return new EventsApi(buildApiClient(apiKey));
    }

    public RoomsApi roomsApi(PretalxApiKey apiKey) {
        return new RoomsApi(buildApiClient(apiKey));
    }

    public SubmissionsApi submissionsApi(PretalxApiKey apiKey) {
        return new SubmissionsApi(buildApiClient(apiKey));
    }

    public SlotsApi slotsApi(PretalxApiKey apiKey) {
        return new SlotsApi(buildApiClient(apiKey));
    }

    private @NonNull ApiClient buildApiClient(PretalxApiKey apiKey) {
        var restClient = buildRestClient(apiKey.getApiKey());
        var apiClient = new ApiClient(restClient);
        apiClient.setUserAgent("ShiftControl-ShiftService/" + appVersion);
        apiClient.setBasePath(apiKey.getPretalxHost());

        return apiClient;
    }

    private @NonNull RestClient buildRestClient(String apiToken) {
        return restClientBuilder
            .defaultHeader("Authorization", "Token " + apiToken)
            .build();
    }
}
