package com.correai.api.adapter.in.web.media;

import com.correai.api.adapter.in.web.config.UserContextInterceptor;
import com.correai.api.adapter.in.web.config.WebConfig;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.model.media.MediaNotFoundException;
import com.correai.api.domain.port.in.media.MediaUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MediaController.class,
        excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = {UserContextInterceptor.class, WebConfig.class}))
class MediaControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean MediaUseCase media;

    @Test
    void uploadPhotoPassesAuthenticatedUserAndBytes() throws Exception {
        UUID userId = UUID.randomUUID();
        byte[] bytes = {1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", bytes);
        mockMvc.perform(multipart("/users/me/photo").file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).requestAttr("userId", userId))
                .andExpect(status().isNoContent());
        verify(media).save(userId, null, MediaKind.USER_PHOTO, bytes);
    }

    @Test
    void downloadRouteIsPrivateAndReturnsOriginalBytes() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        byte[] bytes = {1, 2, 3};
        when(media.get(userId, id, MediaKind.ACTIVITY_ROUTE))
                .thenReturn(new MediaContent("application/gpx+xml", bytes));
        mockMvc.perform(get("/activities/{id}/route", id).requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/gpx+xml"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void missingPhotoReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(media.get(userId, null, MediaKind.USER_PHOTO)).thenThrow(new MediaNotFoundException());
        mockMvc.perform(get("/users/me/photo").requestAttr("userId", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
