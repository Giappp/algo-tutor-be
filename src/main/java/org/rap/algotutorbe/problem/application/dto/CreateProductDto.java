package org.rap.algotutorbe.problem.application.dto;

public record CreateProductDto(String slug,
                              String title,
                              String statement,
                              String difficulty,
                              String status,
                              ConstraintsDto constraints,
                              String[] allowedLanguages){}
