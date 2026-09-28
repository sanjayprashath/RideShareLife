package com.ridesharelife;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:rideshare-test;DB_CLOSE_DELAY=-1") @AutoConfigureMockMvc
class RideShareApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void completeCarpoolFlowAndEnforceSeatAndOwnershipRules() throws Exception {
        long driver = createUser("Driver", "driver@example.com");
        long rider = createUser("Rider", "rider@example.com");
        long other = createUser("Other", "other@example.com");
        LocalDateTime departure = LocalDateTime.now().plusDays(2).withNano(0);
        long offer = createOffer(driver, departure, 1);

        mvc.perform(get("/api/rides/search").param("origin", "pune").param("destination", "mumbai")
                .param("from", departure.minusHours(1).toString()).param("to", departure.plusHours(1).toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(offer));
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("riderId", driver, "rideOfferId", offer))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("own ride")));

        String firstRequestJson = mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("riderId", rider, "rideOfferId", offer))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        long firstRequest = json.readTree(firstRequestJson).get("id").asLong();
        String requestJson = mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("riderId", other, "rideOfferId", offer))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long secondRequest = json.readTree(requestJson).get("id").asLong();

        mvc.perform(put("/api/requests/{id}/decision", secondRequest).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("driverId", driver, "status", "APPROVED"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.ride.availableSeats").value(0));
        mvc.perform(put("/api/requests/{id}/decision", secondRequest).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("driverId", driver, "status", "APPROVED"))))
            .andExpect(status().isBadRequest());

        String pendingJson = mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("riderId", rider, "rideOfferId", createOffer(driver, departure.plusDays(1), 1)))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long pending = json.readTree(pendingJson).get("id").asLong();
        mvc.perform(put("/api/requests/{id}/decision", pending).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("driverId", driver, "status", "APPROVED"))))
            .andExpect(status().isOk());
        mvc.perform(get("/api/users/{id}/history", driver)).andExpect(status().isOk())
            .andExpect(jsonPath("$.ridesAsDriver", hasSize(2))).andExpect(jsonPath("$.requestsAsDriver", hasSize(greaterThanOrEqualTo(2))));
        mvc.perform(get("/api/users/{id}/history", rider)).andExpect(status().isOk())
            .andExpect(jsonPath("$.ridesAsRider[*].id", hasItem((int) firstRequest)));
        mvc.perform(get("/api/users/{id}", driver)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("driver@example.com"));
    }

    @Test
    void rejectsApprovalWhenNoSeatsRemain() throws Exception {
        long driver = createUser("Full Driver", "full-driver@example.com");
        long riderA = createUser("Rider A", "rider-a@example.com");
        long riderB = createUser("Rider B", "rider-b@example.com");
        long offer = createOffer(driver, LocalDateTime.now().plusDays(3).withNano(0), 1);
        long requestA = createRequest(riderA, offer);
        long requestB = createRequest(riderB, offer);
        decide(driver, requestA, "APPROVED").andExpect(status().isOk());
        decide(driver, requestB, "APPROVED").andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("No seats remain")));
    }

    @Test
    void rejectsDecisionFromSomeoneWhoIsNotTheDriver() throws Exception {
        long driver = createUser("Owner", "owner@example.com");
        long rider = createUser("Passenger", "passenger@example.com");
        long stranger = createUser("Stranger", "stranger@example.com");
        long offer = createOffer(driver, LocalDateTime.now().plusDays(4).withNano(0), 1);
        long request = createRequest(rider, offer);
        decide(stranger, request, "APPROVED").andExpect(status().isBadRequest());
    }

    private long createUser(String name, String email) throws Exception {
        String response = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", name, "email", email))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    private long createOffer(long driver, LocalDateTime time, int seats) throws Exception {
        String response = mvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("driverId", driver, "origin", "Pune", "destination", "Mumbai", "departureTime", time.toString(), "seats", seats))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    private long createRequest(long rider, long offer) throws Exception {
        String response = mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("riderId", rider, "rideOfferId", offer))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    private org.springframework.test.web.servlet.ResultActions decide(long driver, long request, String status) throws Exception {
        return mvc.perform(put("/api/requests/{id}/decision", request).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("driverId", driver, "status", status))));
    }
}
