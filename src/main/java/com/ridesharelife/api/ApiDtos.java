package com.ridesharelife.api;

import com.ridesharelife.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {}
    public record CreateUser(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=150) String email) {}
    public record CreateOffer(@NotNull Long driverId, @NotBlank @Size(max=120) String origin,
        @NotBlank @Size(max=120) String destination, @NotNull @Future LocalDateTime departureTime,
        @Min(1) @Max(20) int seats) {}
    public record CreateRequest(@NotNull Long riderId, @NotNull Long rideOfferId) {}
    public record DecideRequest(@NotNull Long driverId, @NotNull RequestStatus status) {}
    public record UserView(Long id, String name, String email) { public static UserView of(User u) { return new UserView(u.getId(),u.getName(),u.getEmail()); } }
    public record OfferView(Long id, UserView driver, String origin, String destination, LocalDateTime departureTime, int totalSeats, int availableSeats) {
        public static OfferView of(RideOffer r) { return new OfferView(r.getId(),UserView.of(r.getDriver()),r.getOrigin(),r.getDestination(),r.getDepartureTime(),r.getTotalSeats(),r.getAvailableSeats()); }
    }
    public record RequestView(Long id, UserView rider, OfferView ride, RequestStatus status, LocalDateTime requestedAt) {
        public static RequestView of(RideRequest r) { return new RequestView(r.getId(),UserView.of(r.getRider()),OfferView.of(r.getRideOffer()),r.getStatus(),r.getRequestedAt()); }
    }
    public record RideHistory(java.util.List<OfferView> ridesAsDriver, java.util.List<RequestView> ridesAsRider,
                              java.util.List<RequestView> requestsAsDriver) {}
}
