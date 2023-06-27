package com.example.technologie;

import com.example.technologie.model.SourceCodeModel;
import com.example.technologie.repo.SourceCodeRepo;
import com.example.technologie.services.SourceCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
class TechnologieApplicationTests {

    private SourceCodeService sourceCodeService;

    @Mock
    private SourceCodeRepo sourceCodeRepo;
private ModelMapper modelMapperConfig;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sourceCodeService = new SourceCodeService(sourceCodeRepo,modelMapperConfig);
    }

    @Test
    void testGetSourceCodeList() {
        // Arrange
        SourceCodeModel sourceCode1 = new SourceCodeModel();
        sourceCode1.setName("code1.java");
        SourceCodeModel sourceCode2 = new SourceCodeModel();
        sourceCode2.setName("code2.java");
        List<SourceCodeModel> mockSourceCodeList = Arrays.asList(sourceCode1, sourceCode2);

        when(sourceCodeRepo.findAll()).thenReturn(mockSourceCodeList);

        // Act
        List<SourceCodeModel> result = sourceCodeService.getSourceCodeList();

        // Assert
        assertEquals(2, result.size());
        assertEquals("code1.java", result.get(0).getName());
        assertEquals("code2.java", result.get(1).getName());
        verify(sourceCodeRepo, times(1)).findAll();
    }

    @Test
    void testUploadSourceCode() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("test.java", "content".getBytes());

        when(sourceCodeRepo.findByName("test.java")).thenReturn(Optional.empty());
        when(sourceCodeRepo.save(any(SourceCodeModel.class))).thenReturn(new SourceCodeModel());

        // Act
        String result = sourceCodeService.uploadSourceCode(mockFile);

        // Assert
        assertEquals("File uploaded successfully: test.java", result);
        verify(sourceCodeRepo, times(1)).findByName("test.java");
        verify(sourceCodeRepo, times(1)).save(any(SourceCodeModel.class));
    }

    @Test
    void testUploadSourceCodeWithExistingFile() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile("test.java", "content".getBytes());
        SourceCodeModel existingFile = new SourceCodeModel();
        existingFile.setName("test.java");

        when(sourceCodeRepo.findByName("test.java")).thenReturn(Optional.of(existingFile));

        // Act
        String result = sourceCodeService.uploadSourceCode(mockFile);

        // Assert
        assertEquals("File with the same name already exists in the database: test.java", result);
        verify(sourceCodeRepo, times(1)).findByName("test.java");
        verify(sourceCodeRepo, times(0)).save(any(SourceCodeModel.class));
    }

    @Test
    void testDownloadSourceCode() {
        // Arrange
        String fileName = "test.java";
        byte[] sourceCodeBytes = "content".getBytes();
        SourceCodeModel sourceCodeModel = new SourceCodeModel();
        sourceCodeModel.setSourceCode(sourceCodeBytes);

        when(sourceCodeRepo.findByName(fileName)).thenReturn(Optional.of(sourceCodeModel));

        // Act
        byte[] result = sourceCodeService.downloadSourceCode(fileName);

        // Assert
        assertArrayEquals(sourceCodeBytes, result);
        verify(sourceCodeRepo, times(1)).findByName(fileName);
    }

}
