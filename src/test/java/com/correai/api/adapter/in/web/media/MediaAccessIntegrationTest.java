package com.correai.api.adapter.in.web.media;

import com.correai.api.domain.model.activity.Activity;
import com.correai.api.domain.model.activity.ActivityType;
import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.port.in.auth.IssueAnonymousTokenUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MediaAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IssueAnonymousTokenUseCase tokens;
    @Autowired ActivityRepositoryPort activities;

    @Test
    void authenticatedOwnerCanStoreAndRetrieveFilesButOtherUsersCannot() throws Exception {
        AuthToken owner = tokens.issue();
        AuthToken other = tokens.issue();
        Activity activity = activities.save(Activity.create(owner.userId(), ActivityType.RUN, 1, 300,
                null, null, null, LocalDate.now()));
        byte[] route = ("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\">"
                + "<trk><trkseg><trkpt lat=\"0\" lon=\"0\"/><trkpt lat=\"1\" lon=\"1\"/>"
                + "</trkseg></trk></gpx>").getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "route.gpx", "application/gpx+xml", route);
        String path = "/activities/" + activity.id() + "/route";
        mvc.perform(multipart(path).file(file).with(request -> {
            request.setMethod("PUT");
            return request;
        }).header("Authorization", "Bearer " + owner.token())).andExpect(status().isNoContent());
        mvc.perform(get(path).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(content().bytes(route));
        mvc.perform(get(path).header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(multipart(path).file(file).with(request -> {
            request.setMethod("PUT");
            return request;
        }).header("Authorization", "Bearer " + other.token())).andExpect(status().isNotFound());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        byte[] photo = output.toByteArray();
        MockMultipartFile image = new MockMultipartFile("file", "photo.png", "image/png", photo);
        mvc.perform(multipart("/users/me/photo").file(image).with(request -> {
            request.setMethod("PUT");
            return request;
        }).header("Authorization", "Bearer " + owner.token())).andExpect(status().isNoContent());
        mvc.perform(get("/users/me/photo").header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(content().bytes(photo));
        mvc.perform(get("/users/me/photo").header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());

        String photoPath = "/activities/" + activity.id() + "/photo";
        mvc.perform(multipart(photoPath).file(image).with(request -> {
            request.setMethod("PUT");
            return request;
        }).header("Authorization", "Bearer " + owner.token())).andExpect(status().isNoContent());
        mvc.perform(get(photoPath).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(content().bytes(photo));
        mvc.perform(get(photoPath).header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());

        String gallery = "/activities/" + activity.id() + "/photos";
        UUID photoId = UUID.randomUUID();
        String individual = gallery + "/" + photoId;
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(multipart(individual).file(image).with(request -> {
                request.setMethod("PUT");
                return request;
            }).header("Authorization", "Bearer " + owner.token())).andExpect(status().isNoContent());
        }
        mvc.perform(get(gallery).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasItems(activity.id().toString(), photoId.toString())));
        mvc.perform(get(individual).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(content().bytes(photo))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(gallery)).andExpect(status().isUnauthorized());
        mvc.perform(get(individual)).andExpect(status().isUnauthorized());
        mvc.perform(get(gallery).header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get(individual).header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(multipart(individual).file(image).with(request -> {
            request.setMethod("PUT");
            return request;
        }).header("Authorization", "Bearer " + other.token())).andExpect(status().isNotFound());
        mvc.perform(get(gallery + "/" + UUID.randomUUID()).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(individual)).andExpect(status().isUnauthorized());
        mvc.perform(delete(individual).header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get(individual).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete(individual).header("Authorization", "Bearer " + owner.token()))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(get(individual).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get(gallery).header("Authorization", "Bearer " + owner.token()))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete(gallery + "/" + activity.id()).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get(photoPath).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isNotFound());
        mvc.perform(get(path).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk()).andExpect(content().bytes(route));
    }
}
