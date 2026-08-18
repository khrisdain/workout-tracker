package com.fitnesstracker.workouttracker.dto;

import com.fitnesstracker.workouttracker.model.BiologicalSex;
import com.fitnesstracker.workouttracker.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable slice of a member's own account, including the body metrics the nutrition service consumes. */
public class ProfileForm {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @Min(value = 100, message = "Height must be at least 100 cm")
    @Max(value = 250, message = "Height cannot exceed 250 cm")
    private Integer heightCm;

    @jakarta.validation.constraints.DecimalMin(value = "30.0", message = "Weight must be at least 30 kg")
    @jakarta.validation.constraints.DecimalMax(value = "300.0", message = "Weight cannot exceed 300 kg")
    private Double weightKg;

    @Min(value = 13, message = "You must be at least 13 to use PulseTrack")
    @Max(value = 100, message = "Age cannot exceed 100")
    private Integer age;

    private BiologicalSex sex = BiologicalSex.UNSPECIFIED;

    public static ProfileForm from(User user) {
        ProfileForm form = new ProfileForm();
        form.fullName = user.getFullName();
        form.email = user.getEmail();
        form.heightCm = user.getHeightCm();
        form.weightKg = user.getWeightKg();
        form.age = user.getAge();
        form.sex = user.getSex() == null ? BiologicalSex.UNSPECIFIED : user.getSex();
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public BiologicalSex getSex() {
        return sex;
    }

    public void setSex(BiologicalSex sex) {
        this.sex = sex;
    }
}
