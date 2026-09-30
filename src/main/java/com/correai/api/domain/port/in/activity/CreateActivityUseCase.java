package com.correai.api.domain.port.in.activity;

/**
 * Inbound port (use case): create a new activity for a user.
 */
public interface CreateActivityUseCase {

    ActivityCreationResult create(CreateActivityCommand command);
}

