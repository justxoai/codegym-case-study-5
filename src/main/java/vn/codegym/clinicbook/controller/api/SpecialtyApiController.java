package vn.codegym.clinicbook.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.codegym.clinicbook.dto.ApiResponses.SpecialtyResponse;
import vn.codegym.clinicbook.dto.SpecialtyRequest;
import vn.codegym.clinicbook.service.SpecialtyService;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyApiController {
    private final SpecialtyService specialtyService;

    public SpecialtyApiController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public List<SpecialtyResponse> list() {
        return specialtyService.findAll().stream().map(SpecialtyResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SpecialtyResponse get(@PathVariable Long id) {
        return SpecialtyResponse.from(specialtyService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyResponse create(@Valid @RequestBody SpecialtyRequest request) {
        return SpecialtyResponse.from(specialtyService.create(request));
    }

    @PutMapping("/{id}")
    public SpecialtyResponse update(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
        return SpecialtyResponse.from(specialtyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        specialtyService.delete(id);
    }
}
