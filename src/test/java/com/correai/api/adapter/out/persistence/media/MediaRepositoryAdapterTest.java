package com.correai.api.adapter.out.persistence.media;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.model.user.User;
import com.correai.api.domain.model.user.ConcurrentProfileUpdateException;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;
import com.correai.api.domain.port.out.user.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MediaRepositoryAdapterTest {
    @Autowired MediaRepositoryPort media;
    @Autowired UserRepositoryPort users;
    @Autowired ActivityRepositoryPort activities;

    @Test
    void storesReplacesAndSeparatesFilesByUserAndKind() {
        UUID user = users.save(User.anonymous()).id();
        UUID other = users.save(User.anonymous()).id();
        UUID activity = activities.save(Activity.create(user, ActivityType.RUN, 1, 300,
                null, null, null, LocalDate.now())).id();
        media.save(user, null, MediaKind.USER_PHOTO, new MediaContent("image/png", new byte[] {1}));
        media.save(user, activity, MediaKind.ACTIVITY_PHOTO, new MediaContent("image/jpeg", new byte[] {2}));
        media.save(user, activity, MediaKind.ACTIVITY_ROUTE, new MediaContent("application/gpx+xml", new byte[] {3}));
        media.save(user, activity, MediaKind.ACTIVITY_PHOTO, new MediaContent("image/png", new byte[] {4}));

        assertArrayEquals(new byte[] {1}, media.find(user, null, MediaKind.USER_PHOTO).orElseThrow().bytes());
        assertArrayEquals(new byte[] {4}, media.find(user, activity, MediaKind.ACTIVITY_PHOTO).orElseThrow().bytes());
        assertArrayEquals(new byte[] {3}, media.find(user, activity, MediaKind.ACTIVITY_ROUTE).orElseThrow().bytes());
        assertTrue(media.find(other, activity, MediaKind.ACTIVITY_PHOTO).isEmpty());
    }

    @Test
    void updatingProfilePreservesPasswordHash() {
        UUID user = users.saveWithPassword(User.registered("Ana", "ana@example.com"), "hash").id();
        users.updateProfile(user, "Ana Maria", "new@example.com", "ana@example.com");
        assertEquals("hash", users.findCredentialsByEmail("new@example.com").orElseThrow().passwordHash());
        assertTrue(users.findCredentialsByEmail("ana@example.com").isEmpty());
        users.updatePasswordHash(user, "new-hash");
        assertEquals("Ana Maria", users.findById(user).orElseThrow().name());
        assertEquals("new-hash", users.findCredentialsByEmail("new@example.com").orElseThrow().passwordHash());
        assertThrows(ConcurrentProfileUpdateException.class,
                () -> users.updateProfile(user, "Stale", "stale@example.com", "ana@example.com"));
        assertEquals("new@example.com", users.findById(user).orElseThrow().email());
    }

    @Test
    void galleryPreservesLegacyPhotoAndRetriesWithoutDuplicates() {
        UUID user = users.save(User.anonymous()).id();
        UUID other = users.save(User.anonymous()).id();
        UUID activity = activities.save(Activity.create(user, ActivityType.RUN, 1, 300,
                null, null, null, LocalDate.now())).id();
        UUID secondActivity = activities.save(Activity.create(user, ActivityType.RUN, 1, 300,
                null, null, null, LocalDate.now())).id();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        media.save(user, activity, MediaKind.ACTIVITY_PHOTO, new MediaContent("image/png", new byte[] {1}));
        media.save(user, activity, MediaKind.ACTIVITY_ROUTE, new MediaContent("application/gpx+xml", new byte[] {9}));
        media.saveActivityPhoto(user, activity, first, new MediaContent("image/png", new byte[] {2}));
        media.saveActivityPhoto(user, activity, second, new MediaContent("image/png", new byte[] {3}));
        media.saveActivityPhoto(user, activity, second, new MediaContent("image/png", new byte[] {4}));
        assertEquals(java.util.Set.of(activity, first, second),
                new java.util.HashSet<>(media.listActivityPhotos(user, activity)));
        assertEquals(3, media.listActivityPhotos(user, activity).size());
        assertArrayEquals(new byte[] {1}, media.findActivityPhoto(user, activity, activity).orElseThrow().bytes());
        assertArrayEquals(new byte[] {4}, media.findActivityPhoto(user, activity, second).orElseThrow().bytes());
        assertTrue(media.listActivityPhotos(other, activity).isEmpty());
        assertTrue(media.findActivityPhoto(other, activity, second).isEmpty());
        assertTrue(media.findActivityPhoto(user, secondActivity, second).isEmpty());
        assertTrue(media.listActivityPhotos(user, secondActivity).isEmpty());
    }
}
