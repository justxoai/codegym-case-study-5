package vn.codegym.clinicbook.service;

import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import java.util.List;

public interface AppointmentService {
    Appointment book(Long slotId, String reason);

    Appointment cancel(Long id);

    Appointment updateStatus(Long id, AppointmentStatus newStatus);

    Appointment findById(Long id);

    List<Appointment> findForCurrentUser();

    List<Appointment> findAll();
}
