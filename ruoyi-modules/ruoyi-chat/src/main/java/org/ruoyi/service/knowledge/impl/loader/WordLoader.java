package org.ruoyi.service.knowledge.impl.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.chat.impl.provider.ProviderImageDescriber;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.TextSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.List;


@Component
@AllArgsConstructor
@Slf4j
public class WordLoader implements ResourceLoader {
    private final TextSplitter textSplitter;
    private final ChatServiceFactory chatServiceFactory;

    private ProviderImageDescriber getImageDescriber() {
        AbstractChatService service = chatServiceFactory.getOriginalService("multimodal");
        if (service instanceof ProviderImageDescriber describer) {
            return describer;
        }
        return null;
    }


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
     * 段落解析，识别标题层级和内嵌图片
     */
    private String parseParagraph(XWPFParagraph paragraph) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            String text = paragraph.getText().trim();
            return text.isEmpty() ? "\n" : text + "\n\n";
        }

        StringBuilder sb = new StringBuilder();
        boolean hasImage = false;

        for (XWPFRun run : runs) {
            List<XWPFPictureData> pictures = run.getEmbeddedPictures();
            if (pictures != null && !pictures.isEmpty()) {
                hasImage = true;
                for (XWPFPictureData pic : pictures) {
                    String fileName = pic.getFileName();
                    String suffix = (fileName != null && fileName.contains("."))
                        ? fileName.substring(fileName.lastIndexOf('.')) : ".png";

                    sb.append("![").append(fileName != null ? fileName : "图片").append("]");

                    try {
                        byte[] data = pic.getData();
                        ProviderImageDescriber describer = getImageDescriber();
                        String description = describer != null
                            ? describer.describe(data, fileName)
                            : (fileName != null ? fileName : "图片");
                        sb.append("![图片描述: ").append(description).append("]");
                    } catch (Exception e) {
                        log.warn("图片描述失败: {}, 跳过", fileName, e);
                        sb.append("![").append(fileName != null ? fileName : "图片").append("]");
                    }
                    sb.append("\n\n");

                    log.debug("识别到图片: {} ({} bytes)", fileName,
                        pic.getData() != null ? pic.getData().length : 0);
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
        if (runs.get(0).getFontSize() != null
            && runs.get(0).getFontSize() >= 18
            && text.length() < 50) {
            return "## " + text + "\n\n";
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



    @Override
    public List<String> getChunkList(String content, String kid) {
        return textSplitter.split(content, kid);
    }

}
