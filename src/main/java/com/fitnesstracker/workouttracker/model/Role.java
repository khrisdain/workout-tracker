package com.fitnesstracker.workouttracker.model;

/**
 * The three roles PulseTrack understands. Spring Security authorities are the
 * constant name prefixed with {@code ROLE_}; the label and description are what
 * the admin console renders.
 */
public enum Role {

    MEMBER("Member", "secondary",
            "Logs their own sessions, follows programs and requests nutrition plans."),
    COACH("Coach", "info",
            "Everything a member can do, plus building programs and reviewing the whole roster."),
    ADMIN("Administrator", "dark",
            "Full control of users, programs, every logged session and the nutrition service.");

    private final String label;
    private final String colour;
    private final String description;

    Role(String label, String colour, String description) {
        this.label = label;
        this.colour = colour;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getColour() {
        return colour;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
