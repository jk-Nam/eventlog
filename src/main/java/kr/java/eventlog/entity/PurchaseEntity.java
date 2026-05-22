package kr.java.eventlog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "purchases")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseEntity {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false, length = 20)
    private String paymentMethod;
}
