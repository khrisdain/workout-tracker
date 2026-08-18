package com.pulsetrack.nutrition.controller;

import com.pulsetrack.nutrition.dto.MealPlanRequest;
import com.pulsetrack.nutrition.dto.MealPlanResponse;
import com.pulsetrack.nutrition.dto.NutritionStats;
import com.pulsetrack.nutrition.model.MealPlan;
import com.pulsetrack.nutrition.model.NutritionGoal;
import com.pulsetrack.nutrition.service.MealPlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * The full REST surface of the nutrition microservice.
 *
 * <pre>
 * GET    /api/meal-plans           200  every plan, newest first
 * GET    /api/meal-plans/{id}      200  one plan            404 when unknown
 * POST   /api/meal-plans           201  created, Location header set
 * PUT    /api/meal-plans/{id}      200  recalculated        404 when unknown
 * DELETE /api/meal-plans/{id}      204  no content          404 when unknown
 * GET    /api/meal-plans/search    200  multi parameter filter
 * GET    /api/meal-plans/stats     200  aggregates for the admin dashboard
 * </pre>
 *
 * Every route requires HTTP Basic credentials; see
 * {@link com.pulsetrack.nutrition.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {

    private final MealPlanService service;

    public MealPlanController(MealPlanService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MealPlanResponse>> findAll() {
        return ResponseEntity.ok(service.findAll().stream().map(MealPlanResponse::from).toList());
    }

    /**
     * Custom endpoint filtering on up to four parameters at once, any subset of
     * which may be omitted.
     */
    @GetMapping("/search")
    public ResponseEntity<List<MealPlanResponse>> search(
            @RequestParam(required = false) String member,
            @RequestParam(required = false) NutritionGoal goal,
            @RequestParam(required = false) Integer minCalories,
            @RequestParam(required = false) Integer maxCalories) {

        List<MealPlanResponse> results = service.search(member, goal, minCalories, maxCalories)
                .stream().map(MealPlanResponse::from).toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/stats")
    public ResponseEntity<NutritionStats> stats() {
        return ResponseEntity.ok(service.stats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealPlanResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(MealPlanResponse.from(service.findById(id)));
    }

    @PostMapping
    public ResponseEntity<MealPlanResponse> create(@Valid @RequestBody MealPlanRequest request) {
        MealPlan created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(MealPlanResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealPlanResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody MealPlanRequest request) {
        return ResponseEntity.ok(MealPlanResponse.from(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
