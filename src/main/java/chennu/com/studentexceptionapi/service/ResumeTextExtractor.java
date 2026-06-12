package chennu.com.studentexceptionapi.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ResumeTextExtractor {

    public String extractText(MultipartFile file) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        byte[] bytes = file.getBytes();

        return switch (extension) {
            case "txt", "md", "csv" -> new String(bytes, StandardCharsets.UTF_8);
            case "pdf" -> extractPdf(bytes);
            case "docx" -> extractDocx(bytes);
            case "doc" -> extractDoc(bytes);
            case "rtf" -> extractRtf(bytes);
            default -> throw new IllegalArgumentException("Unsupported format: " + extension);
        };
    }

    public boolean isSupported(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        return extension.equals("pdf")
            || extension.equals("doc")
            || extension.equals("docx")
            || extension.equals("txt")
            || extension.equals("rtf")
            || extension.equals("md")
            || extension.equals("csv");
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes);
            XWPFDocument document = new XWPFDocument(is);
            XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDoc(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes);
            HWPFDocument document = new HWPFDocument(is);
            WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractRtf(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            RTFEditorKit kit = new RTFEditorKit();
            Document doc = kit.createDefaultDocument();
            kit.read(is, doc, 0);
            return doc.getText(0, doc.getLength());
        } catch (Exception ex) {
            throw new IOException("Failed to parse rtf file", ex);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ENGLISH);
    }
}
