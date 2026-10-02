package edu.cit.menardo.channel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ChannelOrderRepository extends JpaRepository<ChannelOrder, String> {

    Optional<ChannelOrder> findByShopOrderId(UUID shopOrderId);

    @Query("select o.tianggeOrderId from ChannelOrder o where o.decisionSent = false")
    List<String> findUnsentDecisions();

    @Query("select o.tianggeOrderId from ChannelOrder o where o.resolution is not null and o.resolutionSent = false")
    List<String> findUnsentResolutions();

    @Query("select o.tianggeOrderId from ChannelOrder o where o.cancelReceived = true and o.cancelConfirmed = false")
    List<String> findUnconfirmedCancellations();

    @Query("""
            select count(o) from ChannelOrder o
            where o.decisionSent = false
               or (o.resolution is not null and o.resolutionSent = false)
               or (o.cancelReceived = true and o.cancelConfirmed = false)""")
    long countUnsentReplies();
}
