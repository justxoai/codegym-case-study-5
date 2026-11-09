package vn.codegym.clinicbook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;

@Getter
@Setter
@NoArgsConstructor
public class StatusRequest {
    @NotNull
    private AppointmentStatus status;
}
