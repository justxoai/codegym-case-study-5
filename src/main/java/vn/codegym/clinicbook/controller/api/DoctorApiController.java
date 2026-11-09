package vn.codegym.clinicbook.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.codegym.clinicbook.dto.ApiResponses.DoctorResponse;
import vn.codegym.clinicbook.dto.ApiResponses.SlotResponse;
import vn.codegym.clinicbook.dto.DoctorRequest;
import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SlotService;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorApiController {
    private final DoctorService doctorService;
    private final SlotService slotService;

    public DoctorApiController(DoctorService doctorService, SlotService slotService) {
        this.doctorService = doctorService;
        this.slotService = slotService;
    }

    @GetMapping
    public List<DoctorResponse> list(@RequestParam(required = false) Long specialtyId,
                                     @RequestParam(required = false) String keyword) {
        return doctorService.findAll(specialtyId, keyword).stream().map(DoctorResponse::from).toList();
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable Long id) {
        return DoctorResponse.from(doctorService.findById(id));
    }

    @GetMapping("/{id}/slots")
    public List<SlotResponse> availableSlots(@PathVariable Long id) {
        doctorService.findById(id);
        return slotService.findAvailable(id).stream().map(SlotResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorResponse create(@Valid @RequestBody DoctorRequest request) {
        return DoctorResponse.from(doctorService.create(request));
    }

    @PutMapping("/{id}")
    public DoctorResponse update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return DoctorResponse.from(doctorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        doctorService.delete(id);
    }
}
