package com.example.technologie;
import com.example.technologie.model.SourceCodeModel;
import com.example.technologie.services.SourceCodeService;
import org.apache.commons.compress.archivers.ArchiveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/source-code")
@SpringBootApplication
public class TechnologieApplication {

    @Autowired
    private SourceCodeService service;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadSourceCode(@RequestParam("file") MultipartFile file, @RequestParam(value = "method", defaultValue = "similarity") String method) throws IOException, ArchiveException {
        SourceCodeService.PlagiarismResult plagiarismResult;

        if (file.getOriginalFilename().endsWith(".rar")) {
            plagiarismResult = service.checkPlagiarismForRAR(file, method);
        } else {
            if (method.equals("lineContent")) {
                SourceCodeService.PlagiarismResult lineContentPlagiarismResult = service.checkPlagiarismByLineContent(file, new ArrayList<>());
                plagiarismResult = lineContentPlagiarismResult;
            } else {
                plagiarismResult = service.checkPlagiarism(file, method);
            }
        }

        if (plagiarismResult.isPlagiarized()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Plagiarism detected. File rejected. Similarity: " + plagiarismResult.getSimilarityPercentage() + "%");
        } else {
            String uploadResult = service.uploadSourceCode(file);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(uploadResult);
        }
    }

    @GetMapping("/files")
    public String getFiles(Model model) {
        List<SourceCodeModel> sourceCodeList = service.getSourceCodeList();
        model.addAttribute("sourceCodeList", sourceCodeList);
        return "filess.html";
    }

    @GetMapping("/home")
    public String home() {
        return "index.html";
    }

    @PostMapping("/check-plagiarism")
    public String checkPlagiarism(@RequestParam("file") MultipartFile file, @RequestParam(value = "method", defaultValue = "similarity") String method, RedirectAttributes redirectAttributes) throws IOException {
        SourceCodeService.PlagiarismResult plagiarismResult = service.checkPlagiarism(file, method);

        if (plagiarismResult.isPlagiarized()) {
            redirectAttributes.addFlashAttribute("message", "Plagiarism detected. File rejected. Similarity: " + plagiarismResult.getSimilarityPercentage() + "%");
        } else {
            String uploadResult = service.uploadSourceCode(file);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + file.getOriginalFilename());
        }

        return "redirect:/source-code/home";
    }


    @GetMapping("/{fileName}")
public ResponseEntity<byte[]> downloadSourceCode(@PathVariable String fileName) {
    byte[] sourceCode = service.downloadSourceCode(fileName);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentDispositionFormData("attachment", fileName);
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

    return ResponseEntity.ok()
            .headers(headers)
            .body(sourceCode);
}


    public static void main(String[] args) {
        SpringApplication.run(TechnologieApplication.class, args);
    }
}
