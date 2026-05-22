package kr.java.eventlog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String id;
    private EventType eventType;
    private String userId;
    private String sessionId;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;

    public static Event create(EventType eventType, String userId, String sessionId, Map<String, Object> metadata) {
        return Event.builder()
                .id(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }
}
