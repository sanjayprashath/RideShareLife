package com.ridesharelife.service;

import com.ridesharelife.api.ApiDtos.*;
import com.ridesharelife.model.*;
import com.ridesharelife.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RideShareService {
    private final UserRepository users; private final RideOfferRepository offers; private final RideRequestRepository requests;
    public RideShareService(UserRepository users, RideOfferRepository offers, RideRequestRepository requests) { this.users=users; this.offers=offers; this.requests=requests; }
    @Transactional
    public UserView createUser(CreateUser input) {
        if (users.existsByEmailIgnoreCase(input.email())) throw new IllegalArgumentException("Email is already registered.");
        try { return UserView.of(users.save(new User(input.name().trim(), input.email().trim().toLowerCase()))); }
        catch (DataIntegrityViolationException ex) { throw new IllegalArgumentException("Email is already registered."); }
    }
    @Transactional(readOnly=true)
    public UserView getUser(long id) { return UserView.of(user(id)); }
    @Transactional
    public OfferView createOffer(CreateOffer input) {
        User driver = user(input.driverId());
        return OfferView.of(offers.save(new RideOffer(driver,input.origin().trim(),input.destination().trim(),input.departureTime(),input.seats())));
    }
    @Transactional(readOnly=true)
    public List<OfferView> search(String origin, String destination, LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) throw new IllegalArgumentException("from must be earlier than or equal to to.");
        return offers.findMatches(origin.trim(),destination.trim(),from,to).stream().map(OfferView::of).toList();
    }
    @Transactional
    public RequestView requestSeat(CreateRequest input) {
        User rider = user(input.riderId());
        RideOffer offer = offers.findById(input.rideOfferId()).orElseThrow(() -> new NoSuchElementException("Ride offer not found."));
        if (offer.getDriver().getId().equals(rider.getId())) throw new IllegalArgumentException("You cannot request a seat on your own ride.");
        if (requests.existsByRideOfferIdAndRiderId(offer.getId(), rider.getId())) throw new IllegalArgumentException("You already requested this ride.");
        return RequestView.of(requests.save(new RideRequest(rider, offer)));
    }
    @Transactional
    public RequestView decide(long requestId, DecideRequest input) {
        if (input.status() == RequestStatus.PENDING) throw new IllegalArgumentException("A request can only be approved or rejected.");
        RideRequest request = requests.findById(requestId).orElseThrow(() -> new NoSuchElementException("Ride request not found."));
        if (!request.getRideOffer().getDriver().getId().equals(input.driverId())) throw new IllegalArgumentException("Only this ride's driver can decide the request.");
        if (request.getStatus() != RequestStatus.PENDING) throw new IllegalArgumentException("This request has already been decided.");
        if (input.status() == RequestStatus.APPROVED) {
            RideOffer locked = offers.findByIdForUpdate(request.getRideOffer().getId()).orElseThrow(() -> new NoSuchElementException("Ride offer not found."));
            if (locked.getAvailableSeats() < 1) throw new IllegalArgumentException("No seats remain on this ride.");
            locked.reserveSeat();
        }
        request.setStatus(input.status());
        return RequestView.of(request);
    }
    @Transactional(readOnly=true)
    public RideHistory history(long userId) {
        user(userId);
        List<OfferView> asDriver = offers.findByDriverIdOrderByDepartureTimeDesc(userId).stream().map(OfferView::of).toList();
        List<RequestView> asRider = requests.findByRiderIdOrderByRequestedAtDesc(userId).stream().map(RequestView::of).toList();
        List<RequestView> asDriverRequests = requests.findByRideOfferDriverIdOrderByRequestedAtDesc(userId).stream().map(RequestView::of).toList();
        return new RideHistory(asDriver, asRider, asDriverRequests);
    }
    private User user(long id) { return users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found.")); }
}
