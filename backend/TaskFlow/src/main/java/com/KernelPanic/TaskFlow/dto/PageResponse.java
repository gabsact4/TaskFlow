package com.KernelPanic.TaskFlow.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope de paginação estável para a API, independente da representação
 * interna do Spring Data ({@link Page}), evitando expor detalhes internos
 * do framework no contrato público.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
