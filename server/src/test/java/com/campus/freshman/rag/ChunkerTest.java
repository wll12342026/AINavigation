package com.campus.freshman.rag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ChunkerTest {
    @Test void preservesPdfPageNumbersAndOfficeDoesNotInventPages() {
        var chunker=new Chunker();assertThat(chunker.split("第一页内容\f第二页内容",800,100,true)).extracting(Chunker.Piece::page).containsExactly(1,2);assertThat(chunker.split("Word正文",800,100,false).getFirst().page()).isNull();
    }
    @Test void validatesOverlap() {assertThatThrownBy(()->new Chunker().split("内容",200,200,false)).hasMessageContaining("重叠");}
    @Test void parsesQuotedCsvAndRejectsBrokenQuotes() {
        var rows=DocumentParser.csv("标准问题,标准答案\n\"问,题\",\"第一行\n第二行\"\n");assertThat(rows.get(1)).containsExactly("问,题","第一行\n第二行");assertThatThrownBy(()->DocumentParser.csv("a,\"broken")).hasMessageContaining("引号未闭合");
    }
    @Test void dynamicAndMultiTaskQuestionsNeverUseSimpleFastPath() {assertThat(WorkspaceSearch.simple("饭卡在哪领，顺便规划路线")).isFalse();assertThat(WorkspaceSearch.simple("图书馆现在开放吗")).isFalse();}
}
