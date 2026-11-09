package vn.codegym.clinicbook.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookingRequest {
    @NotNull(message = "{validation.required}")
    private Long slotId;

    @Size(max = 500, message = "{validation.size}")
    private String reason;
}
