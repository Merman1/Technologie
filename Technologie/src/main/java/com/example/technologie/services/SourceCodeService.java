package com.example.technologie.services;

import com.example.technologie.model.SourceCodeModel;
import com.example.technologie.repo.SourceCodeRepo;
import org.springframework.mock.web.MockMultipartFile;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SourceCodeService {
    private final SourceCodeRepo repository;
    private final ModelMapper modelMapper;

    @Autowired
    public SourceCodeService(SourceCodeRepo repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

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

    public byte[] downloadSourceCode(String fileName) {
        Optional<SourceCodeModel> dbSourceCodeData = repository.findByName(fileName);
        return dbSourceCodeData.get().getSourceCode();
    }

    public static class PlagiarismResult {
        private final boolean isPlagiarized;
        private final double similarityPercentage;

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

    public PlagiarismResult checkPlagiarismByLineContent(MultipartFile sourceCodeFile) throws IOException {
        List<SourceCodeModel> storedSourceCodes = repository.findAll();
        String sourceCode = new String(sourceCodeFile.getBytes());

        for (SourceCodeModel storedSourceCode : storedSourceCodes) {
            String code = new String(storedSourceCode.getSourceCode());

            String[] sourceCodeLines = sourceCode.split("\\r?\\n");
            String[] codeLines = code.split("\\r?\\n");

            if (sourceCodeLines.length == codeLines.length) {
                int totalLines = sourceCodeLines.length;
                int matchingLines = 0;

                for (int i = 0; i < totalLines; i++) {
                    if (compareLineContent(sourceCodeLines[i], codeLines[i])) {
                        matchingLines++;
                    }
                }

                double similarityPercentage = (double) matchingLines / totalLines * 100.0;
                if (similarityPercentage >= 80.0) {
                    return new PlagiarismResult(true, similarityPercentage);
                }
            }
        }

        return new PlagiarismResult(false, 0.0);
    }

    private boolean compareLineContent(String line1, String line2) {
        String[] words1 = line1.split("\\s");
        String[] words2 = line2.split("\\s");

        int matchCount = 0;

        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equalsIgnoreCase(word2)) {
                    matchCount++;
                    break;
                }
            }
        }

        double similarityPercentage = (double) matchCount / Math.max(words1.length, words2.length) * 100.0;
        return similarityPercentage >= 80.0;
    }

    public PlagiarismResult checkPlagiarism(MultipartFile sourceCodeFile, String method) throws IOException {
        if (method.equals("lineContent")) {
            PlagiarismResult lineContentPlagiarismResult = checkPlagiarismByLineContent(sourceCodeFile);
            return lineContentPlagiarismResult;
        } else {
            List<SourceCodeModel> storedSourceCodes = repository.findAll();
            byte[] sourceCode = sourceCodeFile.getBytes();
            double maxSimilarityPercentage = 0.0;

            for (SourceCodeModel storedSourceCode : storedSourceCodes) {
                byte[] storedCode = storedSourceCode.getSourceCode();
                double similarityPercentage = calculateSimilarityPercentage(sourceCode, storedCode);
                maxSimilarityPercentage = Math.max(maxSimilarityPercentage, similarityPercentage);
            }

            return new PlagiarismResult(maxSimilarityPercentage >= 80.0, maxSimilarityPercentage);
        }
    }

    public PlagiarismResult checkPlagiarismForRAR(MultipartFile rarFile, String method) throws IOException, ArchiveException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        String tempDirPath = tempDir.getAbsolutePath();
        String tempFilePath = tempDirPath + File.separator + rarFile.getOriginalFilename();

        File tempFile = new File(tempFilePath);
        rarFile.transferTo(tempFile);

        double maxSimilarityPercentage = 0.0;
        boolean isPlagiarized = false;

        try (ArchiveInputStream archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(new FileInputStream(tempFile))) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] sourceCode = IOUtils.toByteArray(archiveInputStream);
                    if (method.equals("lineContent")) {
                        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                                entry.getName(),
                                sourceCode
                        );

                        PlagiarismResult plagiarismResult = checkPlagiarismByLineContent(mockMultipartFile);

                        if (plagiarismResult.isPlagiarized()) {
                            return plagiarismResult;
                        } else {
                            maxSimilarityPercentage = Math.max(maxSimilarityPercentage, plagiarismResult.getSimilarityPercentage());
                            isPlagiarized = true; // Ustawienie flagi, jeśli istnieje podobieństwo w jakimkolwiek pliku
                        }
                    } else {
                        List<SourceCodeModel> storedSourceCodes = repository.findAll();
                        for (SourceCodeModel storedSourceCode : storedSourceCodes) {
                            byte[] storedCode = storedSourceCode.getSourceCode();
                            double similarityPercentage = calculateSimilarityPercentage(sourceCode, storedCode);
                            maxSimilarityPercentage = Math.max(maxSimilarityPercentage, similarityPercentage);
                            if (similarityPercentage >= 80.0) {
                                isPlagiarized = true;
                                break; // Przerwij pętlę, jeśli istnieje podobieństwo powyżej 80%
                            }
                        }
                    }
                }
            }
        } finally {
            tempFile.delete();
        }

        if (isPlagiarized || maxSimilarityPercentage >= 80.0) {
            return new PlagiarismResult(true, maxSimilarityPercentage);
        } else {
            return new PlagiarismResult(false, maxSimilarityPercentage);
        }
    }

    private double calculateSimilarityPercentage(byte[] code1, byte[] code2) {
        int matchCount = 0;
        int totalComparisonCount = Math.min(code1.length, code2.length);

        for (int i = 0; i < totalComparisonCount; i++) {
            if (code1[i] == code2[i]) {
                matchCount++;
            }
        }

        return (double) matchCount / totalComparisonCount * 100.0;
    }
}

