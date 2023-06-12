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

    public PlagiarismResult checkPlagiarism(MultipartFile sourceCodeFile, String method) throws IOException {
        if (method.equals("lineLength")) {
            boolean isPlagiarizedByLineLength = checkPlagiarismByLineLength(sourceCodeFile);
            return new PlagiarismResult(isPlagiarizedByLineLength, 100.0);
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

    public boolean checkPlagiarismByLineLength(MultipartFile sourceCodeFile) throws IOException {
        List<SourceCodeModel> storedSourceCodes = repository.findAll();
        String sourceCode = new String(sourceCodeFile.getBytes());

        for (SourceCodeModel storedSourceCode : storedSourceCodes) {
            String storedCode = new String(storedSourceCode.getSourceCode());

            String[] sourceCodeLines = sourceCode.split("\\r?\\n");
            String[] storedCodeLines = storedCode.split("\\r?\\n");

            if (sourceCodeLines.length == storedCodeLines.length) {
                boolean isPlagiarized = true;

                for (int i = 0; i < sourceCodeLines.length; i++) {
                    if (sourceCodeLines[i].length() != storedCodeLines[i].length()) {
                        isPlagiarized = false;
                        break;
                    }
                }

                if (isPlagiarized) {
                    return true;
                }
            }
        }

        return false;
    }

    public PlagiarismResult checkPlagiarismForRAR(MultipartFile rarFile, String method) throws IOException, ArchiveException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        String tempDirPath = tempDir.getAbsolutePath();
        String tempFilePath = tempDirPath + File.separator + rarFile.getOriginalFilename();


        File tempFile = new File(tempFilePath);
        rarFile.transferTo(tempFile);


        List<SourceCodeModel> storedSourceCodes = repository.findAll();
        double maxSimilarityPercentage = 0.0;

        try (ArchiveInputStream archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(new FileInputStream(tempFile))) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] sourceCode = IOUtils.toByteArray(archiveInputStream);
                    if (method.equals("lineLength")) {

                        MockMultipartFile mockMultipartFile = new MockMultipartFile("sourceCodeFile", sourceCode);


                        boolean isPlagiarizedByLineLength = checkPlagiarismByLineLength(mockMultipartFile);

                        if (isPlagiarizedByLineLength) {
                            return new PlagiarismResult(true, 100.0);
                        }
                    } else {
                        for (SourceCodeModel storedSourceCode : storedSourceCodes) {
                            byte[] storedCode = storedSourceCode.getSourceCode();
                            double similarityPercentage = calculateSimilarityPercentage(sourceCode, storedCode);
                            maxSimilarityPercentage = Math.max(maxSimilarityPercentage, similarityPercentage);
                        }
                    }
                }
            }
        } finally {

            tempFile.delete();
        }

        return new PlagiarismResult(maxSimilarityPercentage >= 80.0, maxSimilarityPercentage);
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
