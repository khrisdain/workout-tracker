package com.pulsetrack.nutrition.service;

import com.pulsetrack.nutrition.dto.MealPlanRequest;
import com.pulsetrack.nutrition.dto.NutritionStats;
import com.pulsetrack.nutrition.model.MealPlan;
import com.pulsetrack.nutrition.model.NutritionGoal;
import com.pulsetrack.nutrition.repository.MealPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business layer for the meal plan resource. */
@Service
@Transactional(readOnly = true)
public class MealPlanService {

    private final MealPlanRepository plans;
    private final MacroCalculator calculator;

    public MealPlanService(MealPlanRepository plans, MacroCalculator calculator) {
        this.plans = plans;
        this.calculator = calculator;
    }

    public List<MealPlan> findAll() {
        return plans.findAllByOrderByCreatedAtDescIdDesc();
    }

    public MealPlan findById(Long id) {
        return plans.findById(id).orElseThrow(() -> new MealPlanNotFoundException(id));
    }

    public List<MealPlan> search(String member, NutritionGoal goal, Integer minCalories, Integer maxCalories) {
        String normalisedMember = (member == null || member.isBlank()) ? null : member.trim();
        return plans.search(normalisedMember, goal, minCalories, maxCalories);
    }

    @Transactional
    public MealPlan create(MealPlanRequest request) {
        MealPlan plan = new MealPlan();
        calculator.apply(plan, request);
        return plans.save(plan);
    }

    /** A PUT recalculates from scratch: the stored figures are always derived, never edited by hand. */
    @Transactional
    public MealPlan update(Long id, MealPlanRequest request) {
        MealPlan existing = findById(id);
        calculator.apply(existing, request);
        return plans.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        MealPlan existing = findById(id);
        plans.delete(existing);
    }

    public NutritionStats stats() {
        Map<String, Long> byGoal = new LinkedHashMap<>();
        for (Object[] row : plans.countByGoal()) {
            byGoal.put(((NutritionGoal) row[0]).name(), ((Number) row[1]).longValue());
        }
        return new NutritionStats(
                plans.count(),
                plans.countDistinctMembers(),
                (int) Math.round(plans.averageDailyCalories()),
                (int) Math.round(plans.averageProteinGrams()),
                byGoal);
    }
}
