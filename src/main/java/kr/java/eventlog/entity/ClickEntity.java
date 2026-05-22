package kr.java.eventlog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clicks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEntity {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String elementId;

    @Column(nullable = false, length = 50)
    private String elementType;

    @Column(nullable = false, length = 500)
    private String pageUrl;

    @Column(nullable = false)
    private Integer x;

    @Column(nullable = false)
    private Integer y;
}
