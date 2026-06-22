package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import java.util.ArrayList;


@Component
@AllArgsConstructor
@Slf4j
public class WordLoader implements ResourceLoader {
    private final TextSplitter textSplitter;


    @Override
    public String getContent(InputStream inputStream) {
        try {
            XWPFDocument document = new XWPFDocument(inputStream);
            StringBuilder sb = new StringBuilder();
    
            // 遍历文档中的所有元素（段落、表格等）
            for (IBodyElement element : document.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH -> sb.append(parseParagraph((XWPFParagraph) element));
                    case TABLE -> sb.append(parseTable((XWPFTable) element));
                    default -> {}
                }
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("Word文档解析失败", e);
        }
    }
    
    /**
     * 段落解析，识别标题层级
     */
    private String parseParagraph(XWPFParagraph paragraph) {
        String text = paragraph.getText().trim();
        if (text.isEmpty()) {
            return "\n";
        }
    
        String style = paragraph.getStyle();
        if (style != null) {
            // 识别标题样式
            if (style.contains("Heading")) {
                int level = extractHeadingLevel(style);
                String prefix = "#".repeat(level);
                return prefix + " " + text + "\n\n";
            }
        }
        // 通过字号判断是否为标题（备用逻辑）
        if (paragraph.getRuns().size() > 0) {
            Integer fontSize = paragraph.getRuns().get(0).getFontSize();
            if (fontSize != null && fontSize >= 18 && text.length() < 50) {
                return "## " + text + "\n\n";
            }
        }
        return text + "\n\n";
    }
    
    /**
     * 表格解析为 Markdown 格式
     */
    private String parseTable(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n");
        // 提取表头（第一行）
        XWPFTableRow headerRow = rows.get(0);
        List<String> headers = new ArrayList<>();
        for (XWPFTableCell cell : headerRow.getTableCells()) {
            headers.add(cell.getText().trim().replace("|", "\\|"));
        }
    
        // 表头行
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");
    
        // 分隔行
        sb.append("|").append("------|".repeat(headers.size())).append("\n");
    
        // 数据行
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<String> cells = new ArrayList<>();
            for (int j = 0; j < headers.size() && j < row.getTableCells().size(); j++) {
                cells.add(row.getCell(j).getText().trim().replace("|", "\\|"));
            }
            sb.append("| ").append(String.join(" | ", cells)).append(" |\n");
        }
    
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 从样式名提取标题层级（如 "Heading2" → 2）
     */
    private int extractHeadingLevel(String style) {
        try {
            return Integer.parseInt(style.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    // @Override
    // public String getContent(InputStream inputStream) {
    //     XWPFDocument document = null;
    //     try {
    //         document = new XWPFDocument(inputStream);
    //         XWPFWordExtractor extractor = new XWPFWordExtractor(document);
    //         String content = extractor.getText();
    //         return content;
    //     } catch (IOException e) {
    //         throw new RuntimeException(e);
    //     }
    // }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return textSplitter.split(content, kid);
    }

}
