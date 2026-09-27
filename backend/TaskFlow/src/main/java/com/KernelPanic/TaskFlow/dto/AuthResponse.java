package com.KernelPanic.TaskFlow.dto;

/**
 * Resposta retornada após cadastro ou login bem-sucedidos.
 *
 * @param accessToken token JWT assinado, a ser enviado no cabeçalho
 *                     {@code Authorization: Bearer <token>} nas próximas requisições
 * @param tokenType    tipo do token (sempre "Bearer")
 * @param expiresIn    tempo de expiração do token, em segundos
 * @param user         dados públicos do usuário autenticado
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public AuthResponse(String accessToken, long expiresIn, UserResponse user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}
