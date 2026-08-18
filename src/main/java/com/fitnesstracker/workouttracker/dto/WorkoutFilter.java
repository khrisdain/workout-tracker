package com.fitnesstracker.workouttracker.dto;

import com.fitnesstracker.workouttracker.model.DifficultyLevel;
import com.fitnesstracker.workouttracker.model.WorkoutType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backs the filter/sort toolbar on the workout list. Bound with
 * {@code @ModelAttribute} so the form redisplays whatever the user selected, and
 * handed to the service layer to build the JPA specification and {@code Sort}.
 */
public class WorkoutFilter {

    /** Whitelisted sort fields; anything else falls back to the date. */
    public static final Map<String, String> SORTABLE_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_FIELDS.put("dateLogged", "Date");
        SORTABLE_FIELDS.put("name", "Name");
        SORTABLE_FIELDS.put("durationMinutes", "Duration");
        SORTABLE_FIELDS.put("caloriesBurned", "Calories");
        SORTABLE_FIELDS.put("difficulty", "Difficulty");
    }

    private String q;
    private WorkoutType type;
    private DifficultyLevel difficulty;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private Integer minDuration;

    private String sort = "dateLogged";
    private String direction = "desc";
    private int page = 0;
    private int size = 8;

    public boolean isActive() {
        return (q != null && !q.isBlank()) || type != null || difficulty != null
                || from != null || to != null || minDuration != null;
    }

    public String getSafeSort() {
        return SORTABLE_FIELDS.containsKey(sort) ? sort : "dateLogged";
    }

    public boolean isAscending() {
        return "asc".equalsIgnoreCase(direction);
    }

    /** Query string carrying every filter except {@code page}, for pager links. */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        append(sb, "q", q);
        append(sb, "type", type);
        append(sb, "difficulty", difficulty);
        append(sb, "from", from);
        append(sb, "to", to);
        append(sb, "minDuration", minDuration);
        append(sb, "sort", getSafeSort());
        append(sb, "direction", direction);
        append(sb, "size", size);
        return sb.toString();
    }

    /** Same query string but with the sort flipped, used by the column headers. */
    public String toSortLink(String field) {
        String nextDirection = field.equals(getSafeSort()) && !isAscending() ? "asc" : "desc";
        StringBuilder sb = new StringBuilder();
        append(sb, "q", q);
        append(sb, "type", type);
        append(sb, "difficulty", difficulty);
        append(sb, "from", from);
        append(sb, "to", to);
        append(sb, "minDuration", minDuration);
        append(sb, "sort", field);
        append(sb, "direction", nextDirection);
        append(sb, "size", size);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String key, Object value) {
        if (value == null || value.toString().isBlank()) {
            return;
        }
        sb.append('&').append(key).append('=').append(java.net.URLEncoder
                .encode(value.toString(), java.nio.charset.StandardCharsets.UTF_8));
    }

    // ---- getters / setters ----

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public WorkoutType getType() {
        return type;
    }

    public void setType(WorkoutType type) {
        this.type = type;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public Integer getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(Integer minDuration) {
        this.minDuration = minDuration;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 0);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = (size < 1 || size > 100) ? 8 : size;
    }
}
