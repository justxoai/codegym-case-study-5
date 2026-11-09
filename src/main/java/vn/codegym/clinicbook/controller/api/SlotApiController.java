package vn.codegym.clinicbook.controller.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.codegym.clinicbook.dto.ApiResponses.SlotResponse;
import vn.codegym.clinicbook.dto.SlotRequest;
import vn.codegym.clinicbook.service.SlotService;

@RestController
@RequestMapping("/api/slots")
public class SlotApiController {
    private final SlotService slotService;

    public SlotApiController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse create(@Valid @RequestBody SlotRequest request) {
        return SlotResponse.from(slotService.create(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        slotService.delete(id);
    }
}
