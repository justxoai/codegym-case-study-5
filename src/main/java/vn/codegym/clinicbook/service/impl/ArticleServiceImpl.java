package vn.codegym.clinicbook.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.Article;
import vn.codegym.clinicbook.entity.enums.ArticleCategory;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.repository.ArticleRepository;
import java.util.Locale;
import java.util.List;
import vn.codegym.clinicbook.constant.ErrorMessage;

import vn.codegym.clinicbook.service.ArticleService;

@Service
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {
    private final ArticleRepository articleRepository;

    public ArticleServiceImpl(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public List<Article> search(ArticleCategory category, String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return articleRepository.findAllByOrderByPublishedAtDescIdDesc().stream()
                .filter(a -> category == null || a.getCategory() == category)
                .filter(a -> kw.isEmpty()
                        || a.getTitle().toLowerCase(Locale.ROOT).contains(kw)
                        || a.getSummary().toLowerCase(Locale.ROOT).contains(kw))
                .toList();
    }

    @Override
    public List<Article> latest(int limit) {
        return articleRepository.findAllByOrderByPublishedAtDescIdDesc().stream().limit(limit).toList();
    }

    @Override
    public Article findById(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ARTICLE_NOT_FOUND));
    }

    @Override
    public List<Article> related(Article article, int limit) {
        return articleRepository.findAllByOrderByPublishedAtDescIdDesc().stream()
                .filter(a -> !a.getId().equals(article.getId()))
                .sorted((a, b) -> Boolean.compare(b.getCategory() == article.getCategory(),
                        a.getCategory() == article.getCategory()))
                .limit(limit)
                .toList();
    }
}
