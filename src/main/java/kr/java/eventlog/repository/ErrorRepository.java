package kr.java.eventlog.repository;

import kr.java.eventlog.entity.ErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<ErrorEntity, String> {
}
