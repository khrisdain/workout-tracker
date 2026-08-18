package com.fitnesstracker.workouttracker.service;

import com.fitnesstracker.workouttracker.dto.WorkoutFilter;
import com.fitnesstracker.workouttracker.model.DifficultyLevel;
import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import com.fitnesstracker.workouttracker.model.Workout;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WorkoutServiceTest {

    @Autowired
    private WorkoutService workouts;

    private User member;
    private User coach;

    @BeforeEach
    void setUp() {
        member = new User("test.member", "member@test.local", "Test Member", "hash",
                EnumSet.of(Role.MEMBER));
        member.setId(9001L);
        member.setWeightKg(80.0);

        coach = new User("test.coach", "coach@test.local", "Test Coach", "hash",
                EnumSet.of(Role.COACH, Role.MEMBER));
        coach.setId(9002L);
    }

    @Nested
    @DisplayName("calorie estimation")
    class CalorieEstimation {

        @Test
        @DisplayName("fills in calories from MET, bodyweight and intensity when the field is blank")
        void estimatesWhenBlank() {
            Workout workout = new Workout("Zone 2 ride", WorkoutType.CARDIO, 60,
                    DifficultyLevel.INTERMEDIATE, null, LocalDate.now());

            workouts.applyCalorieEstimate(workout, member);

            // 7.0 MET x 80 kg x 1.0 h x 1.0 intensity = 560 kcal
            assertThat(workout.getCaloriesBurned()).isEqualTo(560);
        }

        @Test
        @DisplayName("scales the estimate by the difficulty intensity factor")
        void scalesByDifficulty() {
            Workout easy = new Workout("Easy", WorkoutType.CARDIO, 60, DifficultyLevel.BEGINNER, null, LocalDate.now());
            Workout hard = new Workout("Hard", WorkoutType.CARDIO, 60, DifficultyLevel.ADVANCED, null, LocalDate.now());

            workouts.applyCalorieEstimate(easy, member);
            workouts.applyCalorieEstimate(hard, member);

            assertThat(hard.getCaloriesBurned()).isGreaterThan(easy.getCaloriesBurned());
        }

        @Test
        @DisplayName("falls back to a 70 kg reference when the member has no saved weight")
        void fallsBackWithoutWeight() {
            User anonymous = new User("anon", "a@test.local", "Anon", "hash", EnumSet.of(Role.MEMBER));
            Workout workout = new Workout("Ride", WorkoutType.CARDIO, 60,
                    DifficultyLevel.INTERMEDIATE, null, LocalDate.now());

            workouts.applyCalorieEstimate(workout, anonymous);

            assertThat(workout.getCaloriesBurned()).isEqualTo(490);
        }

        @Test
        @DisplayName("never overwrites a figure the member typed themselves")
        void keepsUserSuppliedValue() {
            Workout workout = new Workout("Ride", WorkoutType.CARDIO, 60,
                    DifficultyLevel.INTERMEDIATE, 123, LocalDate.now());

            workouts.applyCalorieEstimate(workout, member);

            assertThat(workout.getCaloriesBurned()).isEqualTo(123);
        }
    }

    @Nested
    @DisplayName("authorisation")
    class Authorisation {

        @Test
        @DisplayName("a member may modify only their own session")
        void memberOwnsTheirRows() {
            Workout mine = new Workout();
            mine.setOwner(member);
            Workout theirs = new Workout();
            theirs.setOwner(coach);

            assertThat(workouts.canModify(mine, member)).isTrue();
            assertThat(workouts.canModify(theirs, member)).isFalse();
        }

        @Test
        @DisplayName("a coach may modify any session")
        void coachesModifyAnything() {
            Workout mine = new Workout();
            mine.setOwner(member);

            assertThat(workouts.canModify(mine, coach)).isTrue();
        }

        @Test
        @DisplayName("an anonymous visitor may modify nothing")
        void anonymousModifiesNothing() {
            assertThat(workouts.canModify(new Workout(), null)).isFalse();
        }
    }

    @Nested
    @DisplayName("search, filter and sort")
    class Searching {

        @Test
        @DisplayName("returns a page sized by the filter")
        void pagesResults() {
            WorkoutFilter filter = new WorkoutFilter();
            filter.setSize(5);

            Page<Workout> page = workouts.search(filter, null);

            assertThat(page.getContent()).hasSizeLessThanOrEqualTo(5);
            assertThat(page.getTotalElements()).isPositive();
        }

        @Test
        @DisplayName("filters on type and difficulty together")
        void filtersOnTwoAttributes() {
            WorkoutFilter filter = new WorkoutFilter();
            filter.setSize(100);
            filter.setType(WorkoutType.STRENGTH);
            filter.setDifficulty(DifficultyLevel.ADVANCED);

            Page<Workout> page = workouts.search(filter, null);

            assertThat(page.getContent()).allSatisfy(workout -> {
                assertThat(workout.getType()).isEqualTo(WorkoutType.STRENGTH);
                assertThat(workout.getDifficulty()).isEqualTo(DifficultyLevel.ADVANCED);
            });
        }

        @Test
        @DisplayName("sorts by duration ascending when asked")
        void sortsAscending() {
            WorkoutFilter filter = new WorkoutFilter();
            filter.setSize(20);
            filter.setSort("durationMinutes");
            filter.setDirection("asc");

            Page<Workout> page = workouts.search(filter, null);

            assertThat(page.getContent())
                    .extracting(Workout::getDurationMinutes)
                    .isSortedAccordingTo(Integer::compareTo);
        }

        @Test
        @DisplayName("ignores an unknown sort field instead of failing")
        void rejectsUnknownSortField() {
            WorkoutFilter filter = new WorkoutFilter();
            filter.setSort("; drop table workouts");

            assertThat(filter.getSafeSort()).isEqualTo("dateLogged");
            assertThat(workouts.search(filter, null).getTotalElements()).isPositive();
        }

        @Test
        @DisplayName("narrows results to a single owner")
        void scopesToOwner() {
            WorkoutFilter filter = new WorkoutFilter();
            filter.setSize(100);

            Page<Workout> page = workouts.search(filter, "jordan");

            assertThat(page.getContent()).allSatisfy(workout ->
                    assertThat(workout.getOwner().getUsername()).isEqualTo("jordan"));
        }
    }
}
