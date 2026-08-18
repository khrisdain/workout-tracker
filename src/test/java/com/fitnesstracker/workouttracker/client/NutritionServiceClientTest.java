package com.fitnesstracker.workouttracker.client;

import com.fitnesstracker.workouttracker.dto.MealPlanResponse;
import com.fitnesstracker.workouttracker.dto.NutritionStats;
import com.fitnesstracker.workouttracker.dto.RemoteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/**
 * The integration point that has to survive the microservice being down. These
 * tests stub the transport so every failure mode can be forced deliberately.
 */
@SpringBootTest
class NutritionServiceClientTest {

    @Autowired
    private NutritionServiceClient client;

    @Autowired
    private RestTemplate nutritionRestTemplate;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(nutritionRestTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    @DisplayName("parses a successful meal plan list")
    void readsMealPlans() {
        server.expect(requestTo("http://localhost:8081/api/meal-plans"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":1,"memberUsername":"jordan","memberName":"Jordan Reyes","goal":"MAINTAIN",
                          "activityLevel":"MODERATE","dailyCalories":2740,"proteinGrams":141,
                          "carbGrams":353,"fatGrams":85,"hydrationLitres":2.9,"summary":"ok"}]
                        """, MediaType.APPLICATION_JSON));

        RemoteResult<List<MealPlanResponse>> result = client.findAll();

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).memberUsername()).isEqualTo("jordan");
        assertThat(result.data().get(0).proteinPercent()).isPositive();
    }

    @Test
    @DisplayName("degrades gracefully, not fatally, when the service refuses the connection")
    void handlesConnectionFailure() {
        server.expect(requestTo("http://localhost:8081/api/meal-plans/stats"))
                .andRespond(request -> {
                    throw new org.springframework.web.client.ResourceAccessException("Connection refused");
                });

        RemoteResult<NutritionStats> result = client.stats();

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.data()).isNull();
        assertThat(result.status().message()).contains("not responding");
    }

    @Test
    @DisplayName("reports a credentials problem distinctly from an outage")
    void handlesUnauthorized() {
        server.expect(requestTo("http://localhost:8081/api/meal-plans"))
                .andRespond(withUnauthorizedRequest());

        RemoteResult<List<MealPlanResponse>> result = client.findAll();

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.status().message()).contains("Basic Auth");
    }

    @Test
    @DisplayName("reports a server side failure without leaking the exception")
    void handlesServerError() {
        server.expect(requestTo("http://localhost:8081/api/meal-plans"))
                .andRespond(withServerError());

        RemoteResult<List<MealPlanResponse>> result = client.findAll();

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.status().message()).contains("internal error");
    }

    @Test
    @DisplayName("builds the multi parameter search URL from whatever filters are present")
    void buildsSearchQuery() {
        server.expect(requestTo("http://localhost:8081/api/meal-plans/search?member=priya&goal=CUT&minCalories=1500"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        RemoteResult<List<MealPlanResponse>> result = client.search("priya", "CUT", 1500, null);

        assertThat(result.isAvailable()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("an empty search result is a valid answer, not an outage")
    void emptyLatestIsNotAnOutage() {
        server.expect(requestTo("http://localhost:8081/api/meal-plans/search?member=nobody"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.OK)
                        .body("[]").contentType(MediaType.APPLICATION_JSON));

        RemoteResult<MealPlanResponse> result = client.latestFor("nobody");

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.data()).isNull();
    }
}
