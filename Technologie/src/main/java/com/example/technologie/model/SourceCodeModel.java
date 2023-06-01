package com.example.technologie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "SourceCodeData")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SourceCodeModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    @Lob
    @Column(name = "sourcecode", length = 100000)
    private byte[] sourceCode;
}
