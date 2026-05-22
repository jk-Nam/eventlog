package kr.java.eventlog.repository;

import kr.java.eventlog.entity.PageViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageViewRepository extends JpaRepository<PageViewEntity, String> {
}
