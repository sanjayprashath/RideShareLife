package com.ridesharelife.repository;
import com.ridesharelife.model.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    boolean existsByRideOfferIdAndRiderId(Long offerId, Long riderId);
    List<RideRequest> findByRiderIdOrderByRequestedAtDesc(Long riderId);
    List<RideRequest> findByRideOfferDriverIdOrderByRequestedAtDesc(Long driverId);
}
