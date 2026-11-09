package vn.codegym.clinicbook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "{validation.required}")
    @Size(min = 4, max = 50, message = "{validation.size}")
    private String username;

    @NotBlank(message = "{validation.required}")
    @Size(min = 6, max = 100, message = "{validation.size}")
    private String password;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    private String fullName;

    @Email(message = "{validation.email}")
    private String email;

    @Size(max = 20, message = "{validation.size}")
    private String phone;
}
