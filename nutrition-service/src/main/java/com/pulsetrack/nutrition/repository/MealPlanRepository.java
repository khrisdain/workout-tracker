package com.pulsetrack.nutrition.repository;

import com.pulsetrack.nutrition.model.MealPlan;
import com.pulsetrack.nutrition.model.NutritionGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    List<MealPlan> findAllByOrderByCreatedAtDescIdDesc();

    List<MealPlan> findByMemberUsernameIgnoreCaseOrderByCreatedAtDescIdDesc(String memberUsername);

    /**
     * Backs the custom multi parameter endpoint. Every parameter is optional; a
     * null simply drops that predicate, which keeps one query serving all
     * sixteen filter combinations.
     */
    @Query("""
            select p from MealPlan p
            where (:member is null or lower(p.memberUsername) = lower(:member))
              and (:goal is null or p.goal = :goal)
              and (:minCalories is null or p.dailyCalories >= :minCalories)
              and (:maxCalories is null or p.dailyCalories <= :maxCalories)
            order by p.createdAt desc, p.id desc
            """)
    List<MealPlan> search(@Param("member") String member,
                          @Param("goal") NutritionGoal goal,
                          @Param("minCalories") Integer minCalories,
                          @Param("maxCalories") Integer maxCalories);

    @Query("select count(distinct p.memberUsername) from MealPlan p")
    long countDistinctMembers();

    @Query("select coalesce(avg(p.dailyCalories), 0) from MealPlan p")
    double averageDailyCalories();

    @Query("select coalesce(avg(p.proteinGrams), 0) from MealPlan p")
    double averageProteinGrams();

    @Query("select p.goal, count(p) from MealPlan p group by p.goal order by p.goal")
    List<Object[]> countByGoal();
}
