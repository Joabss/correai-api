package com.correai.api.domain.port.in.challenge;

import com.correai.api.domain.model.challenge.Challenge;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.model.pagination.PageResult;
import java.util.UUID;

public interface ChallengeDraftsUseCase {
    Challenge create(UUID actor, ChallengeDraftCommand command);
    Challenge edit(UUID actor, UUID id, ChallengeDraftCommand command);
    Challenge publish(UUID actor, UUID id);
    Challenge getOwn(UUID actor, UUID id);
    PageResult<Challenge> listOwn(UUID actor, PageQuery page);
}
