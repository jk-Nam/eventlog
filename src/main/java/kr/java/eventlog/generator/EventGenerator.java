package kr.java.eventlog.generator;

import kr.java.eventlog.model.Event;
import kr.java.eventlog.model.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class EventGenerator {

    private static final String[] USERS = {"user001", "user002", "user003", "user004", "user005"};
    private static final String[] PAGES = {"/home", "/products", "/cart", "/checkout", "/profile", "/about"};
    private static final String[] PRODUCTS = {"노트북", "마우스", "키보드", "모니터", "헤드셋"};
    private static final String[] ERROR_MESSAGES = {
            "NullPointerException",
            "Database connection timeout",
            "Invalid user input",
            "Payment gateway error",
            "Session expired"
    };
    private static final String[] ELEMENTS = {"btn-submit", "btn-buy", "link-product", "img-banner", "nav-menu"};

    private final Random random = new Random();

    /**
     * 지정된 개수만큼 랜덤 이벤트를 생성합니다.
     */
    public List<Event> generateEvents(int count) {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Event event = generateRandomEvent();
            events.add(event);
            log.debug("Generated event: {}", event);
        }

        log.info("Generated {} events", count);
        return events;
    }

    /**
     * 랜덤한 타입의 이벤트 하나를 생성합니다.
     */
    public Event generateRandomEvent() {
        EventType eventType = randomEventType();
        String userId = randomUser();
        String sessionId = UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> metadata = switch (eventType) {
            case PAGE_VIEW -> generatePageViewMetadata();
            case PURCHASE -> generatePurchaseMetadata();
            case ERROR -> generateErrorMetadata();
            case CLICK -> generateClickMetadata();
        };

        return Event.create(eventType, userId, sessionId, metadata);
    }

    private EventType randomEventType() {
        // 이벤트 발생 확률 조정: PAGE_VIEW > CLICK > PURCHASE > ERROR
        int rand = random.nextInt(100);
        if (rand < 40) return EventType.PAGE_VIEW;
        if (rand < 70) return EventType.CLICK;
        if (rand < 90) return EventType.PURCHASE;
        return EventType.ERROR;
    }

    private Map<String, Object> generatePageViewMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("url", randomElement(PAGES));
        metadata.put("referrer", random.nextBoolean() ? randomElement(PAGES) : "direct");
        metadata.put("durationSeconds", random.nextInt(300) + 5);
        metadata.put("userAgent", "Mozilla/5.0");
        return metadata;
    }

    private Map<String, Object> generatePurchaseMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        String product = randomElement(PRODUCTS);
        int quantity = random.nextInt(5) + 1;
        int price = (random.nextInt(10) + 1) * 10000;

        metadata.put("productId", "PROD-" + random.nextInt(1000));
        metadata.put("productName", product);
        metadata.put("price", price);
        metadata.put("quantity", quantity);
        metadata.put("totalAmount", price * quantity);
        metadata.put("paymentMethod", random.nextBoolean() ? "card" : "transfer");
        return metadata;
    }

    private Map<String, Object> generateErrorMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("errorCode", "ERR-" + (random.nextInt(9000) + 1000));
        metadata.put("errorMessage", randomElement(ERROR_MESSAGES));
        metadata.put("severity", random.nextBoolean() ? "HIGH" : "MEDIUM");
        metadata.put("page", randomElement(PAGES));
        return metadata;
    }

    private Map<String, Object> generateClickMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("elementId", randomElement(ELEMENTS));
        metadata.put("elementType", "button");
        metadata.put("pageUrl", randomElement(PAGES));
        metadata.put("x", random.nextInt(1920));
        metadata.put("y", random.nextInt(1080));
        return metadata;
    }

    private String randomUser() {
        return USERS[random.nextInt(USERS.length)];
    }

    private <T> T randomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }
}
