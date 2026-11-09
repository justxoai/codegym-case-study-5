package vn.codegym.clinicbook.entity;

import jakarta.persistence.*;
import vn.codegym.clinicbook.util.DoctorNames;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @Column(length = 1000)
    private String bio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public String getDisplayName() {
        return DoctorNames.display(fullName);
    }
}
