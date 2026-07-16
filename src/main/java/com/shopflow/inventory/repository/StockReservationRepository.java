package com.shopflow.inventory.repository;

import com.shopflow.inventory.domain.ReservationStatus;
import com.shopflow.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.Instant;
import java.util.List;

@RestResource(exported = false)
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /**
     * 상태 전이를 조건부로 원자 처리(검토 보고서 #7). from 상태일 때만 to로 바꾸고 1행 반환.
     * 동시 스윕/확정/해제의 이중 처리를 막는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockReservation r set r.status = :to where r.id = :id and r.status = :from")
    int transition(@Param("id") Long id, @Param("from") ReservationStatus from, @Param("to") ReservationStatus to);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant time);
}
