package com.correai.api.domain.port.in.auth;

import com.correai.api.domain.model.auth.AuthToken;

public interface RefreshTokenUseCase {

    /** Exchanges a refresh token for a new access token and a rotated refresh token. */
    AuthToken refresh(String refreshToken);
}
