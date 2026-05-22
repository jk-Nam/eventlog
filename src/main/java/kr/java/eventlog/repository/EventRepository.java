package kr.java.eventlog.repository;

import kr.java.eventlog.entity.EventEntity;
import kr.java.eventlog.model.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, String> {

    List<EventEntity> findByEventType(EventType eventType);

    List<EventEntity> findByUserId(String userId);

    List<EventEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    long countByEventType(EventType eventType);
}
