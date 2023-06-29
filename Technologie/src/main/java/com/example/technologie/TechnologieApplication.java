package com.example.technologie;
import com.example.technologie.model.SourceCodeModel;
import com.example.technologie.repo.SourceCodeRepo;
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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/source-code")
@SpringBootApplication
public class TechnologieApplication {

    @Autowired
    private SourceCodeService service;
    @Autowired
    private final SourceCodeRepo sourceCodeRepo;
    @Autowired
    public TechnologieApplication(SourceCodeRepo sourceCodeRepo) {
        this.sourceCodeRepo = sourceCodeRepo;
    }
    @PostMapping("/upload")
    public String uploadSourceCode(@RequestParam("file") MultipartFile file, @RequestParam(value = "method", defaultValue = "similarity") String method, Model model) throws IOException, ArchiveException {
        SourceCodeService.PlagiarismResult plagiarismResult;

        if (file.getOriginalFilename().endsWith(".rar")) {
            plagiarismResult = service.checkPlagiarismForRAR(file, method);
        } else {
            if (method.equals("lineContent")) {
                SourceCodeService.PlagiarismResult lineContentPlagiarismResult = service.checkPlagiarismByLineContent(file);
                plagiarismResult = lineContentPlagiarismResult;
            } else {
                plagiarismResult = service.checkPlagiarism(file, method);
            }
        }

        if (plagiarismResult.isPlagiarized()) {
            model.addAttribute("message", "Plagiarism detected. File rejected. Similarity: " + plagiarismResult.getSimilarityPercentage() + "%");
        } else {
            String uploadResult = service.uploadSourceCode(file);
            model.addAttribute("message", uploadResult);
        }

        List<SourceCodeModel> sourceCodeList = service.getSourceCodeList();
        model.addAttribute("sourceCodeList", sourceCodeList);

        return "filess.html";
    }

    @GetMapping("/files")
    public String getFiles(Model model) {
        List<SourceCodeModel> sourceCodeList = service.getSourceCodeList();
        model.addAttribute("sourceCodeList", sourceCodeList);
        return "filess.html";
    }
    @GetMapping("/files/delete/{id}")
    public String deleteFile(@PathVariable("id") Long id) {
        sourceCodeRepo.deleteById(id);
        return "redirect:/source-code/files";
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
