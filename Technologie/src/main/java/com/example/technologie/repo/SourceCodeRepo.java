package com.example.technologie.repo;

import com.example.technologie.model.SourceCodeModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceCodeRepo extends JpaRepository<SourceCodeModel, Long> {

    Optional<SourceCodeModel> findByName(String fileName);
}
