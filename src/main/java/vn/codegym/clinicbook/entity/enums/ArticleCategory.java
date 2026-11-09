package vn.codegym.clinicbook.entity.enums;

public enum ArticleCategory {
    HEART("bi-heart-pulse"),
    MENTAL("bi-moon-stars"),
    CHILD("bi-emoji-smile"),
    SKIN("bi-sun"),
    NUTRITION("bi-basket"),
    PREVENTION("bi-shield-check");

    private final String icon;

    ArticleCategory(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }
}
