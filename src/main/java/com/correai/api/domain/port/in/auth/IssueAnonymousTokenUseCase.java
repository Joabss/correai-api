package com.correai.api.domain.port.in.auth;

import com.correai.api.domain.model.auth.AuthToken;

/**
 * Inbound port (use case): creates an anonymous user and issues an access token for it.
 */
public interface IssueAnonymousTokenUseCase {

    AuthToken issue();
}
