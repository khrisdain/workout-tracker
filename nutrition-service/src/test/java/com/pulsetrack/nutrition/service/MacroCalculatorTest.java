package com.pulsetrack.nutrition.service;

import com.pulsetrack.nutrition.dto.MealPlanRequest;
import com.pulsetrack.nutrition.model.ActivityLevel;
import com.pulsetrack.nutrition.model.BiologicalSex;
import com.pulsetrack.nutrition.model.MealPlan;
import com.pulsetrack.nutrition.model.NutritionGoal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the domain logic that justifies this being a service
 * rather than a table in the primary application's database.
 */
class MacroCalculatorTest {

    private final MacroCalculator calculator = new MacroCalculator();

    private MealPlanRequest request(NutritionGoal goal, ActivityLevel level, double kg, int cm,
                                    int age, BiologicalSex sex, int weeklyMinutes) {
        return new MealPlanRequest("member", "Test Member", goal, level, kg, cm, age, sex, weeklyMinutes, null);
    }

    @Test
    @DisplayName("Mifflin-St Jeor is applied exactly for a male reference subject")
    void computesMaleBmr() {
        // 10(80) + 6.25(180) - 5(30) + 5 = 1780
        assertThat(calculator.basalMetabolicRate(80.0, 180, 30, BiologicalSex.MALE)).isEqualTo(1780);
    }

    @Test
    @DisplayName("Mifflin-St Jeor is applied exactly for a female reference subject")
    void computesFemaleBmr() {
        // 10(60) + 6.25(165) - 5(30) - 161 = 1320
        assertThat(calculator.basalMetabolicRate(60.0, 165, 30, BiologicalSex.FEMALE)).isEqualTo(1320);
    }

    @Test
    @DisplayName("the unspecified constant sits between the male and female constants")
    void unspecifiedSitsBetween() {
        int male = calculator.basalMetabolicRate(70.0, 175, 30, BiologicalSex.MALE);
        int female = calculator.basalMetabolicRate(70.0, 175, 30, BiologicalSex.FEMALE);
        int unspecified = calculator.basalMetabolicRate(70.0, 175, 30, BiologicalSex.UNSPECIFIED);

        assertThat(unspecified).isBetween(female, male);
    }

    @Test
    @DisplayName("training minutes reported by PulseTrack raise maintenance calories")
    void trainingVolumeRaisesMaintenance() {
        int resting = calculator.maintenanceCalories(1800, ActivityLevel.MODERATE, 0);
        int training = calculator.maintenanceCalories(1800, ActivityLevel.MODERATE, 300);

        assertThat(training).isGreaterThan(resting);
    }

    @Test
    @DisplayName("a cut lands below maintenance and a bulk above it")
    void goalMovesTheTargetInTheRightDirection() {
        MealPlan cut = new MealPlan();
        MealPlan bulk = new MealPlan();
        calculator.apply(cut, request(NutritionGoal.CUT, ActivityLevel.MODERATE, 90.0, 180, 30, BiologicalSex.MALE, 200));
        calculator.apply(bulk, request(NutritionGoal.BULK, ActivityLevel.MODERATE, 90.0, 180, 30, BiologicalSex.MALE, 200));

        assertThat(cut.getDailyCalories()).isLessThan(cut.getMaintenanceCalories());
        assertThat(bulk.getDailyCalories()).isGreaterThan(bulk.getMaintenanceCalories());
    }

    @Test
    @DisplayName("a cut is floored at 110% of resting energy however aggressive the multiplier")
    void neverPrescribesBelowTheFloor() {
        // Small, sedentary, untrained: the raw cut multiplier would dip under the floor.
        MealPlan plan = new MealPlan();
        calculator.apply(plan, request(NutritionGoal.CUT, ActivityLevel.SEDENTARY, 45.0, 150, 60, BiologicalSex.FEMALE, 0));

        assertThat(plan.getDailyCalories())
                .isGreaterThanOrEqualTo((int) Math.round(plan.getBasalMetabolicRate() * 1.10));
    }

    @Test
    @DisplayName("the macro split adds back up to the calorie target")
    void macrosReconcileWithCalories() {
        MealPlan plan = new MealPlan();
        calculator.apply(plan, request(NutritionGoal.MAINTAIN, ActivityLevel.HIGH, 78.0, 176, 27,
                BiologicalSex.UNSPECIFIED, 240));

        int fromMacros = plan.getProteinGrams() * 4 + plan.getCarbGrams() * 4 + plan.getFatGrams() * 9;

        // Rounding to whole grams costs a handful of calories, never more.
        assertThat(fromMacros).isCloseTo(plan.getDailyCalories(),
                org.assertj.core.data.Offset.offset(12));
    }

    @Test
    @DisplayName("a cut prescribes more protein per kilogram than a bulk")
    void cutProtectsProtein() {
        MealPlan cut = new MealPlan();
        MealPlan bulk = new MealPlan();
        calculator.apply(cut, request(NutritionGoal.CUT, ActivityLevel.MODERATE, 80.0, 180, 30, BiologicalSex.MALE, 150));
        calculator.apply(bulk, request(NutritionGoal.BULK, ActivityLevel.MODERATE, 80.0, 180, 30, BiologicalSex.MALE, 150));

        assertThat(cut.getProteinGrams()).isGreaterThan(bulk.getProteinGrams());
    }

    @Test
    @DisplayName("carbohydrate is never negative, even when protein and fat eat the whole budget")
    void carbsNeverGoNegative() {
        assertThat(calculator.carbGrams(1200, 250, 90)).isZero();
    }

    @Test
    @DisplayName("hydration scales with bodyweight and training volume")
    void hydrationScales() {
        double light = calculator.hydrationLitres(60.0, 0);
        double heavy = calculator.hydrationLitres(100.0, 500);

        assertThat(heavy).isGreaterThan(light);
        assertThat(light).isGreaterThan(1.5);
    }

    @Test
    @DisplayName("a missing sex value falls back to the midpoint rather than failing")
    void nullSexIsTolerated() {
        MealPlan plan = new MealPlan();
        calculator.apply(plan, request(NutritionGoal.MAINTAIN, ActivityLevel.LIGHT, 70.0, 170, 30, null, 100));

        assertThat(plan.getSex()).isEqualTo(BiologicalSex.UNSPECIFIED);
        assertThat(plan.getDailyCalories()).isPositive();
    }

    @Test
    @DisplayName("the human readable summary reports the numbers that were stored")
    void summaryMatchesTheStoredFigures() {
        MealPlan plan = new MealPlan();
        calculator.apply(plan, request(NutritionGoal.CUT, ActivityLevel.HIGH, 85.0, 183, 35, BiologicalSex.MALE, 320));

        assertThat(plan.getSummary())
                .contains(String.valueOf(plan.getDailyCalories()))
                .contains("deficit")
                .contains("320 minutes");
    }
}
