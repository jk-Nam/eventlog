package kr.java.eventlog.repository;

import kr.java.eventlog.entity.ClickEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickRepository extends JpaRepository<ClickEntity, String> {
}
