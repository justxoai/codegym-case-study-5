package vn.codegym.clinicbook.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.codegym.clinicbook.dto.ApiResponses.AppointmentResponse;
import vn.codegym.clinicbook.dto.BookingRequest;
import vn.codegym.clinicbook.dto.StatusRequest;
import vn.codegym.clinicbook.service.AppointmentService;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {
    private final AppointmentService appointmentService;

    public AppointmentApiController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@Valid @RequestBody BookingRequest request) {
        return AppointmentResponse.from(appointmentService.book(request.getSlotId(), request.getReason()));
    }

    @GetMapping
    public List<AppointmentResponse> list() {
        return appointmentService.findForCurrentUser().stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return AppointmentResponse.from(appointmentService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return AppointmentResponse.from(appointmentService.updateStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return AppointmentResponse.from(appointmentService.cancel(id));
    }
}
