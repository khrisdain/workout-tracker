package com.fitnesstracker.workouttracker.repository;

import com.fitnesstracker.workouttracker.model.Workout;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Paging, sorting and filtering all happen in the database. The dynamic filter
 * combinations used by the list view are built by
 * {@link com.fitnesstracker.workouttracker.repository.WorkoutSpecifications}
 * and executed through {@link JpaSpecificationExecutor}.
 */
@Repository
public interface WorkoutRepository extends JpaRepository<Workout, Long>, JpaSpecificationExecutor<Workout> {

    List<Workout> findTop5ByOrderByDateLoggedDescIdDesc();

    List<Workout> findTop5ByOwnerUsernameOrderByDateLoggedDescIdDesc(String username);

    long countByOwnerUsername(String username);

    @Query("select coalesce(sum(w.durationMinutes), 0) from Workout w")
    long totalMinutes();

    @Query("select coalesce(sum(w.caloriesBurned), 0) from Workout w")
    long totalCalories();

    @Query("select coalesce(sum(w.durationMinutes), 0) from Workout w where w.owner.username = :username")
    long totalMinutesForUser(@Param("username") String username);

    @Query("select coalesce(sum(w.caloriesBurned), 0) from Workout w where w.owner.username = :username")
    long totalCaloriesForUser(@Param("username") String username);

    @Query("""
            select coalesce(sum(w.durationMinutes), 0) from Workout w
            where w.owner.username = :username and w.dateLogged >= :from
            """)
    long minutesForUserSince(@Param("username") String username, @Param("from") LocalDate from);

    /** [type, sessions, minutes, calories] grouped for the dashboard charts. */
    @Query("""
            select w.type, count(w), coalesce(sum(w.durationMinutes), 0), coalesce(sum(w.caloriesBurned), 0)
            from Workout w
            group by w.type
            order by count(w) desc
            """)
    List<Object[]> aggregateByType();

    @Query("""
            select w.type, count(w), coalesce(sum(w.durationMinutes), 0), coalesce(sum(w.caloriesBurned), 0)
            from Workout w
            where w.owner.username = :username
            group by w.type
            order by count(w) desc
            """)
    List<Object[]> aggregateByTypeForUser(@Param("username") String username);

    /** [date, minutes] for the trend chart, newest sessions last. */
    @Query("""
            select w.dateLogged, coalesce(sum(w.durationMinutes), 0)
            from Workout w
            where w.dateLogged >= :from
            group by w.dateLogged
            order by w.dateLogged
            """)
    List<Object[]> dailyMinutesSince(@Param("from") LocalDate from);

    @Query("""
            select w.dateLogged, coalesce(sum(w.durationMinutes), 0)
            from Workout w
            where w.owner.username = :username and w.dateLogged >= :from
            group by w.dateLogged
            order by w.dateLogged
            """)
    List<Object[]> dailyMinutesForUserSince(@Param("username") String username, @Param("from") LocalDate from);

    long countByType(WorkoutType type);
}
