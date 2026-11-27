package com.correai.api.adapter.in.web.media;

import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.port.in.media.MediaUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import java.util.List;

@RestController
public class MediaController {
    private final MediaUseCase media;

    public MediaController(MediaUseCase media) {
        this.media = media;
    }

    @PutMapping(path = "/users/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveUserPhoto(@RequestAttribute("userId") UUID userId,
                                              @RequestPart("file") MultipartFile file) throws IOException {
        media.save(userId, null, MediaKind.USER_PHOTO, file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/photo")
    public ResponseEntity<byte[]> getUserPhoto(@RequestAttribute("userId") UUID userId) {
        return response(media.get(userId, null, MediaKind.USER_PHOTO));
    }

    @PutMapping(path = "/activities/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveActivityPhoto(@RequestAttribute("userId") UUID userId, @PathVariable UUID id,
                                                  @RequestPart("file") MultipartFile file) throws IOException {
        media.save(userId, id, MediaKind.ACTIVITY_PHOTO, file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities/{id}/photo")
    public ResponseEntity<byte[]> getActivityPhoto(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return response(media.get(userId, id, MediaKind.ACTIVITY_PHOTO));
    }

    @PutMapping(path = "/activities/{id}/route", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveActivityRoute(@RequestAttribute("userId") UUID userId, @PathVariable UUID id,
                                                  @RequestPart("file") MultipartFile file) throws IOException {
        media.save(userId, id, MediaKind.ACTIVITY_ROUTE, file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities/{id}/photos")
    public List<UUID> listActivityPhotos(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return media.listActivityPhotos(userId, id);
    }

    @PutMapping(path = "/activities/{id}/photos/{photoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> saveActivityPhotoById(@RequestAttribute("userId") UUID userId,
            @PathVariable UUID id, @PathVariable UUID photoId, @RequestPart("file") MultipartFile file)
            throws IOException {
        media.saveActivityPhoto(userId, id, photoId, file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities/{id}/photos/{photoId}")
    public ResponseEntity<byte[]> getActivityPhotoById(@RequestAttribute("userId") UUID userId,
            @PathVariable UUID id, @PathVariable UUID photoId) {
        return response(media.getActivityPhoto(userId, id, photoId));
    }

    @DeleteMapping("/activities/{id}/photos/{photoId}")
    public ResponseEntity<Void> deleteActivityPhoto(@RequestAttribute("userId") UUID userId,
            @PathVariable UUID id, @PathVariable UUID photoId) {
        media.deleteActivityPhoto(userId, id, photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities/{id}/route")
    public ResponseEntity<byte[]> getActivityRoute(@RequestAttribute("userId") UUID userId, @PathVariable UUID id) {
        return response(media.get(userId, id, MediaKind.ACTIVITY_ROUTE));
    }

    private ResponseEntity<byte[]> response(MediaContent content) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }
}
