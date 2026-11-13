package net.jxdg.travel.common;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 生成（出团通知/合同/结算单/名单）。使用系统中文字体，缺失时降级。
 */
@Component
public class PdfUtil {

    private static final String[] FONT_PATHS = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
    };

    public byte[] render(String title, List<String> lines) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType0Font font = null;
            for (String path : FONT_PATHS) {
                File f = new File(path);
                if (f.exists()) {
                    try {
                        font = PDType0Font.load(doc, f);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
            float margin = 56f;
            float y = page.getMediaBox().getHeight() - margin;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                if (font != null) {
                    cs.setFont(font, 18);
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(title);
                    cs.endText();
                    y -= 32;
                    cs.setFont(font, 11);
                } else {
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 16);
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText("TRAVEL PLAN");
                    cs.endText();
                    y -= 28;
                    cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                }
                List<String> wrapped = new ArrayList<>();
                for (String line : lines) {
                    wrapped.addAll(wrap(line == null ? "" : line, 48));
                }
                for (String line : wrapped) {
                    if (y < margin + 20) {
                        cs.endText();
                        break;
                    }
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    try {
                        cs.showText(line);
                    } catch (Exception e) {
                        cs.showText(new String(line.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                java.nio.charset.StandardCharsets.ISO_8859_1));
                    }
                    cs.endText();
                    y -= 18;
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("PDF 生成失败：" + e.getMessage());
        }
    }

    private List<String> wrap(String text, int size) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        if (text.length() <= size) {
            out.add(text);
            return out;
        }
        for (int i = 0; i < text.length(); i += size) {
            out.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return out;
    }

    /** 下载响应头 */
    public static String attachment(String filename) {
        try {
            return "attachment;filename*=utf-8''" + java.net.URLEncoder.encode(filename,
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "attachment";
        }
    }
}
