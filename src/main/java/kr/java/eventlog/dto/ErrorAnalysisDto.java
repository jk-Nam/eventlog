package kr.java.eventlog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorAnalysisDto {
    private String errorCode;
    private String errorMessage;
    private String severity;
    private String page;
    private Long occurrenceCount;
    private Long affectedUsers;
    private LocalDateTime lastOccurrence;
}
