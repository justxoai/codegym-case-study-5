package vn.codegym.clinicbook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import vn.codegym.clinicbook.entity.enums.ArticleCategory;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "articles")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, length = 8000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleCategory category;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(nullable = false)
    private LocalDate publishedAt;

    @Column(nullable = false)
    private int readMinutes;
}
