package vn.codegym.clinicbook.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DoctorRequest {
    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    private String fullName;

    @NotNull(message = "{validation.required}")
    private Long specialtyId;

    @NotNull(message = "{validation.required}")
    @Min(value = 0, message = "{validation.min}")
    private Integer yearsExperience;

    @Size(max = 1000, message = "{validation.size}")
    private String bio;

    private String username;
    private String password;
}
