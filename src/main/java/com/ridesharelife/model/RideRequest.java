package com.ridesharelife.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"ride_offer_id", "rider_id"}))
public class RideRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) private User rider;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) private RideOffer rideOffer;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RequestStatus status = RequestStatus.PENDING;
    @Column(nullable = false) private LocalDateTime requestedAt = LocalDateTime.now();
    protected RideRequest() {}
    public RideRequest(User rider, RideOffer offer) { this.rider = rider; this.rideOffer = offer; }
    public Long getId() { return id; }
    public User getRider() { return rider; }
    public RideOffer getRideOffer() { return rideOffer; }
    public RequestStatus getStatus() { return status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setStatus(RequestStatus status) { this.status = status; }
}
