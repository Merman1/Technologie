package com.example.technologie.services;

import com.example.technologie.model.SourceCodeModel;
import com.example.technologie.repo.SourceCodeRepo;


import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class SourceCodeService {
    @Autowired
    private SourceCodeRepo repository;

    private ModelMapper modelMapper;

    @Autowired
    public SourceCodeService(SourceCodeRepo repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    // ...

    public List<SourceCodeModel> getSourceCodeList() {
        List<SourceCodeModel> sourceCodeList = repository.findAll();
        return sourceCodeList.stream()
                .map(sourceCodeModel -> modelMapper.map(sourceCodeModel, SourceCodeModel.class))
                .collect(Collectors.toList());
    }
    public String uploadSourceCode(MultipartFile file) throws IOException {
        SourceCodeModel sourceCodeModel = repository.save(SourceCodeModel.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .sourceCode(file.getBytes()).build());
        if (sourceCodeModel != null) {
            return "File uploaded successfully: " + file.getOriginalFilename();
        }
        return null;
    }

    private boolean performPlagiarismCheck(MultipartFile file) {
        // Symulacja sprawdzania pliku przez antyplagiat
        Random random = new Random();
        return random.nextBoolean();
    }

    public byte[] downloadSourceCode(String fileName) {
        Optional<SourceCodeModel> dbSourceCodeData = repository.findByName(fileName);
        return dbSourceCodeData.get().getSourceCode();
    }

    public class PlagiarismResult {
        private boolean isPlagiarized;
        private double similarityPercentage;

        public PlagiarismResult(boolean isPlagiarized, double similarityPercentage) {
            this.isPlagiarized = isPlagiarized;
            this.similarityPercentage = similarityPercentage;
        }

        public boolean isPlagiarized() {
            return isPlagiarized;
        }

        public double getSimilarityPercentage() {
            return similarityPercentage;
        }
    }


    public PlagiarismResult checkPlagiarism(MultipartFile sourceCodeFile) throws IOException {
        List<SourceCodeModel> storedSourceCodes = repository.findAll();

        // Pobierz zawartość przesłanego pliku z kodem źródłowym
        byte[] sourceCode = sourceCodeFile.getBytes();

        double maxSimilarityPercentage = 0.0;

        for (SourceCodeModel storedSourceCode : storedSourceCodes) {
            // Pobierz zawartość przechowywanego pliku z kodem źródłowym
            byte[] storedCode = storedSourceCode.getSourceCode();

            // Sprawdź podobieństwo między przesłanym plikiem a przechowywanymi plikami
            double similarityPercentage = calculateSimilarityPercentage(sourceCode, storedCode);
            if (similarityPercentage >= 80.0) {
                // Znaleziono podobny kod, zwróć informację o plagiacie i procentowy poziom podobieństwa
                System.out.println("Plagiat!");
                return new PlagiarismResult(true, similarityPercentage);
            } else {
                // Aktualizuj najwyższy procentowy poziom podobieństwa
                maxSimilarityPercentage = Math.max(maxSimilarityPercentage, similarityPercentage);
            }
        }

        // Nie znaleziono podobnego kodu, zwróć najwyższy procentowy poziom podobieństwa
        return new PlagiarismResult(false, maxSimilarityPercentage);
    }




    private double calculateSimilarityPercentage(byte[] code1, byte[] code2) {
        int matchCount = 0;
        int totalCount = Math.max(code1.length, code2.length);

        for (int i = 0; i < totalCount; i++) {
            byte byte1 = (i < code1.length) ? code1[i] : 0;
            byte byte2 = (i < code2.length) ? code2[i] : 0;

            if (byte1 == byte2) {
                matchCount++;
            }
        }

        return (double) matchCount / totalCount * 100.0;
    }



}
