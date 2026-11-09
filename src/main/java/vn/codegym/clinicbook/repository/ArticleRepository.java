package vn.codegym.clinicbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegym.clinicbook.entity.Article;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findAllByOrderByPublishedAtDescIdDesc();
}
