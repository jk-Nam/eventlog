package kr.java.eventlog.repository;

import kr.java.eventlog.entity.PurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseEntity, String> {
}
