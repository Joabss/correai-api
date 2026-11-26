package com.correai.api.adapter.out.persistence.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "media_files")
public class MediaEntity {
    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false)
    private UUID userId;

    private UUID activityId;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, length = 15728640)
    private byte[] content;

    protected MediaEntity() {
    }

    MediaEntity(String id, UUID userId, UUID activityId, String contentType, byte[] content) {
        this.id = id;
        this.userId = userId;
        this.activityId = activityId;
        this.contentType = contentType;
        this.content = content;
    }

    String contentType() {
        return contentType;
    }

    byte[] content() {
        return content;
    }
}
