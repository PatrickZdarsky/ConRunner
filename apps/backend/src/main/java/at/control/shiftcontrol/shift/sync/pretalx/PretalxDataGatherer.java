package at.control.shiftcontrol.shift.sync.pretalx;

import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

//import at.shiftcontrol.pretalxclient.model.Event;
//import at.shiftcontrol.pretalxclient.model.Room;
//import at.shiftcontrol.pretalxclient.model.Submission;
//import at.shiftcontrol.pretalxclient.model.TalkSlot;

@RequiredArgsConstructor
@Component
public class PretalxDataGatherer {
//    private static final int ROOMS_PAGINATION_LIMIT = 1024;
//    private static final ObjectMapper mapper = new ObjectMapper();
//    private static final TypeReference<HashMap<String, Object>> mapTypeRef
//        = new TypeReference<>() {};
//
//    private final PretalxApiKeyLoader apiKeyLoader;
//    private final PretalxApiSupplier apiSupplier;
//
//    public List<Room> gatherRooms(String eventSlug) {
//        var apiKey = apiKeyLoader.getApiKeyForEventSlug(eventSlug);
//        var roomsApi = apiSupplier.roomsApi(apiKey);
//        return roomsApi.roomsList(eventSlug, ROOMS_PAGINATION_LIMIT, null, 0, null).getResults();
//    }
//
//    public List<Submission> gatherSubmissions(String eventSlug) {
//        var apiKey = apiKeyLoader.getApiKeyForEventSlug(eventSlug);
//        var submissionsApi = apiSupplier.submissionsApi(apiKey);
//        return submissionsApi.submissionsList(eventSlug, null, null, null, null, null, null, null, null, null, null, null).getResults();
//    }
//
//    public String getInternalNoteForSubmission(String eventSlug, String submissionCode) {
//        var apiKey = apiKeyLoader.getApiKeyForEventSlug(eventSlug);
//        var submissionsApi = apiSupplier.submissionsApi(apiKey);
//        var submission = submissionsApi.submissionsRetrieveWithResponseSpec(submissionCode, eventSlug, null);
//        var bodyString = submission.body(String.class);
//        try {
//            var bodyMap = mapper.readValue(bodyString, mapTypeRef);
//            if (bodyMap.containsKey("internal_notes")) {
//                return (String) bodyMap.get("internal_notes");
//            } else {
//                return null;
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to parse submission internal notes", e);
//        }
//    }
//
//    public List<TalkSlot> gatherSlots(String eventSlug) {
//        var apiKey = apiKeyLoader.getApiKeyForEventSlug(eventSlug);
//        var slotsApi = apiSupplier.slotsApi(apiKey);
//        return slotsApi.slotsList(eventSlug, null, null, null, null, null, null, null, null, null, null, null).getResults();
//    }
//
//    public Event gatherEventDetails(String eventSlug) {
//        var apiKey = apiKeyLoader.getApiKeyForEventSlug(eventSlug);
//        var eventsApi = apiSupplier.eventsApi(apiKey);
//        return eventsApi.rootRetrieve(eventSlug);
//    }
}
