# Workout Tracker

A Spring Boot web application for logging and tracking workouts.

## Tech Stack

- Java 17
- Spring Boot 3.3.0 (Web, Thymeleaf, Data JPA, Validation)
- H2 in-memory database
- Maven

## Features

- Log workouts with name, type, duration, difficulty, calories burned, and date
- Workout types: `CARDIO`, `STRENGTH`, `FLEXIBILITY`, `HIIT`, `SPORTS`
- Difficulty levels: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- Input validation (required fields, duration 1–300 minutes, non-negative calories)

## Getting Started

### Prerequisites

- Java 17+
- Maven (or use the included wrapper, if present)

### Run the application

```bash
./mvnw spring-boot:run
```

The app starts on [http://localhost:8080](http://localhost:8080).

### H2 Database Console

While running, the H2 console is available at [http://localhost:8080/h2-console](http://localhost:8080/h2-console):

- JDBC URL: `jdbc:h2:mem:workoutdb`
- Username: `sa`
- Password: *(none)*

## Project Structure

```
src/main/java/com/fitnesstracker/workouttracker/
├── WorkoutTrackerApplication.java   # Application entry point
├── model/
│   ├── Workout.java                 # Workout entity
│   ├── WorkoutType.java             # Workout type enum
│   └── DifficultyLevel.java         # Difficulty level enum
└── repository/
    └── WorkoutRepository.java       # JPA repository for Workout
```
