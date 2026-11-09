package vn.codegym.clinicbook.service;

import vn.codegym.clinicbook.dto.ApiResponses.AuthResponse;
import vn.codegym.clinicbook.dto.LoginRequest;
import vn.codegym.clinicbook.dto.RegisterRequest;
import vn.codegym.clinicbook.entity.User;

public interface AuthService {
    User registerPatient(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    User getProfile(String username);

    void changePassword(String username, String currentPassword, String newPassword);
}
