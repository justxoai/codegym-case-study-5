package vn.codegym.clinicbook.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PageView<T> {
    public static final int DEFAULT_SIZE = 10;

    private final List<T> content;
    private final long total;
    private final int page;
    private final int size;
    private String path = "";
    private String query = "";

    private PageView(List<T> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageView<T> of(List<T> all, int page, int size) {
        int current = clamp(page, all.size(), size);
        int from = Math.min((current - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageView<>(List.copyOf(all.subList(from, to)), all.size(), current, size);
    }

    public static <T> PageView<T> ofSlice(List<T> content, long total, int page, int size) {
        return new PageView<>(content, total, page, size);
    }

    public static int clamp(int requested, long total, int size) {
        int last = (int) Math.max(1, (total + size - 1) / size);
        return Math.min(Math.max(requested, 1), last);
    }

    public PageView<T> link(String path, Map<String, ?> filters) {
        this.path = path;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> e : filters.entrySet()) {
            Object value = e.getValue();
            if (value == null || value.toString().isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=').append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
        }
        this.query = sb.toString();
        return this;
    }

    public String url(int p) {
        return path + "?" + query + (query.isEmpty() ? "" : "&") + "page=" + p;
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return (int) Math.max(1, (total + size - 1) / size);
    }

    public boolean isFirst() {
        return page <= 1;
    }

    public boolean isLast() {
        return page >= getTotalPages();
    }

    public long getFrom() {
        return total == 0 ? 0 : (long) (page - 1) * size + 1;
    }

    public long getTo() {
        return total == 0 ? 0 : (long) (page - 1) * size + content.size();
    }

    public List<Integer> getPages() {
        int last = getTotalPages();
        List<Integer> result = new ArrayList<>();
        int previous = 0;
        for (int p = 1; p <= last; p++) {
            boolean show = p == 1 || p == last || Math.abs(p - page) <= 1 || (last <= 7);
            if (!show) {
                continue;
            }
            if (previous != 0 && p - previous > 1) {
                result.add(0);
            }
            result.add(p);
            previous = p;
        }
        return result;
    }
}
