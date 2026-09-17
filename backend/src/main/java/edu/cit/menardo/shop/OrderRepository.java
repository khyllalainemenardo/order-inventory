package edu.cit.menardo.shop;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderRecord, UUID> {

    @EntityGraph(attributePaths = "items")
    List<OrderRecord> findAllByOrderByCreatedAtDesc();
}
