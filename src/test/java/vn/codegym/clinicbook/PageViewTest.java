package vn.codegym.clinicbook;

import org.junit.jupiter.api.Test;
import vn.codegym.clinicbook.dto.PageView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageViewTest {
    private static List<Integer> numbers(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    @Test
    void cutsTenRowsPerPage() {
        PageView<Integer> first = PageView.of(numbers(25), 1, PageView.DEFAULT_SIZE);
        assertEquals(10, first.getContent().size());
        assertEquals(3, first.getTotalPages());
        assertEquals(1, first.getFrom());
        assertEquals(10, first.getTo());
        assertTrue(first.isFirst());
        assertFalse(first.isLast());

        PageView<Integer> last = PageView.of(numbers(25), 3, PageView.DEFAULT_SIZE);
        assertEquals(5, last.getContent().size());
        assertEquals(21, last.getFrom());
        assertEquals(25, last.getTo());
        assertTrue(last.isLast());
    }

    @Test
    void pageNumberOutOfRangeIsClamped() {
        assertEquals(1, PageView.of(numbers(25), 0, 10).getPage());
        assertEquals(1, PageView.of(numbers(25), -4, 10).getPage());
        assertEquals(3, PageView.of(numbers(25), 99, 10).getPage());
        assertEquals(1, PageView.of(List.<Integer>of(), 5, 10).getPage());
    }

    @Test
    void emptyListHasOneEmptyPage() {
        PageView<Integer> empty = PageView.of(List.of(), 1, 10);
        assertEquals(0, empty.getTotal());
        assertEquals(1, empty.getTotalPages());
        assertEquals(0, empty.getFrom());
        assertEquals(0, empty.getTo());
        assertTrue(empty.isFirst());
        assertTrue(empty.isLast());
    }

    @Test
    void exactMultipleOfTheSizeHasNoExtraPage() {
        assertEquals(2, PageView.of(numbers(20), 1, 10).getTotalPages());
        assertEquals(3, PageView.of(numbers(21), 1, 10).getTotalPages());
    }

    @Test
    void linksKeepTheFiltersAndSkipBlankOnes() {
        Map<String, Object> keep = new LinkedHashMap<>();
        keep.put("keyword", "nguyen an");
        keep.put("specialty", 3L);
        keep.put("status", null);
        keep.put("date", "");
        PageView<Integer> pv = PageView.of(numbers(25), 1, 10).link("/admin/doctors", keep);

        assertEquals("/admin/doctors?keyword=nguyen+an&specialty=3&page=2", pv.url(2));
    }

    @Test
    void linkWithoutFiltersHasOnlyThePage() {
        PageView<Integer> pv = PageView.of(numbers(25), 1, 10).link("/admin/audit", new LinkedHashMap<>());
        assertEquals("/admin/audit?page=3", pv.url(3));
    }

    @Test
    void longListsShowGapsInsteadOfEveryPage() {
        List<Integer> pages = PageView.of(numbers(200), 10, 10).getPages();
        assertEquals(List.of(1, 0, 9, 10, 11, 0, 20), pages);

        assertEquals(List.of(1, 2, 3), PageView.of(numbers(25), 2, 10).getPages());
    }

    @Test
    void databaseSlicesKeepTheirTotal() {
        PageView<Integer> pv = PageView.ofSlice(List.of(11, 12, 13), 23, 3, 10);
        assertEquals(3, pv.getTotalPages());
        assertEquals(21, pv.getFrom());
        assertEquals(23, pv.getTo());
    }
}
