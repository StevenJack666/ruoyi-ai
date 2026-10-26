package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.*;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.service.chat.doc.ImageOcrService;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;


@Component
@AllArgsConstructor
@Slf4j
public class WordLoader implements ResourceLoader {
    private final TextSplitter textSplitter;
    private final ImageOcrService imageOcrService = SpringUtils.getBean(ImageOcrService.class);


    @Override
    public String getContent(InputStream inputStream) {
        try {
            XWPFDocument document = new XWPFDocument(inputStream);
            StringBuilder sb = new StringBuilder();
            List<byte[]> images = new ArrayList<>();

            // 遍历文档中的所有元素（段落、表格等）
            for (IBodyElement element : document.getBodyElements()) {
                switch (element.getElementType()) {
                    case PARAGRAPH -> sb.append(parseParagraph((XWPFParagraph) element, images));
                    case TABLE -> sb.append(parseTable((XWPFTable) element, images));
                    default -> {}
                }
            }

            // 并发 OCR，一次性替换占位符
            String content = sb.toString();
            log.info("Word文档解析完成，开始进行图片OCR识别: 图片数量={}, 文本长度={}", images.size(), content.length());
            if (!images.isEmpty()) {
                List<String> ocrTexts = imageOcrService.ocrAll(images);
                for (int i = 0; i < ocrTexts.size(); i++) {
                    content = content.replace(
                        "{{IMG:" + i + "}}",
                        toImageDescription(ocrTexts.get(i))
                    );
                }
            }
            return content.trim();
        } catch (IOException e) {
            throw new RuntimeException("Word文档解析失败", e);
        }
    }

    /**
     * 把 OCR 结果渲染成与原逻辑一致的格式：![图片描述: xxx]
     * <p>
     * 注意：alt 文本里的换行会把 Markdown 拆断，"]" 会提前闭合标签，
     * 所以这里统一压成单行并转义。
     */
    private String toImageDescription(String text) {
        if (text == null || text.isBlank()) {
            return "![图片描述: 无法识别]";
        }
        String singleLine = text
            .replaceAll("\\s+", " ")   // 换行/制表/连续空白 → 单空格
            .replace("]", "\\]")       // 防止提前闭合
            .trim();
        return "![图片描述: " + singleLine + "]";
    }


    /**
     * 段落解析，识别标题层级和内嵌图片
     */
    private String parseParagraph(XWPFParagraph paragraph, List<byte[]> images) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            String text = paragraph.getText().trim();
            return text.isEmpty() ? "\n" : text + "\n\n";
        }

        StringBuilder sb = new StringBuilder();
        boolean hasImage = false;

        for (XWPFRun run : runs) {
            List<XWPFPicture> pictures = run.getEmbeddedPictures();
            if (pictures != null && !pictures.isEmpty()) {
                hasImage = true;
                for (XWPFPicture picture : pictures) {
                    XWPFPictureData data = picture.getPictureData();
                    if (data == null || data.getData() == null) continue;
                    images.add(data.getData());
                    sb.append("\n{{IMG:").append(images.size() - 1).append("}}\n");
                }
            } else {
                String runText = run.getText(0);
                if (runText != null) {
                    sb.append(runText);
                }
            }
        }

        if (hasImage) {
            return sb.toString();
        }

        // 纯文本段落，走原有标题检测逻辑
        String text = paragraph.getText().trim();
        if (text.isEmpty()) {
            return "\n";
        }

        String style = paragraph.getStyle();
        if (style != null && style.contains("Heading")) {
            int level = extractHeadingLevel(style);
            return "#".repeat(level) + " " + text + "\n\n";
        }
        Double fontSize = runs.getFirst().getFontSizeAsDouble();
        // 判断字号是否 >= 18（先判断 fontSize != null，防止空指针异常）
        if (fontSize != null && fontSize >= 18 && text.length() < 50) {
            return "## " + text + "\n\n";
        }
        return text + "\n\n";
    }

    /**
     * 表格解析为 Markdown 格式
     */
    private String parseTable(XWPFTable table, List<byte[]> images) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n");
        // 提取表头（第一行）
        XWPFTableRow headerRow = rows.get(0);
        List<String> headers = new ArrayList<>();
        for (XWPFTableCell cell : headerRow.getTableCells()) {
            headers.add(extractCellText(cell, images));
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
                cells.add(extractCellText(row.getCell(j), images));
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
    /**
     * 提取表格单元格内容，同时识别单元格内嵌图片（占位符格式与段落一致，便于统一 OCR 替换）
     */
    private String extractCellText(XWPFTableCell cell, List<byte[]> images) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph paragraph : cell.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs == null || runs.isEmpty()) {
                continue;
            }
            for (XWPFRun run : runs) {
                List<XWPFPicture> pictures = run.getEmbeddedPictures();
                if (pictures != null && !pictures.isEmpty()) {
                    for (XWPFPicture picture : pictures) {
                        XWPFPictureData data = picture.getPictureData();
                        if (data == null || data.getData() == null) {
                            continue;
                        }
                        images.add(data.getData());
                        sb.append("{{IMG:").append(images.size() - 1).append("}}");
                    }
                } else {
                    String runText = run.getText(0);
                    if (runText != null) {
                        sb.append(runText);
                    }
                }
            }
        }
        return sb.toString().trim().replace("|", "\\|");
    }

    @Override
    public List<String> getChunkList(String content, String kid) {
        return textSplitter.split(content, kid);
    }

}
