package kr.java.eventlog.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.java.eventlog.generator.EventGenerator;
import kr.java.eventlog.model.Event;
import kr.java.eventlog.service.AnalyticsService;
import kr.java.eventlog.service.ChartService;
import kr.java.eventlog.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventGeneratorRunner implements CommandLineRunner {

    private final EventGenerator eventGenerator;
    private final EventService eventService;
    private final AnalyticsService analyticsService;
    private final ChartService chartService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(80));
        log.info("이벤트 생성기 시작");
        log.info("=".repeat(80));

        // 기본 100개의 이벤트 생성
        int eventCount = 100;

        // 명령줄 인자로 개수 지정 가능
        if (args.length > 0) {
            try {
                eventCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.warn("Invalid event count argument. Using default: 100");
            }
        }

        List<Event> events = eventGenerator.generateEvents(eventCount);

        log.info("\n생성된 이벤트 샘플 (처음 5개):");
        log.info("-".repeat(80));

        events.stream()
                .limit(5)
                .forEach(event -> {
                    try {
                        String json = objectMapper.writeValueAsString(event);
                        System.out.println(json);
                    } catch (Exception e) {
                        log.error("Failed to serialize event", e);
                    }
                });

        log.info("-".repeat(80));
        log.info("총 {}개의 이벤트가 생성되었습니다.", events.size());

        // 이벤트 타입별 통계
        log.info("\n이벤트 타입별 통계:");
        events.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Event::getEventType,
                        java.util.stream.Collectors.counting()
                ))
                .forEach((type, count) ->
                    log.info("  {} : {} 건 ({}%)",
                        type,
                        count,
                        String.format("%.1f", (count * 100.0 / events.size()))
                    )
                );

        // 데이터베이스에 저장
        log.info("\n데이터베이스에 저장 중...");
        eventService.saveEvents(events);
        log.info("✓ 데이터베이스 저장 완료!");

        log.info("=".repeat(80));

        // 데이터 집계 분석 실행
        analyticsService.printAnalyticsSummary();

        // 차트 생성
        chartService.generateAllCharts();
    }
}
