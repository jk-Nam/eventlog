package kr.java.eventlog.entity;

import jakarta.persistence.*;
import kr.java.eventlog.model.EventType;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType eventType;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 50)
    private String sessionId;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
