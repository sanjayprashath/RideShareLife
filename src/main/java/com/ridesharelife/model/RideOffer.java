package com.ridesharelife.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
public class RideOffer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) private User driver;
    @NotBlank @Size(max = 120) private String origin;
    @NotBlank @Size(max = 120) private String destination;
    @NotNull private LocalDateTime departureTime;
    @Min(1) @Max(20) private int totalSeats;
    @Min(0) @Max(20) private int availableSeats;
    protected RideOffer() {}
    public RideOffer(User driver, String origin, String destination, LocalDateTime departureTime, int seats) {
        this.driver = driver; this.origin = origin; this.destination = destination; this.departureTime = departureTime;
        this.totalSeats = seats; this.availableSeats = seats;
    }
    public Long getId() { return id; }
    public User getDriver() { return driver; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public int getTotalSeats() { return totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public void reserveSeat() { availableSeats--; }
}
