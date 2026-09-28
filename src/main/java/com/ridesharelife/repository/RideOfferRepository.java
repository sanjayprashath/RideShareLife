package com.ridesharelife.repository;
import com.ridesharelife.model.RideOffer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface RideOfferRepository extends JpaRepository<RideOffer, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RideOffer r where r.id = :id") Optional<RideOffer> findByIdForUpdate(@Param("id") Long id);
    @Query("select r from RideOffer r where lower(r.origin) = lower(:origin) and lower(r.destination) = lower(:destination) and r.departureTime between :fromTime and :toTime and r.availableSeats > 0 order by r.departureTime")
    List<RideOffer> findMatches(@Param("origin") String origin, @Param("destination") String destination, @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);
    List<RideOffer> findByDriverIdOrderByDepartureTimeDesc(Long driverId);
}
