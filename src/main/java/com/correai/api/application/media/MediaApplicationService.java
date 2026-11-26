package com.correai.api.application.media;

import com.correai.api.domain.model.activity.ActivityNotFoundException;
import com.correai.api.domain.model.media.MediaContent;
import com.correai.api.domain.model.media.MediaKind;
import com.correai.api.domain.model.media.MediaNotFoundException;
import com.correai.api.domain.port.in.media.MediaUseCase;
import com.correai.api.domain.port.out.activity.ActivityRepositoryPort;
import com.correai.api.domain.port.out.media.MediaRepositoryPort;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class MediaApplicationService implements MediaUseCase {
    private static final int MAX_PHOTO_BYTES = 15 * 1024 * 1024;
    private static final int MAX_GPX_BYTES = 5 * 1024 * 1024;
    private final MediaRepositoryPort media;
    private final ActivityRepositoryPort activities;

    public MediaApplicationService(MediaRepositoryPort media, ActivityRepositoryPort activities) {
        this.media = media;
        this.activities = activities;
    }

    @Override
    public void save(UUID userId, UUID activityId, MediaKind kind, byte[] bytes) {
        checkOwner(userId, activityId, kind);
        if (bytes == null || bytes.length == 0
                || bytes.length > (kind == MediaKind.ACTIVITY_ROUTE ? MAX_GPX_BYTES : MAX_PHOTO_BYTES)) {
            throw new IllegalArgumentException("Invalid file size");
        }
        String contentType = kind == MediaKind.ACTIVITY_ROUTE ? validateGpx(bytes) : validatePhoto(bytes);
        media.save(userId, activityId, kind, new MediaContent(contentType, bytes));
    }

    @Override
    public MediaContent get(UUID userId, UUID activityId, MediaKind kind) {
        checkOwner(userId, activityId, kind);
        return media.find(userId, activityId, kind).orElseThrow(MediaNotFoundException::new);
    }

    private void checkOwner(UUID userId, UUID activityId, MediaKind kind) {
        if (userId == null || kind == null || (kind == MediaKind.USER_PHOTO) != (activityId == null)) {
            throw new IllegalArgumentException("Invalid file owner");
        }
        if (activityId != null && activities.findByIdAndUserId(activityId, userId).isEmpty()) {
            throw new ActivityNotFoundException();
        }
    }

    private String validatePhoto(byte[] bytes) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Only JPEG and PNG photos are supported");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("png")) {
                    throw new IllegalArgumentException("Only JPEG and PNG photos are supported");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 10000 || height > 10000
                        || (long) width * height > 40_000_000) {
                    throw new IllegalArgumentException("Invalid image dimensions");
                }
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("Invalid image");
                }
                return format.equalsIgnoreCase("png") ? "image/png" : "image/jpeg";
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid image", e);
        }
    }

    private String validateGpx(byte[] bytes) {
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(bytes));
            Deque<QName> path = new ArrayDeque<>();
            String namespace = null;
            int count = 0;
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
                        throw new IllegalArgumentException("Invalid GPX document");
                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        if (path.size() >= 64) {
                            throw new IllegalArgumentException("GPX nesting is too deep");
                        }
                        String name = reader.getLocalName();
                        if (path.isEmpty()) {
                            namespace = reader.getNamespaceURI();
                            if (!"gpx".equals(name) || !("http://www.topografix.com/GPX/1/1".equals(namespace)
                                    || "http://www.topografix.com/GPX/1/0".equals(namespace))) {
                                throw new IllegalArgumentException("Invalid GPX document");
                            }
                        } else if (namespace.equals(reader.getNamespaceURI())
                                && (("trkpt".equals(name) && List.copyOf(path).equals(List.of(
                                        new QName(namespace, "trkseg"), new QName(namespace, "trk"),
                                        new QName(namespace, "gpx"))))
                                || ("rtept".equals(name) && List.copyOf(path).equals(List.of(
                                        new QName(namespace, "rte"), new QName(namespace, "gpx")))))) {
                            validatePoint(reader);
                            if (++count > 100_000) {
                                throw new IllegalArgumentException("Too many GPX points");
                            }
                        }
                        path.push(reader.getName());
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        path.pop();
                    }
                }
            } finally {
                reader.close();
            }
            if (count < 2) {
                throw new IllegalArgumentException("GPX must contain at least 2 route points");
            }
            return "application/gpx+xml";
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Invalid GPX document", e);
        }
    }

    private void validatePoint(XMLStreamReader reader) {
        try {
            double latitude = Double.parseDouble(reader.getAttributeValue(null, "lat"));
            double longitude = Double.parseDouble(reader.getAttributeValue(null, "lon"));
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                    || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                throw new IllegalArgumentException("Invalid GPX coordinates");
            }
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid GPX coordinates", e);
        }
    }
}
