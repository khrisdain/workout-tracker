package com.pulsetrack.nutrition.model;

/**
 * The constant term in the Mifflin-St Jeor equation. {@code UNSPECIFIED} uses
 * the midpoint between the two published constants so the field stays optional
 * for the caller.
 */
public enum BiologicalSex {

    FEMALE(-161),
    MALE(5),
    UNSPECIFIED(-78);

    private final int bmrConstant;

    BiologicalSex(int bmrConstant) {
        this.bmrConstant = bmrConstant;
    }

    public int getBmrConstant() {
        return bmrConstant;
    }
}
