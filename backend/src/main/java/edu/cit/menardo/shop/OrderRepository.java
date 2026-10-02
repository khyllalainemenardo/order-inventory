package edu.cit.menardo.shop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface OrderRepository extends JpaRepository<OrderRecord, UUID> {

    @EntityGraph(attributePaths = "items")
    List<OrderRecord> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderRecord o where o.orderId = :orderId")
    Optional<OrderRecord> findForUpdate(UUID orderId);

    @EntityGraph(attributePaths = "items")
    List<OrderRecord> findByStatusOrderByCreatedAtAsc(String status);
}
