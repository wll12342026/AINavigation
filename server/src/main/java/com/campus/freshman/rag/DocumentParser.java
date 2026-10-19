package com.campus.freshman.rag;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

@Component
public class DocumentParser {
    public static final int MAX_TEXT = 1_000_000;
    public Parsed parse(Path file, String format) throws Exception {
        String text;
        String warning = null;
        if ("PDF".equals(format)) {
            byte[] signature = new byte[5];
            try (var stream = Files.newInputStream(file)) { if (stream.read(signature) != 5 || !new String(signature, StandardCharsets.US_ASCII).equals("%PDF-")) throw new IllegalArgumentException("文件内容不是有效 PDF"); }
            try (var pdf = Loader.loadPDF(file.toFile())) {
                if (pdf.isEncrypted()) throw new IllegalArgumentException("加密PDF暂不支持，请提供未加密文件");
                if (pdf.getNumberOfPages() > 300) throw new IllegalArgumentException("单份PDF最多300页，请拆分后上传");
                StringBuilder result = new StringBuilder();
                PDFTextStripper stripper = new PDFTextStripper(); stripper.setSortByPosition(true);
                for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                    stripper.setStartPage(i); stripper.setEndPage(i);
                    if (i > 1) result.append('\f');
                    result.append(stripper.getText(pdf));
                    if (result.length() > MAX_TEXT) throw new IllegalArgumentException("提取正文过长，请拆分文档");
                }
                text = result.toString(); warning = "仅提取文字；图片、扫描页与复杂表格需人工检查，未执行OCR";
            }
        } else if ("DOCX".equals(format)) {
            validateOffice(file);
            try (var stream = Files.newInputStream(file); var doc = new XWPFDocument(stream)) {
                StringBuilder result = new StringBuilder();
                for (var element : doc.getBodyElements()) {
                    if (element instanceof XWPFParagraph paragraph) result.append(paragraph.getText()).append("\n\n");
                    if (element instanceof XWPFTable table) {
                        String headers = table.getRows().isEmpty() ? "" : table.getRows().getFirst().getTableCells().stream().map(XWPFTableCell::getText).reduce((a,b) -> a + " | " + b).orElse("");
                        result.append("表格列：").append(headers).append('\n');
                        for (var row : table.getRows()) result.append(row.getTableCells().stream().map(XWPFTableCell::getText).reduce((a,b) -> a + " | " + b).orElse("")).append('\n');
                        result.append('\n');
                    }
                    if (result.length() > MAX_TEXT) throw new IllegalArgumentException("提取正文过长，请拆分文档");
                }
                text = result.toString(); warning = "保留段落与表格文字；图片未提取，Word页码未推断";
            }
        } else {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(Files.readAllBytes(file))).toString();
            if (text.indexOf('\0') >= 0) throw new IllegalArgumentException("检测到二进制内容，请检查文件类型");
        }
        text = text.replace("\r\n", "\n").replace("\r", "\n").replace("\uFEFF", "").trim();
        if (text.isBlank()) throw new IllegalArgumentException("未提取到文字，可能是扫描件；请先OCR或上传文字版");
        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("正文最多100万字符");
        return new Parsed(text, warning);
    }

    public List<List<String>> readFaqTable(Path file, String format) throws Exception {
        if ("CSV".equals(format)) return csv(Files.readString(file, StandardCharsets.UTF_8).replace("\uFEFF", ""));
        validateOffice(file);
        try (var stream = Files.newInputStream(file); var workbook = WorkbookFactory.create(stream)) {
            List<List<String>> result = new ArrayList<>(); DataFormatter formatter = new DataFormatter();
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > 1000) throw new IllegalArgumentException("单次最多导入1000条问答");
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (int col = 0; col < Math.min(12, row.getLastCellNum()); col++) values.add(formatter.formatCellValue(row.getCell(col)));
                result.add(values);
            }
            return result;
        }
    }
    static List<List<String>> csv(String text) {
        List<List<String>> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false;
        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            if (c=='"') { if (quoted && i+1<text.length() && text.charAt(i+1)=='"') { cell.append('"'); i++; } else quoted=!quoted; }
            else if (c==',' && !quoted) { row.add(cell.toString().trim()); cell.setLength(0); }
            else if ((c=='\n' || c=='\r') && !quoted) {
                if (c=='\r' && i+1<text.length() && text.charAt(i+1)=='\n') i++;
                row.add(cell.toString().trim()); cell.setLength(0); rows.add(row); row = new ArrayList<>();
                if (rows.size()>1001) throw new IllegalArgumentException("单次最多导入1000条问答");
            } else cell.append(c);
        }
        if (quoted) throw new IllegalArgumentException("CSV引号未闭合");
        if (!cell.isEmpty() || !row.isEmpty()) { row.add(cell.toString().trim()); rows.add(row); }
        return rows;
    }
    private void validateOffice(Path path) throws Exception {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            if (zip.getEntry("[Content_Types].xml") == null) throw new IllegalArgumentException("不是有效的Office文档");
            long total = 0; int count = 0;
            for (var entries = zip.entries(); entries.hasMoreElements();) {
                var entry = entries.nextElement(); count++; total += Math.max(0, entry.getSize());
                if (count>2000 || total>80L*1024*1024) throw new IllegalArgumentException("Office文档展开后过大");
            }
        }
    }
    public record Parsed(String text, String warning) {}
}
