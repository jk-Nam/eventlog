package kr.java.eventlog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "errors")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorEntity {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String errorCode;

    @Column(nullable = false, length = 500)
    private String errorMessage;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 500)
    private String page;
}
