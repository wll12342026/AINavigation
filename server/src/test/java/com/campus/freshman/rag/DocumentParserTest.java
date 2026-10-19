package com.campus.freshman.rag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class DocumentParserTest {
    @TempDir Path temporary;
    @Test void extractsRealPdfTextAndPageBoundaries() throws Exception {
        Path file=temporary.resolve("guide.pdf");
        try(var document=new PDDocument()) {
            for(int i=1;i<=2;i++) {var page=new PDPage();document.addPage(page);try(var stream=new PDPageContentStream(document,page)){stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);stream.newLineAtOffset(40,700);stream.showText("Campus guide page "+i);stream.endText();}}
            document.save(file.toFile());
        }
        var parsed=new DocumentParser().parse(file,"PDF");assertThat(parsed.text()).contains("Campus guide page 1","\f","Campus guide page 2");
        assertThat(new Chunker().split(parsed.text(),800,100,true)).extracting(Chunker.Piece::page).containsExactly(1,2);
    }
    @Test void extractsRealDocxParagraphsAndTableHeaders() throws Exception {
        Path file=temporary.resolve("guide.docx");
        try(var doc=new XWPFDocument();var out=Files.newOutputStream(file)) {doc.createParagraph().createRun().setText("校园卡办理说明");var table=doc.createTable(2,2);table.getRow(0).getCell(0).setText("地点");table.getRow(0).getCell(1).setText("材料");table.getRow(1).getCell(0).setText("服务中心");table.getRow(1).getCell(1).setText("身份证");doc.write(out);}
        var parsed=new DocumentParser().parse(file,"DOCX");assertThat(parsed.text()).contains("校园卡办理说明","表格列：地点 | 材料","服务中心 | 身份证");assertThat(parsed.warning()).contains("图片未提取");
    }
    @Test void rejectsRenamedBinaryAndEmptyText() throws Exception {
        Path file=temporary.resolve("fake.pdf");Files.writeString(file,"not pdf",StandardCharsets.UTF_8);assertThatThrownBy(()->new DocumentParser().parse(file,"PDF")).hasMessageContaining("有效 PDF");
        Path empty=temporary.resolve("empty.txt");Files.writeString(empty,"   ");assertThatThrownBy(()->new DocumentParser().parse(empty,"TXT")).hasMessageContaining("未提取到文字");
    }
}
