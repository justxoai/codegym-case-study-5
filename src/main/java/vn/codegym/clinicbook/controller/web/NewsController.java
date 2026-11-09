package vn.codegym.clinicbook.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.codegym.clinicbook.dto.ContentBlock;
import vn.codegym.clinicbook.entity.Article;
import vn.codegym.clinicbook.entity.enums.ArticleCategory;
import vn.codegym.clinicbook.service.ArticleService;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/news")
public class NewsController {
    private final ArticleService articleService;

    public NewsController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(required = false, defaultValue = "") String keyword,
                       Model model) {
        ArticleCategory selected = parseCategory(category);
        List<Article> articles = articleService.search(selected, keyword);
        model.addAttribute("articles", articles);
        model.addAttribute("categories", ArticleCategory.values());
        model.addAttribute("category", selected);
        model.addAttribute("keyword", keyword);
        return "news/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Article article = articleService.findById(id);
        model.addAttribute("article", article);
        model.addAttribute("blocks", ContentBlock.parse(article.getContent()));
        model.addAttribute("related", articleService.related(article, 3));
        return "news/detail";
    }

    private static ArticleCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(ArticleCategory.values())
                .filter(c -> c.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
