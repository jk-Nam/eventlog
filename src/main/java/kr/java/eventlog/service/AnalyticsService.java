package kr.java.eventlog.service;

import kr.java.eventlog.dto.ConversionFunnelDto;
import kr.java.eventlog.dto.ErrorAnalysisDto;
import kr.java.eventlog.dto.RevenueAnalysisDto;
import kr.java.eventlog.model.EventType;
import kr.java.eventlog.repository.ErrorRepository;
import kr.java.eventlog.repository.EventRepository;
import kr.java.eventlog.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventRepository eventRepository;
    private final PurchaseRepository purchaseRepository;
    private final ErrorRepository errorRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 1. 전환 퍼널 분석: 페이지 조회 → 클릭 → 구매 전환율
     */
    public ConversionFunnelDto analyzeConversionFunnel() {
        String sql = """
            WITH funnel AS (
              SELECT
                user_id,
                MAX(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 ELSE 0 END) as viewed,
                MAX(CASE WHEN event_type = 'CLICK' THEN 1 ELSE 0 END) as clicked,
                MAX(CASE WHEN event_type = 'PURCHASE' THEN 1 ELSE 0 END) as purchased
              FROM events
              GROUP BY user_id
            )
            SELECT
              COUNT(*) as total_users,
              SUM(viewed) as viewed_users,
              SUM(clicked) as clicked_users,
              SUM(purchased) as purchased_users,
              ROUND(SUM(clicked)::numeric / NULLIF(SUM(viewed), 0) * 100, 2) as click_rate,
              ROUND(SUM(purchased)::numeric / NULLIF(SUM(viewed), 0) * 100, 2) as conversion_rate
            FROM funnel
            """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);

        return ConversionFunnelDto.builder()
                .totalUsers(((Number) result.get("total_users")).longValue())
                .viewedUsers(((Number) result.get("viewed_users")).longValue())
                .clickedUsers(((Number) result.get("clicked_users")).longValue())
                .purchasedUsers(((Number) result.get("purchased_users")).longValue())
                .clickRate(result.get("click_rate") != null ? ((Number) result.get("click_rate")).doubleValue() : 0.0)
                .conversionRate(result.get("conversion_rate") != null ? ((Number) result.get("conversion_rate")).doubleValue() : 0.0)
                .build();
    }

    /**
     * 2. 매출 분석: 일별/사용자별 매출, 평균 구매액, 구매 빈도
     */
    public List<RevenueAnalysisDto> analyzeRevenue() {
        String sql = """
            SELECT
              DATE(e.timestamp) as date,
              COUNT(DISTINCT e.user_id) as unique_buyers,
              COUNT(*) as total_purchases,
              SUM(p.total_amount) as total_revenue,
              ROUND(AVG(p.total_amount)) as avg_order_value,
              ROUND(SUM(p.total_amount)::numeric / COUNT(DISTINCT e.user_id)) as revenue_per_user
            FROM events e
            JOIN purchases p ON e.id = p.event_id
            GROUP BY DATE(e.timestamp)
            ORDER BY date
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                RevenueAnalysisDto.builder()
                        .date(rs.getDate("date").toLocalDate())
                        .uniqueBuyers(rs.getLong("unique_buyers"))
                        .totalPurchases(rs.getLong("total_purchases"))
                        .totalRevenue(rs.getLong("total_revenue"))
                        .avgOrderValue(rs.getLong("avg_order_value"))
                        .revenuePerUser(rs.getLong("revenue_per_user"))
                        .build()
        );
    }

    /**
     * 3. 에러 모니터링: 에러 발생 빈도, 영향받는 사용자, 심각도
     */
    public List<ErrorAnalysisDto> analyzeErrors() {
        String sql = """
            SELECT
              er.error_code,
              er.error_message,
              er.severity,
              er.page,
              COUNT(*) as occurrence_count,
              COUNT(DISTINCT e.user_id) as affected_users,
              MAX(e.timestamp) as last_occurrence
            FROM errors er
            JOIN events e ON er.event_id = e.id
            GROUP BY er.error_code, er.error_message, er.severity, er.page
            ORDER BY occurrence_count DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                ErrorAnalysisDto.builder()
                        .errorCode(rs.getString("error_code"))
                        .errorMessage(rs.getString("error_message"))
                        .severity(rs.getString("severity"))
                        .page(rs.getString("page"))
                        .occurrenceCount(rs.getLong("occurrence_count"))
                        .affectedUsers(rs.getLong("affected_users"))
                        .lastOccurrence(rs.getTimestamp("last_occurrence").toLocalDateTime())
                        .build()
        );
    }

    /**
     * 전체 통계 요약
     */
    public void printAnalyticsSummary() {
        log.info("\n" + "=".repeat(80));
        log.info("데이터 집계 분석 결과");
        log.info("=".repeat(80));

        // 1. 전환 퍼널 분석
        log.info("\n[1] 전환 퍼널 분석 (페이지 조회 → 클릭 → 구매)");
        log.info("-".repeat(80));
        ConversionFunnelDto funnel = analyzeConversionFunnel();
        log.info("총 사용자 수: {}", funnel.getTotalUsers());
        log.info("페이지 조회 사용자: {} ({}%)", funnel.getViewedUsers(),
                String.format("%.1f", funnel.getViewedUsers() * 100.0 / funnel.getTotalUsers()));
        log.info("클릭한 사용자: {} ({}%)", funnel.getClickedUsers(),
                String.format("%.1f", funnel.getClickedUsers() * 100.0 / funnel.getTotalUsers()));
        log.info("구매한 사용자: {} ({}%)", funnel.getPurchasedUsers(),
                String.format("%.1f", funnel.getPurchasedUsers() * 100.0 / funnel.getTotalUsers()));
        log.info("클릭률 (CTR): {}%", funnel.getClickRate());
        log.info("전환율 (CVR): {}%", funnel.getConversionRate());

        // 2. 매출 분석
        log.info("\n[2] 일별 매출 분석");
        log.info("-".repeat(80));
        List<RevenueAnalysisDto> revenueList = analyzeRevenue();
        if (revenueList.isEmpty()) {
            log.info("매출 데이터가 없습니다.");
        } else {
            log.info(String.format("%-12s | %8s | %8s | %12s | %12s | %12s",
                    "날짜", "구매자수", "구매건수", "총매출", "평균구매액", "인당매출"));
            log.info("-".repeat(80));
            long totalRevenue = 0;
            for (RevenueAnalysisDto revenue : revenueList) {
                log.info(String.format("%-12s | %8d | %8d | %,12d원 | %,12d원 | %,12d원",
                        revenue.getDate(),
                        revenue.getUniqueBuyers(),
                        revenue.getTotalPurchases(),
                        revenue.getTotalRevenue(),
                        revenue.getAvgOrderValue(),
                        revenue.getRevenuePerUser()));
                totalRevenue += revenue.getTotalRevenue();
            }
            log.info("-".repeat(80));
            log.info("총 매출: {:,}원", totalRevenue);
        }

        // 3. 에러 분석
        log.info("\n[3] 에러 발생 현황");
        log.info("-".repeat(80));
        List<ErrorAnalysisDto> errors = analyzeErrors();
        if (errors.isEmpty()) {
            log.info("에러가 발생하지 않았습니다. ✓");
        } else {
            log.info(String.format("%-15s | %-10s | %-30s | %8s | %8s",
                    "에러코드", "심각도", "에러메시지", "발생횟수", "영향사용자"));
            log.info("-".repeat(80));
            for (ErrorAnalysisDto error : errors) {
                log.info(String.format("%-15s | %-10s | %-30s | %8d | %8d",
                        error.getErrorCode(),
                        error.getSeverity(),
                        error.getErrorMessage().substring(0, Math.min(30, error.getErrorMessage().length())),
                        error.getOccurrenceCount(),
                        error.getAffectedUsers()));
            }
            log.info("-".repeat(80));
            log.info("총 에러 유형: {}", errors.size());
            log.info("총 에러 발생 횟수: {}", errors.stream().mapToLong(ErrorAnalysisDto::getOccurrenceCount).sum());
        }

        log.info("\n" + "=".repeat(80));
    }
}
