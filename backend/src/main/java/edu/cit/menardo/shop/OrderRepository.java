package edu.cit.menardo.shop;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<OrderRecord, UUID> {
}
