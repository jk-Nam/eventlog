package kr.java.eventlog.service;

import kr.java.eventlog.entity.*;
import kr.java.eventlog.model.Event;
import kr.java.eventlog.model.EventType;
import kr.java.eventlog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final PageViewRepository pageViewRepository;
    private final PurchaseRepository purchaseRepository;
    private final ErrorRepository errorRepository;
    private final ClickRepository clickRepository;

    @Transactional
    public void saveEvent(Event event) {
        saveEventWithDetails(event);
        log.debug("Saved event: {} - {}", event.getEventType(), event.getId());
    }

    @Transactional
    public void saveEvents(List<Event> events) {
        for (Event event : events) {
            saveEventWithDetails(event);
        }
        log.info("Saved {} events to database", events.size());
    }

    private void saveEventWithDetails(Event event) {
        // 1. 먼저 기본 Event 정보 저장
        EventEntity eventEntity = EventEntity.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .sessionId(event.getSessionId())
                .timestamp(event.getTimestamp())
                .build();
        eventRepository.save(eventEntity);

        // 2. 이벤트 타입에 따라 상세 정보 저장
        Map<String, Object> metadata = event.getMetadata();
        switch (event.getEventType()) {
            case PAGE_VIEW -> {
                PageViewEntity pageView = PageViewEntity.builder()
                        .eventId(event.getId())
                        .url((String) metadata.get("url"))
                        .referrer((String) metadata.get("referrer"))
                        .durationSeconds((Integer) metadata.get("durationSeconds"))
                        .userAgent((String) metadata.get("userAgent"))
                        .build();
                pageViewRepository.save(pageView);
            }
            case PURCHASE -> {
                PurchaseEntity purchase = PurchaseEntity.builder()
                        .eventId(event.getId())
                        .productId((String) metadata.get("productId"))
                        .productName((String) metadata.get("productName"))
                        .price((Integer) metadata.get("price"))
                        .quantity((Integer) metadata.get("quantity"))
                        .totalAmount((Integer) metadata.get("totalAmount"))
                        .paymentMethod((String) metadata.get("paymentMethod"))
                        .build();
                purchaseRepository.save(purchase);
            }
            case ERROR -> {
                ErrorEntity error = ErrorEntity.builder()
                        .eventId(event.getId())
                        .errorCode((String) metadata.get("errorCode"))
                        .errorMessage((String) metadata.get("errorMessage"))
                        .severity((String) metadata.get("severity"))
                        .page((String) metadata.get("page"))
                        .build();
                errorRepository.save(error);
            }
            case CLICK -> {
                ClickEntity click = ClickEntity.builder()
                        .eventId(event.getId())
                        .elementId((String) metadata.get("elementId"))
                        .elementType((String) metadata.get("elementType"))
                        .pageUrl((String) metadata.get("pageUrl"))
                        .x((Integer) metadata.get("x"))
                        .y((Integer) metadata.get("y"))
                        .build();
                clickRepository.save(click);
            }
        }
    }

    public long countByEventType(EventType eventType) {
        return eventRepository.countByEventType(eventType);
    }

    public List<EventEntity> findAll() {
        return eventRepository.findAll();
    }
}

