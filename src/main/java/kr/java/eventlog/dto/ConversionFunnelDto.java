package kr.java.eventlog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionFunnelDto {
    private Long totalUsers;
    private Long viewedUsers;
    private Long clickedUsers;
    private Long purchasedUsers;
    private Double clickRate;
    private Double conversionRate;
}
