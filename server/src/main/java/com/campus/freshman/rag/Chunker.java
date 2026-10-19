package com.campus.freshman.rag;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class Chunker {
    public List<Piece> split(String text, int size, int overlap, boolean paged) {
        if (size < 200 || size > 4000 || overlap < 0 || overlap >= size / 2) throw new IllegalArgumentException("切片长度需200—4000字符，重叠需小于长度的一半");
        List<Piece> result = new ArrayList<>(); String section = "正文";
        String[] pages = text.split("\f", -1);
        for (int page = 0; page < pages.length; page++) {
            String content = pages[page].trim(); int start = 0;
            while (start < content.length()) {
                int end = Math.min(content.length(), start + size);
                if (end < content.length()) {
                    int boundary = content.lastIndexOf("\n\n", end);
                    if (boundary > start + size / 2) end = boundary;
                    else { boundary = content.lastIndexOf('。', end-1); if (boundary > start + size / 2) end = boundary+1; }
                    if (end>start && Character.isHighSurrogate(content.charAt(end-1))) end--;
                }
                String part = content.substring(start, end).trim();
                if (!part.isBlank()) {
                    for (String line : part.split("\n")) if (line.matches("#{1,6} .*")) section = line.replaceFirst("^#+\\s*", "");
                    result.add(new Piece(part, paged ? page+1 : null, section));
                }
                if (end>=content.length()) break;
                int next = Math.max(start+1, end-overlap);
                if (next<content.length() && Character.isLowSurrogate(content.charAt(next))) next++;
                start = next;
            }
        }
        if (result.size()>5000) throw new IllegalArgumentException("切片超过5000条，请拆分文档");
        return result;
    }
    public record Piece(String content, Integer page, String section) {}
}
