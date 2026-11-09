package at.control.shiftcontrol.shift.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import at.shiftcontrol.lib.entity.Activity;
import at.shiftcontrol.lib.entity.Event;
import at.shiftcontrol.lib.entity.SocialMediaLink;
import at.shiftcontrol.shiftservice.dto.event.EventDto;
import at.shiftcontrol.shiftservice.dto.event.EventModificationDto;
import at.shiftcontrol.shiftservice.dto.event.SocialMediaLinkDto;
import at.shiftcontrol.shiftservice.dto.event.schedule.ActivityScheduleDto;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class EventMapper {
    public static EventDto toEventDto(Event event) {
        Instant now = Instant.now();
        return EventDto.builder()
            .id(String.valueOf(event.getId()))
            .name(event.getName())
            .longDescription(event.getLongDescription())
            .shortDescription(event.getShortDescription())
            .startTime(event.getStartTime())
            .endTime(event.getEndTime())
            .active(isActive(now, event))
            .socialMediaLinks(toSocialMediaLinkDto(event.getSocialMediaLinks()))
            .rewardPointsRedeemUrl(event.getRewardPointsRedeemUrl())
            .build();
    }

    public static List<EventDto> toEventDto(Collection<Event> events) {
        return events.stream()
            .map(EventMapper::toEventDto)
            .toList();
    }

    public static Event toEvent(EventModificationDto modificationDto) {
        return Event.builder()
            .name(modificationDto.getName())
            .shortDescription(modificationDto.getShortDescription())
            .longDescription(modificationDto.getLongDescription())
            .startTime(modificationDto.getStartTime())
            .endTime(modificationDto.getEndTime())
            .rewardPointsRedeemUrl(modificationDto.getRewardPointsRedeemUrl())
            .build();
    }

    public static void updateEvent(Event event, EventModificationDto eventModificationDto) {
        event.setName(eventModificationDto.getName());
        event.setShortDescription(eventModificationDto.getShortDescription());
        event.setLongDescription(eventModificationDto.getLongDescription());
        event.setStartTime(eventModificationDto.getStartTime());
        event.setEndTime(eventModificationDto.getEndTime());
        event.setRewardPointsRedeemUrl(eventModificationDto.getRewardPointsRedeemUrl());
    }

    public static ActivityScheduleDto toActivityScheduleDto(Event event, List<Activity> activities) {
        return ActivityScheduleDto.builder()
            .event(toEventDto(event))
            .activities(ActivityMapper.toActivityDto(activities))
            .build();
    }

    public static Collection<SocialMediaLinkDto> toSocialMediaLinkDto(Collection<SocialMediaLink> links) {
        if (links == null) {
            return Collections.emptyList();
        }
        return links.stream().map(EventMapper::toSocialMediaLinkDto).toList();
    }

    public static SocialMediaLinkDto toSocialMediaLinkDto(SocialMediaLink link) {
        return SocialMediaLinkDto.builder()
            .type(link.getType())
            .url(link.getUrl())
            .build();
    }

    public static Collection<SocialMediaLink> toSocialMediaLink(Collection<SocialMediaLinkDto> links, Event event) {
        if (links == null) {
            return Collections.emptyList();
        }
        return links.stream().map(l -> toSocialMediaLink(l, event)).toList();
    }

    public static SocialMediaLink toSocialMediaLink(SocialMediaLinkDto link, Event event) {
        return SocialMediaLink.builder()
            .type(link.getType())
            .url(link.getUrl())
            .event(event)
            .build();
    }

    private static boolean isActive(Instant now, Event event) {
        return event.getStartTime().isBefore(now) && now.isBefore(event.getEndTime());
    }
}
