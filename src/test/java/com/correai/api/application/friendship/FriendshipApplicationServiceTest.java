package com.correai.api.application.friendship;

import com.correai.api.domain.model.friendship.Friendship;
import com.correai.api.domain.model.friendship.FriendshipConflictException;
import com.correai.api.domain.model.friendship.FriendshipNotFoundException;
import com.correai.api.domain.model.friendship.FriendshipState;
import com.correai.api.domain.port.out.friendship.FriendshipRepositoryPort;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendshipApplicationServiceTest {
    private final FriendshipRepositoryPort repository = mock(FriendshipRepositoryPort.class);
    private final Instant now = Instant.parse("2026-06-01T12:00:00Z");
    private final FriendshipApplicationService service =
            new FriendshipApplicationService(repository, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void rejectsInteractionsWithSelfAndDoesNotCallPersistence() {
        UUID user = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.request(user, user));
        assertThrows(IllegalArgumentException.class, () -> service.accept(user, user));
        assertThrows(IllegalArgumentException.class, () -> service.remove(user, user));
        assertThrows(IllegalArgumentException.class, () -> service.block(user, user));
        assertThrows(IllegalArgumentException.class, () -> service.unblock(user, user));
        verifyNoInteractions(repository);
    }

    @Test
    void requestUsesInjectedClock() {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        service.request(user, other);
        verify(repository).request(user, other, now);
    }

    @Test
    void onlyRecipientCanAcceptAndOppositeRequestCannotAcceptImplicitly() {
        UUID requester = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        Friendship pending = new Friendship(requester, recipient, FriendshipState.PENDING, now);
        assertSame(pending, pending.requestAgain(requester));
        assertThrows(FriendshipConflictException.class, () -> pending.requestAgain(recipient));
        assertThrows(FriendshipNotFoundException.class, () -> pending.accept(requester));
        Friendship accepted = pending.accept(recipient);
        assertEquals(FriendshipState.ACCEPTED, accepted.state());
        assertSame(accepted, accepted.accept(recipient));
    }
}
