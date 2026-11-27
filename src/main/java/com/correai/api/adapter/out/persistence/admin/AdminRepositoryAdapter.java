package com.correai.api.adapter.out.persistence.admin;

import com.correai.api.adapter.out.persistence.user.SpringDataUserRepository;
import com.correai.api.domain.model.admin.AdminAccessDeniedException;
import com.correai.api.domain.model.admin.AdminConflictException;
import com.correai.api.domain.model.user.UserNotFoundException;
import com.correai.api.domain.port.out.admin.AdminRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.Instant;
import java.util.UUID;

@Component
public class AdminRepositoryAdapter implements AdminRepositoryPort {
    private final SpringDataAdminStateRepository state;
    private final SpringDataAdminRepository admins;
    private final SpringDataAdminAuditRepository audit;
    private final SpringDataUserRepository users;

    public AdminRepositoryAdapter(SpringDataAdminStateRepository state, SpringDataAdminRepository admins,
                                  SpringDataAdminAuditRepository audit, SpringDataUserRepository users) {
        this.state = state;
        this.admins = admins;
        this.audit = audit;
        this.users = users;
    }

    private AdminStateEntity lockState() {
        return state.lockState().orElseThrow(() -> new IllegalStateException("Admin state is missing"));
    }

    private void requireAdmin(UUID userId) {
        if (!admins.existsById(userId)) throw new AdminAccessDeniedException();
    }

    private void requireRegistered(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getEmail() == null || user.getPasswordHash() == null) {
            throw new AdminConflictException("Administrator must have a registered account");
        }
    }

    @Override
    @Transactional
    public void bootstrap(UUID userId, Instant now) {
        AdminStateEntity current = lockState();
        if (current.initialized) throw new AdminConflictException("Administrator bootstrap already completed");
        requireRegistered(userId);
        admins.save(new AdminEntity(userId, userId, now));
        audit.save(new AdminAuditEntity(userId, userId, "BOOTSTRAP", now));
        current.initialized = true;
    }

    @Override
    @Transactional
    public void grant(UUID actorId, UUID targetId, Instant now) {
        lockState();
        requireAdmin(actorId);
        requireRegistered(targetId);
        if (!admins.existsById(targetId)) {
            admins.save(new AdminEntity(targetId, actorId, now));
            audit.save(new AdminAuditEntity(actorId, targetId, "GRANT", now));
        }
    }

    @Override
    @Transactional
    public void revoke(UUID actorId, UUID targetId, Instant now) {
        lockState();
        requireAdmin(actorId);
        if (admins.existsById(targetId)) {
            if (admins.count() <= 1) throw new AdminConflictException("Cannot revoke the last administrator");
            admins.deleteById(targetId);
            audit.save(new AdminAuditEntity(actorId, targetId, "REVOKE", now));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(UUID userId) {
        return admins.existsById(userId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean isAdminLocked(UUID userId) {
        lockState();
        return admins.existsById(userId);
    }
}
