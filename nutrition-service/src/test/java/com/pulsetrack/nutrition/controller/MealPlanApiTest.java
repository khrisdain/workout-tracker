package com.pulsetrack.nutrition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsetrack.nutrition.dto.MealPlanRequest;
import com.pulsetrack.nutrition.model.ActivityLevel;
import com.pulsetrack.nutrition.model.BiologicalSex;
import com.pulsetrack.nutrition.model.NutritionGoal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Contract tests for the REST API: status codes, Basic Auth, validation and the
 * custom search endpoint, exercised the same way PulseTrack exercises them.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MealPlanApiTest {

    private static final String SERVICE_USER = "pulsetrack-app";
    private static final String SERVICE_PASSWORD = "app-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String json(MealPlanRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private MealPlanRequest sample(String member, NutritionGoal goal) {
        return new MealPlanRequest(member, "Test Member", goal, ActivityLevel.MODERATE,
                80.0, 180, 30, BiologicalSex.MALE, 240, "created by the test suite");
    }

    @Nested
    @DisplayName("HTTP Basic protection")
    class Authentication {

        @Test
        @DisplayName("an anonymous request is refused with 401")
        void anonymousIsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/meal-plans")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("wrong credentials are refused with 401")
        void badCredentialsRejected() throws Exception {
            mockMvc.perform(get("/api/meal-plans").with(httpBasic(SERVICE_USER, "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("the service account is accepted")
        void serviceAccountAccepted() throws Exception {
            mockMvc.perform(get("/api/meal-plans").with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("the actuator env endpoint is administrator only")
        void actuatorIsAdminOnly() throws Exception {
            mockMvc.perform(get("/actuator/env").with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/actuator/env").with(httpBasic("nutrition-admin", "admin-secret")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("CRUD with correct HTTP semantics")
    class Crud {

        @Test
        @DisplayName("GET all returns 200 and the seeded plans")
        void getAll() throws Exception {
            mockMvc.perform(get("/api/meal-plans").with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
        }

        @Test
        @DisplayName("POST returns 201, a Location header, and server computed macros")
        void postCreates() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(sample("api.test.create", NutritionGoal.MAINTAIN))))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.dailyCalories").isNumber())
                    .andExpect(jsonPath("$.proteinGrams").isNumber())
                    .andExpect(jsonPath("$.summary").isNotEmpty())
                    .andReturn();

            assertThat(result.getResponse().getHeader("Location")).contains("/api/meal-plans/");
        }

        @Test
        @DisplayName("GET by id returns 200 for a known plan and 404 for an unknown one")
        void getById() throws Exception {
            String body = mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(sample("api.test.read", NutritionGoal.CUT))))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();

            mockMvc.perform(get("/api/meal-plans/{id}", id).with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberUsername").value("api.test.read"));

            mockMvc.perform(get("/api/meal-plans/{id}", 999999).with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("PUT recalculates the plan and returns 200, or 404 when the id is unknown")
        void putUpdates() throws Exception {
            String body = mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(sample("api.test.update", NutritionGoal.MAINTAIN))))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();
            int before = objectMapper.readTree(body).get("dailyCalories").asInt();

            MealPlanRequest bulked = new MealPlanRequest("api.test.update", "Test Member",
                    NutritionGoal.BULK, ActivityLevel.ATHLETE, 80.0, 180, 30, BiologicalSex.MALE, 600, null);

            mockMvc.perform(put("/api/meal-plans/{id}", id)
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(bulked)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.goal").value("BULK"))
                    .andExpect(jsonPath("$.dailyCalories").value(org.hamcrest.Matchers.greaterThan(before)));

            mockMvc.perform(put("/api/meal-plans/{id}", 999999)
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(bulked)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE returns 204, then 404 on the second attempt")
        void deleteRemoves() throws Exception {
            String body = mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(sample("api.test.delete", NutritionGoal.CUT))))
                    .andReturn().getResponse().getContentAsString();
            long id = objectMapper.readTree(body).get("id").asLong();

            mockMvc.perform(delete("/api/meal-plans/{id}", id).with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/meal-plans/{id}", id).with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("a body missing required fields comes back as 400 with per field messages")
        void rejectsIncompleteBody() throws Exception {
            mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors.memberUsername").exists())
                    .andExpect(jsonPath("$.fieldErrors.goal").exists());
        }

        @Test
        @DisplayName("an out of range weight is rejected")
        void rejectsOutOfRangeWeight() throws Exception {
            MealPlanRequest absurd = new MealPlanRequest("api.test.bad", "Test", NutritionGoal.CUT,
                    ActivityLevel.MODERATE, 900.0, 180, 30, BiologicalSex.MALE, 100, null);

            mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(absurd)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.weightKg").exists());
        }

        @Test
        @DisplayName("an unknown enum value is rejected as a bad request, not a 500")
        void rejectsUnknownEnum() throws Exception {
            mockMvc.perform(post("/api/meal-plans")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"memberUsername":"x","goal":"NOT_A_GOAL","activityLevel":"MODERATE",
                                     "weightKg":80,"heightCm":180,"age":30}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("custom query endpoints")
    class CustomEndpoints {

        @Test
        @DisplayName("search filters on member, goal and a calorie range at once")
        void searchFiltersOnManyParameters() throws Exception {
            mockMvc.perform(post("/api/meal-plans")
                    .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(sample("api.test.search", NutritionGoal.BULK))));

            mockMvc.perform(get("/api/meal-plans/search")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .param("member", "api.test.search")
                            .param("goal", "BULK")
                            .param("minCalories", "1")
                            .param("maxCalories", "10000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].memberUsername").value("api.test.search"));
        }

        @Test
        @DisplayName("search with no parameters returns everything")
        void searchWithoutParametersReturnsAll() throws Exception {
            mockMvc.perform(get("/api/meal-plans/search").with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
        }

        @Test
        @DisplayName("a calorie range that matches nothing returns an empty array, not a 404")
        void emptySearchIsStillOk() throws Exception {
            mockMvc.perform(get("/api/meal-plans/search")
                            .with(httpBasic(SERVICE_USER, SERVICE_PASSWORD))
                            .param("minCalories", "99000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("stats aggregates plan counts and averages")
        void statsAggregates() throws Exception {
            mockMvc.perform(get("/api/meal-plans/stats").with(httpBasic(SERVICE_USER, SERVICE_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPlans").value(org.hamcrest.Matchers.greaterThan(0)))
                    .andExpect(jsonPath("$.distinctMembers").value(org.hamcrest.Matchers.greaterThan(0)))
                    .andExpect(jsonPath("$.averageDailyCalories").isNumber())
                    .andExpect(jsonPath("$.plansByGoal").exists());
        }
    }
}
