package vn.codegym.clinicbook.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public class ContentBlock {
    private static final Pattern NUMBERED = Pattern.compile("^\\d+\\.\\s+.*");

    private final String kind;
    private final String text;
    private final List<String> items;

    private ContentBlock(String kind, String text, List<String> items) {
        this.kind = kind;
        this.text = text;
        this.items = items;
    }

    public static List<ContentBlock> parse(String content) {
        List<ContentBlock> blocks = new ArrayList<>();
        for (String raw : content.split("\\R\\s*\\R")) {
            String block = raw.strip();
            if (block.isEmpty()) {
                continue;
            }
            String[] lines = block.split("\\R");
            if (block.startsWith("## ")) {
                blocks.add(new ContentBlock("h", block.substring(3).strip(), List.of()));
            } else if (allMatch(lines, l -> l.startsWith("- "))) {
                blocks.add(new ContentBlock("ul", null, strip(lines, 2)));
            } else if (allMatch(lines, l -> NUMBERED.matcher(l).matches())) {
                blocks.add(new ContentBlock("ol", null, strip(lines, -1)));
            } else {
                blocks.add(new ContentBlock("p", String.join(" ", lines), List.of()));
            }
        }
        return blocks;
    }

    private static boolean allMatch(String[] lines, java.util.function.Predicate<String> test) {
        for (String line : lines) {
            if (!test.test(line.strip())) {
                return false;
            }
        }
        return true;
    }

    private static List<String> strip(String[] lines, int marker) {
        List<String> items = new ArrayList<>();
        for (String line : lines) {
            String l = line.strip();
            items.add(marker >= 0 ? l.substring(marker) : l.replaceFirst("^\\d+\\.\\s+", ""));
        }
        return items;
    }
}
