package vn.codegym.clinicbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {
    @NotBlank(message = "{validation.required}")
    private String currentPassword;

    @NotBlank(message = "{validation.required}")
    @Size(min = 6, max = 100, message = "{validation.size}")
    private String newPassword;

    @NotBlank(message = "{validation.required}")
    private String confirmPassword;
}
