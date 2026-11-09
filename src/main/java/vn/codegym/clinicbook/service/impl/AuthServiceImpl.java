package vn.codegym.clinicbook.service.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.ApiResponses.AuthResponse;
import vn.codegym.clinicbook.dto.LoginRequest;
import vn.codegym.clinicbook.dto.RegisterRequest;
import vn.codegym.clinicbook.entity.Patient;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.UserRepository;
import vn.codegym.clinicbook.security.JwtService;
import vn.codegym.clinicbook.constant.ErrorMessage;

import vn.codegym.clinicbook.service.AuthService;
import vn.codegym.clinicbook.service.AuditLogService;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(UserRepository userRepository,
                       PatientRepository patientRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @Override
    public User registerPatient(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorMessage.USERNAME_TAKEN);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(Role.PATIENT);
        user = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patientRepository.save(patient);
        return user;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String role = principal.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("");
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));
        return new AuthResponse(jwtService.generateToken(user.getId()), principal.getUsername(), role);
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public User getProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.USER_NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorMessage.WRONG_PASSWORD);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorMessage.PASSWORD_SAME_AS_OLD);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.record("PASSWORD_CHANGED", username, "password changed");
    }
}
