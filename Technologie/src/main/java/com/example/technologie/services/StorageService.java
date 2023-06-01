package com.example.technologie.services;

import com.example.technologie.model.imageModel;
import com.example.technologie.repo.StorageRepo;
import com.example.technologie.utils.imageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

@Service
public class StorageService {
    @Autowired
    private StorageRepo repository;

    public String uploadImage(MultipartFile file) throws IOException {

        imageModel imagemodel = repository.save(imageModel.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .imageData(imageUtil.compressImage(file.getBytes())).build());
        if (imagemodel != null) {
            return "file uploaded successfully : " + file.getOriginalFilename();
        }
        return null;
    }

    private boolean performPlagiarismCheck(MultipartFile file) {
        // Symulacja sprawdzania pliku przez antyplagiat
        Random random = new Random();
        return random.nextBoolean();
    }
    public byte[] downloadImage(String fileName){
        Optional<imageModel> dbImageData = repository.findByName(fileName);
        byte[] images=imageUtil.decompressImage(dbImageData.get().getImageData());
        return images;
    }
}
