package com.correai.api.adapter.in.web.group;

import com.correai.api.adapter.in.web.dto.PageResponse;
import com.correai.api.domain.model.group.Group;
import com.correai.api.domain.model.pagination.PageQuery;
import com.correai.api.domain.port.in.group.GroupsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {
    private final GroupsUseCase groups;

    public GroupController(GroupsUseCase groups) {
        this.groups = groups;
    }

    public record CreateGroupRequest(@NotBlank @Size(max = 100) String name) {}

    @PostMapping
    public ResponseEntity<Group> create(@RequestAttribute("userId") UUID userId,
                                        @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groups.create(userId, request.name()));
    }

    @GetMapping
    public PageResponse<Group> search(@RequestAttribute("userId") UUID userId,
                                      @RequestParam String name,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "" + PageQuery.DEFAULT_SIZE) int size) {
        return PageResponse.from(groups.search(userId, name, new PageQuery(page, size)));
    }

    @PutMapping("/{id}/membership")
    public ResponseEntity<Void> join(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        groups.join(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/membership")
    public ResponseEntity<Void> leave(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        groups.leave(userId, id);
        return ResponseEntity.noContent().build();
    }
}
