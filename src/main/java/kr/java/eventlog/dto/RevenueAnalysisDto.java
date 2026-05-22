package kr.java.eventlog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalysisDto {
    private LocalDate date;
    private Long uniqueBuyers;
    private Long totalPurchases;
    private Long totalRevenue;
    private Long avgOrderValue;
    private Long revenuePerUser;
}
