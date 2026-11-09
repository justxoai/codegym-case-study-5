package vn.codegym.clinicbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SpecialtyRequest {
    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    private String name;

    @Size(max = 100, message = "{validation.size}")
    private String nameEn;

    @Size(max = 500, message = "{validation.size}")
    private String description;
}
