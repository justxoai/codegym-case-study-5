package vn.codegym.clinicbook.service;

import vn.codegym.clinicbook.entity.Article;
import vn.codegym.clinicbook.entity.enums.ArticleCategory;
import java.util.List;

public interface ArticleService {
    List<Article> search(ArticleCategory category, String keyword);

    List<Article> latest(int limit);

    Article findById(Long id);

    List<Article> related(Article article, int limit);
}
