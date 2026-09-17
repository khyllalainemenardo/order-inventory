package edu.cit.menardo.shop;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderRecord, UUID> {

    /** Order history, newest first, with line items loaded in the same query. */
    @EntityGraph(attributePaths = "items")
    List<OrderRecord> findAllByOrderByCreatedAtDesc();
}
