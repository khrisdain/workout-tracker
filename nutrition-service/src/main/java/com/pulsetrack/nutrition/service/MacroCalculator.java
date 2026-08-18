package com.pulsetrack.nutrition.service;

import com.pulsetrack.nutrition.dto.MealPlanRequest;
import com.pulsetrack.nutrition.model.ActivityLevel;
import com.pulsetrack.nutrition.model.BiologicalSex;
import com.pulsetrack.nutrition.model.MealPlan;
import com.pulsetrack.nutrition.model.NutritionGoal;
import org.springframework.stereotype.Component;

/**
 * The reason this service exists. It is not a pass through: given a member's
 * metrics and how much they trained last week, it derives a calorie target and a
 * macronutrient split.
 *
 * <p>The chain is:
 * <ol>
 *   <li>Resting energy from the Mifflin-St Jeor equation.</li>
 *   <li>Maintenance energy, resting scaled by the activity multiplier plus a
 *       bonus for the training minutes PulseTrack reported.</li>
 *   <li>A goal target, maintenance scaled by the goal multiplier and floored at
 *       110% of resting so a cut can never become dangerous.</li>
 *   <li>Protein from bodyweight, fat from a share of calories, carbohydrate
 *       from whatever energy is left.</li>
 * </ol>
 */
@Component
public class MacroCalculator {

    /** Roughly 5 kcal per logged training minute, spread across the week. */
    private static final double KCAL_PER_TRAINING_MINUTE = 5.0;
    private static final int PROTEIN_KCAL_PER_GRAM = 4;
    private static final int CARB_KCAL_PER_GRAM = 4;
    private static final int FAT_KCAL_PER_GRAM = 9;

    /** Applies the whole calculation to {@code plan}, overwriting any previous figures. */
    public void apply(MealPlan plan, MealPlanRequest request) {
        plan.setMemberUsername(request.memberUsername());
        plan.setMemberName(request.memberName());
        plan.setGoal(request.goal());
        plan.setActivityLevel(request.activityLevel());
        plan.setWeightKg(request.weightKg());
        plan.setHeightCm(request.heightCm());
        plan.setAge(request.age());
        plan.setSex(request.sex() == null ? BiologicalSex.UNSPECIFIED : request.sex());
        plan.setWeeklyTrainingMinutes(request.weeklyTrainingMinutes() == null ? 0 : request.weeklyTrainingMinutes());
        plan.setNotes(request.notes());

        int bmr = basalMetabolicRate(plan.getWeightKg(), plan.getHeightCm(), plan.getAge(), plan.getSex());
        int maintenance = maintenanceCalories(bmr, plan.getActivityLevel(), plan.getWeeklyTrainingMinutes());
        int target = goalCalories(maintenance, bmr, plan.getGoal());

        int protein = proteinGrams(plan.getWeightKg(), plan.getGoal());
        int fat = fatGrams(target, plan.getGoal());
        int carbs = carbGrams(target, protein, fat);

        plan.setBasalMetabolicRate(bmr);
        plan.setMaintenanceCalories(maintenance);
        plan.setDailyCalories(target);
        plan.setProteinGrams(protein);
        plan.setFatGrams(fat);
        plan.setCarbGrams(carbs);
        plan.setHydrationLitres(hydrationLitres(plan.getWeightKg(), plan.getWeeklyTrainingMinutes()));
        plan.setSummary(summary(plan));
    }

    /** Mifflin-St Jeor: 10 x kg + 6.25 x cm - 5 x age + sex constant. */
    int basalMetabolicRate(double weightKg, int heightCm, int age, BiologicalSex sex) {
        double bmr = (10 * weightKg) + (6.25 * heightCm) - (5.0 * age) + sex.getBmrConstant();
        return (int) Math.round(bmr);
    }

    int maintenanceCalories(int bmr, ActivityLevel level, int weeklyTrainingMinutes) {
        double base = bmr * level.getMultiplier();
        double trainingBonus = (weeklyTrainingMinutes * KCAL_PER_TRAINING_MINUTE) / 7.0;
        return (int) Math.round(base + trainingBonus);
    }

    /**
     * Never prescribes less than 110% of resting energy, however aggressive the
     * cut multiplier would otherwise be.
     */
    int goalCalories(int maintenance, int bmr, NutritionGoal goal) {
        int target = (int) Math.round(maintenance * goal.getCalorieMultiplier());
        int floor = (int) Math.round(bmr * 1.10);
        return Math.max(target, floor);
    }

    int proteinGrams(double weightKg, NutritionGoal goal) {
        return (int) Math.round(weightKg * goal.getProteinPerKg());
    }

    int fatGrams(int dailyCalories, NutritionGoal goal) {
        return (int) Math.round((dailyCalories * goal.getFatShare()) / FAT_KCAL_PER_GRAM);
    }

    /** Whatever energy is left after protein and fat is carbohydrate, never negative. */
    int carbGrams(int dailyCalories, int proteinGrams, int fatGrams) {
        int remaining = dailyCalories
                - (proteinGrams * PROTEIN_KCAL_PER_GRAM)
                - (fatGrams * FAT_KCAL_PER_GRAM);
        return Math.max(0, (int) Math.round(remaining / (double) CARB_KCAL_PER_GRAM));
    }

    /** 33 ml per kilogram, plus 12 ml per daily training minute. */
    double hydrationLitres(double weightKg, int weeklyTrainingMinutes) {
        double litres = (weightKg * 0.033) + ((weeklyTrainingMinutes / 7.0) * 0.012);
        return Math.round(litres * 10) / 10.0;
    }

    private String summary(MealPlan plan) {
        String direction = switch (plan.getGoal()) {
            case CUT -> "a deficit against";
            case BULK -> "a surplus over";
            case MAINTAIN -> "level with";
        };
        return "%s kcal a day, %s an estimated %s kcal maintenance. Protein %sg, carbohydrate %sg, fat %sg, based on %s minutes of training in the last week."
                .formatted(plan.getDailyCalories(), direction, plan.getMaintenanceCalories(),
                        plan.getProteinGrams(), plan.getCarbGrams(), plan.getFatGrams(),
                        plan.getWeeklyTrainingMinutes());
    }
}
