package com.example.technologie.repo;

import com.example.technologie.model.imageModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorageRepo extends JpaRepository<imageModel,Long> {

    Optional<imageModel> findByName(String fileName);
}

