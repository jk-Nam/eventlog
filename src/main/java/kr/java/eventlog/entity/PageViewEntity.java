package kr.java.eventlog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "page_views")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageViewEntity {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 500)
    private String referrer;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(length = 200)
    private String userAgent;
}
