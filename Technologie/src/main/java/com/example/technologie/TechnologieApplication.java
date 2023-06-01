package com.example.technologie;

import com.example.technologie.model.SourceCodeModel;
import com.example.technologie.repo.SourceCodeRepo;
import com.example.technologie.services.SourceCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/source-code")
@SpringBootApplication
public class TechnologieApplication {

    @Autowired
    private SourceCodeService service;


    @PostMapping
    public ResponseEntity<?> uploadSourceCode(@RequestParam("file") MultipartFile file) throws IOException {
        // Sprawdzenie antyplagiatu
        SourceCodeService.PlagiarismResult plagiarismResult = service.checkPlagiarism(file);

        if (plagiarismResult.isPlagiarized()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Plagiarism detected. File rejected. Similarity: " + plagiarismResult.getSimilarityPercentage() + "%");
        } else {
            String uploadResult = service.uploadSourceCode(file);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(uploadResult);
        }
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<?> downloadSourceCode(@PathVariable String fileName) {
        byte[] sourceCode = service.downloadSourceCode(fileName);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("text/plain"))
                .body(sourceCode);
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
    public String checkPlagiarism(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        SourceCodeService.PlagiarismResult plagiarismResult = service.checkPlagiarism(file);

        if (plagiarismResult.isPlagiarized()) {
            redirectAttributes.addFlashAttribute("message", "Plagiarism detected. File rejected. Similarity: " + plagiarismResult.getSimilarityPercentage() + "%");
        } else {
            String uploadResult = service.uploadSourceCode(file);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + file.getOriginalFilename());
        }

        return "redirect:/source-code/home";
    }
    public static void main(String[] args) {
        SpringApplication.run(TechnologieApplication.class, args);
    }

}
