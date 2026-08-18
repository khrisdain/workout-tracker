package com.fitnesstracker.workouttracker.repository;

import com.fitnesstracker.workouttracker.model.Program;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    Page<Program> findByPublishedTrue(Pageable pageable);

    List<Program> findByPublishedTrueOrderByCreatedAtDesc();

    List<Program> findByFocusAndPublishedTrue(WorkoutType focus);

    List<Program> findByCoachUsernameOrderByCreatedAtDesc(String username);

    @Query("select p from Program p join p.members m where m.username = :username order by p.name")
    List<Program> findEnrolled(@Param("username") String username);

    @Query("select count(p) from Program p join p.members m where m.username = :username")
    long countEnrolled(@Param("username") String username);
}
