package com.ridesharelife.api;

import com.ridesharelife.api.ApiDtos.*;
import com.ridesharelife.service.RideShareService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController @RequestMapping("/api") @Validated
public class RideShareController {
    private final RideShareService service;
    public RideShareController(RideShareService service) { this.service=service; }
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED) public UserView createUser(@Valid @RequestBody CreateUser input) { return service.createUser(input); }
    @GetMapping("/users/{id}") public UserView getUser(@PathVariable long id) { return service.getUser(id); }
    @PostMapping("/rides") @ResponseStatus(HttpStatus.CREATED) public OfferView createOffer(@Valid @RequestBody CreateOffer input) { return service.createOffer(input); }
    @GetMapping("/rides/search") public List<OfferView> search(@RequestParam @NotBlank String origin, @RequestParam @NotBlank String destination,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) { return service.search(origin,destination,from,to); }
    @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED) public RequestView request(@Valid @RequestBody CreateRequest input) { return service.requestSeat(input); }
    @PutMapping("/requests/{id}/decision") public RequestView decide(@PathVariable long id, @Valid @RequestBody DecideRequest input) { return service.decide(id,input); }
    @GetMapping("/users/{id}/history") public RideHistory history(@PathVariable long id) { return service.history(id); }
}
