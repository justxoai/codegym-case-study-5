package vn.codegym.clinicbook.entity;

import jakarta.persistence.*;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "specialties")
public class Specialty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 500)
    private String description;

    public String getLocalizedName() {
        boolean english = "en".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage());
        return english && nameEn != null && !nameEn.isBlank() ? nameEn : name;
    }
}
