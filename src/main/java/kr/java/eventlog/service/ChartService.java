package kr.java.eventlog.service;

import kr.java.eventlog.dto.ConversionFunnelDto;
import kr.java.eventlog.dto.ErrorAnalysisDto;
import kr.java.eventlog.dto.RevenueAnalysisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChartService {

    private final AnalyticsService analyticsService;
    private final JdbcTemplate jdbcTemplate;

    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 600;
    private static final String OUTPUT_DIR = "charts";

    /**
     * 모든 차트를 생성하고 저장합니다.
     */
    public void generateAllCharts() {
        // 출력 디렉토리 생성
        File directory = new File(OUTPUT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        log.info("\n" + "=".repeat(80));
        log.info("차트 생성 중...");
        log.info("=".repeat(80));

        try {
            // 1. 이벤트 타입별 분포 파이 차트
            createEventTypePieChart();

            // 2. 전환 퍼널 바 차트
            createConversionFunnelBarChart();

            // 3. 일별 매출 바 차트
            createRevenueBarChart();

            // 4. 에러 심각도별 파이 차트
            createErrorSeverityPieChart();

            log.info("\n✓ 모든 차트가 생성되었습니다!");
            log.info("저장 위치: ./{}/", OUTPUT_DIR);
            log.info("  - event_type_distribution.png");
            log.info("  - conversion_funnel.png");
            log.info("  - daily_revenue.png");
            log.info("  - error_severity.png");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("차트 생성 중 오류 발생", e);
        }
    }

    /**
     * 1. 이벤트 타입별 분포 파이 차트
     */
    private void createEventTypePieChart() throws IOException {
        String sql = "SELECT event_type, COUNT(*) as count FROM events GROUP BY event_type";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Map<String, Object> row : results) {
            String eventType = (String) row.get("event_type");
            Long count = ((Number) row.get("count")).longValue();
            dataset.setValue(eventType, count);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Event Type Distribution",
                dataset,
                true,  // legend
                true,  // tooltips
                false  // URLs
        );

        // 차트 스타일 설정
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("PAGE_VIEW", new Color(52, 152, 219));
        plot.setSectionPaint("CLICK", new Color(46, 204, 113));
        plot.setSectionPaint("PURCHASE", new Color(241, 196, 15));
        plot.setSectionPaint("ERROR", new Color(231, 76, 60));
        plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})", new DecimalFormat("0"), new DecimalFormat("0.0%")));

        // 배경 설정
        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundPaint(Color.WHITE);

        File outputFile = new File(OUTPUT_DIR + "/event_type_distribution.png");
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("✓ 이벤트 타입별 분포 차트 생성: {}", outputFile.getName());
    }

    /**
     * 2. 전환 퍼널 바 차트
     */
    private void createConversionFunnelBarChart() throws IOException {
        ConversionFunnelDto funnel = analyticsService.analyzeConversionFunnel();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(funnel.getViewedUsers(), "Users", "Page View");
        dataset.addValue(funnel.getClickedUsers(), "Users", "Click");
        dataset.addValue(funnel.getPurchasedUsers(), "Users", "Purchase");

        JFreeChart chart = ChartFactory.createBarChart(
                "Conversion Funnel (Page View → Click → Purchase)",
                "Stage",
                "Number of Users",
                dataset,
                PlotOrientation.VERTICAL,
                true,  // legend
                true,  // tooltips
                false  // URLs
        );

        // 차트 스타일 설정
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.getRenderer().setSeriesPaint(0, new Color(52, 152, 219));

        chart.setBackgroundPaint(Color.WHITE);

        // 부제목 추가 (전환율)
        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
                String.format("CTR: %.1f%% | CVR: %.1f%%",
                        funnel.getClickRate(),
                        funnel.getConversionRate()),
                new Font("SansSerif", Font.PLAIN, 12)
        ));

        File outputFile = new File(OUTPUT_DIR + "/conversion_funnel.png");
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("✓ 전환 퍼널 차트 생성: {}", outputFile.getName());
    }

    /**
     * 3. 일별 매출 바 차트
     */
    private void createRevenueBarChart() throws IOException {
        List<RevenueAnalysisDto> revenueList = analyticsService.analyzeRevenue();

        if (revenueList.isEmpty()) {
            log.warn("매출 데이터가 없어 차트를 생성하지 않습니다.");
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (RevenueAnalysisDto revenue : revenueList) {
            dataset.addValue(revenue.getTotalRevenue() / 1000.0, "Revenue (K₩)",
                    revenue.getDate().toString());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Daily Revenue",
                "Date",
                "Revenue (Thousand KRW)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // 차트 스타일 설정
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.getRenderer().setSeriesPaint(0, new Color(241, 196, 15));

        chart.setBackgroundPaint(Color.WHITE);

        // 총 매출 부제목
        long totalRevenue = revenueList.stream()
                .mapToLong(RevenueAnalysisDto::getTotalRevenue)
                .sum();
        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
                String.format("Total Revenue: ₩%,d", totalRevenue),
                new Font("SansSerif", Font.PLAIN, 12)
        ));

        File outputFile = new File(OUTPUT_DIR + "/daily_revenue.png");
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("✓ 일별 매출 차트 생성: {}", outputFile.getName());
    }

    /**
     * 4. 에러 심각도별 파이 차트
     */
    private void createErrorSeverityPieChart() throws IOException {
        String sql = "SELECT severity, COUNT(*) as count FROM errors GROUP BY severity";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        if (results.isEmpty()) {
            log.warn("에러 데이터가 없어 차트를 생성하지 않습니다.");
            return;
        }

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Map<String, Object> row : results) {
            String severity = (String) row.get("severity");
            Long count = ((Number) row.get("count")).longValue();
            dataset.setValue(severity, count);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Error Severity Distribution",
                dataset,
                true,
                true,
                false
        );

        // 차트 스타일 설정
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("HIGH", new Color(231, 76, 60));
        plot.setSectionPaint("MEDIUM", new Color(243, 156, 18));
        plot.setSectionPaint("LOW", new Color(241, 196, 15));
        plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})", new DecimalFormat("0"), new DecimalFormat("0.0%")));

        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundPaint(Color.WHITE);

        File outputFile = new File(OUTPUT_DIR + "/error_severity.png");
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("✓ 에러 심각도 차트 생성: {}", outputFile.getName());
    }
}
