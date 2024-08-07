package com.eqh.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Policy {

    @Id
    private Long id;
    private String polNumber;
    private String productCode;

    // Getters and setters
}

