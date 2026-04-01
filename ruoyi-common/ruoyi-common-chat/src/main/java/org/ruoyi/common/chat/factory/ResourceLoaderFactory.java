package org.ruoyi.common.chat.factory;

import lombok.AllArgsConstructor;
import org.ruoyi.common.chat.constant.FileTypeConstants;
import org.ruoyi.common.chat.knowledge.ResourceLoader;
import org.ruoyi.common.chat.knowledge.loader.*;
import org.ruoyi.common.chat.knowledge.split.CharacterTextSplitter;
import org.ruoyi.common.chat.knowledge.split.CodeTextSplitter;
import org.ruoyi.common.chat.knowledge.split.ExcelTextSplitter;
import org.ruoyi.common.chat.knowledge.split.MarkdownTextSplitter;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ResourceLoaderFactory {
    private final CharacterTextSplitter characterTextSplitter;
    private final CodeTextSplitter codeTextSplitter;
    private final MarkdownTextSplitter markdownTextSplitter;
    private final ExcelTextSplitter excelTextSplitter;

    public ResourceLoader getLoaderByFileType(String fileType) {
        if (FileTypeConstants.isTextFile(fileType)) {
            return new TextFileLoader(characterTextSplitter);
        } else if (FileTypeConstants.isWord(fileType)) {
            return new WordLoader(characterTextSplitter);
        } else if (FileTypeConstants.isPdf(fileType)) {
            return new PdfFileLoader(characterTextSplitter);
        } else if (FileTypeConstants.isMdFile(fileType)) {
            return new MarkDownFileLoader(markdownTextSplitter);
        } else if (FileTypeConstants.isCodeFile(fileType)) {
            return new CodeFileLoader(codeTextSplitter);
        } else if (FileTypeConstants.isExcel(fileType)) {
            return new ExcelFileLoader(excelTextSplitter);
        } else {
            return new TextFileLoader(characterTextSplitter);
        }
    }
}
