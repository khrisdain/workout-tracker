package com.fitnesstracker.workouttracker.repository;

import com.fitnesstracker.workouttracker.model.Role;
import com.fitnesstracker.workouttracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    @Query("select u from User u join u.roles r where r = :role order by u.fullName")
    List<User> findByRole(@Param("role") Role role);

    @Query("select count(u) from User u join u.roles r where r = :role")
    long countByRole(@Param("role") Role role);
}
