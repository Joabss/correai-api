package com.correai.api.adapter.in.web.friendship;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.friendship.FriendshipEntry;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.in.friendship.FriendshipsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/friendships")
@SecurityRequirement(name = "bearerAuth")
public class FriendshipController {
    private final FriendshipsUseCase friendships;

    public FriendshipController(FriendshipsUseCase friendships) {
        this.friendships = friendships;
    }

    @GetMapping
    public PageResponse<FriendshipEntry> list(@RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(friendships.list(userId, new PageQuery(page, size)));
    }

    @GetMapping("/blocks")
    public PageResponse<UUID> listBlocked(@RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(friendships.listBlocked(userId, new PageQuery(page, size)));
    }

    @PutMapping("/{id}/request")
    public ResponseEntity<Void> request(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        friendships.request(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        friendships.accept(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        friendships.remove(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<Void> block(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        friendships.block(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/block")
    public ResponseEntity<Void> unblock(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        friendships.unblock(userId, id);
        return ResponseEntity.noContent().build();
    }
}
