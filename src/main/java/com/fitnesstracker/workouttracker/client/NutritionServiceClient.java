package com.fitnesstracker.workouttracker.client;

import com.fitnesstracker.workouttracker.config.PulseTrackProperties;
import com.fitnesstracker.workouttracker.dto.MealPlanRequest;
import com.fitnesstracker.workouttracker.dto.MealPlanResponse;
import com.fitnesstracker.workouttracker.dto.NutritionStats;
import com.fitnesstracker.workouttracker.dto.RemoteResult;
import com.fitnesstracker.workouttracker.dto.RemoteServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.function.Supplier;

/**
 * The one place PulseTrack talks to the nutrition microservice over HTTP.
 *
 * <p>Every call is funnelled through {@link #call(String, Supplier)} so a
 * microservice that is down, slow, or rejecting our credentials always produces
 * a {@link RemoteServiceStatus} the UI can show, never an exception that reaches
 * the user as a 500 page.
 */
@Component
public class NutritionServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NutritionServiceClient.class);
    private static final String PLANS = "/api/meal-plans";

    private final RestTemplate restTemplate;
    private final PulseTrackProperties.NutritionService config;

    public NutritionServiceClient(RestTemplate nutritionRestTemplate, PulseTrackProperties properties) {
        this.restTemplate = nutritionRestTemplate;
        this.config = properties.getNutritionService();
    }

    public String baseUrl() {
        return config.getBaseUrl();
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    /** GET /api/meal-plans - every plan the service knows about. */
    public RemoteResult<List<MealPlanResponse>> findAll() {
        return call("list meal plans", () -> restTemplate.exchange(
                PLANS, HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<MealPlanResponse>>() {
                }).getBody());
    }

    /** GET /api/meal-plans/{id} */
    public RemoteResult<MealPlanResponse> findById(Long id) {
        return call("load meal plan " + id,
                () -> restTemplate.getForObject(PLANS + "/{id}", MealPlanResponse.class, id));
    }

    /** GET /api/meal-plans/search - the multi parameter custom endpoint. */
    public RemoteResult<List<MealPlanResponse>> search(String member, String goal,
                                                       Integer minCalories, Integer maxCalories) {
        String uri = UriComponentsBuilder.fromPath(PLANS + "/search")
                .queryParamIfPresent("member", java.util.Optional.ofNullable(blankToNull(member)))
                .queryParamIfPresent("goal", java.util.Optional.ofNullable(blankToNull(goal)))
                .queryParamIfPresent("minCalories", java.util.Optional.ofNullable(minCalories))
                .queryParamIfPresent("maxCalories", java.util.Optional.ofNullable(maxCalories))
                .build().toUriString();

        return call("search meal plans", () -> restTemplate.exchange(
                uri, HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<MealPlanResponse>>() {
                }).getBody());
    }

    /** POST /api/meal-plans - the microservice computes the macros and stores the plan. */
    public RemoteResult<MealPlanResponse> create(MealPlanRequest request) {
        return call("create meal plan", () -> {
            ResponseEntity<MealPlanResponse> response =
                    restTemplate.postForEntity(PLANS, request, MealPlanResponse.class);
            return response.getBody();
        });
    }

    /** PUT /api/meal-plans/{id} - recalculates an existing plan from new inputs. */
    public RemoteResult<MealPlanResponse> update(Long id, MealPlanRequest request) {
        return call("update meal plan " + id, () -> restTemplate.exchange(
                PLANS + "/{id}", HttpMethod.PUT, new HttpEntity<>(request),
                MealPlanResponse.class, id).getBody());
    }

    /** DELETE /api/meal-plans/{id} */
    public RemoteResult<Boolean> delete(Long id) {
        return call("delete meal plan " + id, () -> {
            restTemplate.delete(PLANS + "/{id}", id);
            return Boolean.TRUE;
        });
    }

    /** GET /api/meal-plans/stats - aggregate figures for the admin dashboard. */
    public RemoteResult<NutritionStats> stats() {
        return call("load nutrition stats",
                () -> restTemplate.getForObject(PLANS + "/stats", NutritionStats.class));
    }

    /** The most recent plan for one member, or an empty result when they have none. */
    public RemoteResult<MealPlanResponse> latestFor(String username) {
        RemoteResult<List<MealPlanResponse>> plans = search(username, null, null, null);
        if (!plans.isAvailable()) {
            return RemoteResult.failed(plans.status());
        }
        MealPlanResponse latest = plans.data() == null || plans.data().isEmpty()
                ? null
                : plans.data().get(0);
        return new RemoteResult<>(latest, plans.status());
    }

    // ------------------------------------------------------------------
    // Error handling: one funnel, one place to change the degradation rules
    // ------------------------------------------------------------------

    private <T> RemoteResult<T> call(String action, Supplier<T> invocation) {
        if (!config.isEnabled()) {
            return RemoteResult.failed(RemoteServiceStatus.disabled(config.getBaseUrl()));
        }
        try {
            return RemoteResult.ok(invocation.get(), config.getBaseUrl());
        } catch (ResourceAccessException e) {
            // Connection refused, DNS failure, or our timeout elapsed.
            log.warn("Nutrition service unreachable while trying to {}: {}", action, e.getMessage());
            return RemoteResult.failed(RemoteServiceStatus.down(config.getBaseUrl(),
                    "The nutrition service is not responding. Start it with "
                            + "\"mvn spring-boot:run\" in the nutrition-service folder, then reload this page."));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.warn("Nutrition service rejected our credentials while trying to {}", action);
            return RemoteResult.failed(RemoteServiceStatus.down(config.getBaseUrl(),
                    "The nutrition service rejected PulseTrack's Basic Auth credentials. "
                            + "Check pulsetrack.nutrition-service.username and password."));
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Nutrition service returned 404 while trying to {}", action);
            return RemoteResult.failed(RemoteServiceStatus.down(config.getBaseUrl(),
                    "The nutrition service has no record for that request."));
        } catch (HttpClientErrorException e) {
            log.warn("Nutrition service rejected the request to {}: {}", action, e.getStatusCode());
            return RemoteResult.failed(RemoteServiceStatus.down(config.getBaseUrl(),
                    "The nutrition service rejected the request (" + statusText(e.getStatusCode()) + ")."));
        } catch (HttpServerErrorException e) {
            log.error("Nutrition service failed to {}: {}", action, e.getStatusCode());
            return RemoteResult.failed(RemoteServiceStatus.down(config.getBaseUrl(),
                    "The nutrition service reported an internal error (" + statusText(e.getStatusCode()) + ")."));
        } catch (RuntimeException e) {
            log.error("Unexpected failure calling the nutrition service to {}", action, e);
            return RemoteResult.failed(RemoteServiceStatus.down(config.getBaseUrl(),
                    "PulseTrack could not read a response from the nutrition service."));
        }
    }

    private static String statusText(HttpStatusCode status) {
        return "HTTP " + status.value();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
