package com.correai.api.application.media;

import com.correai.api.domain.model.activity.TimedRoutePoint;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class GpxReader {
    private GpxReader() {
    }

    public static List<List<TimedRoutePoint>> read(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid GPX size");
        }
        try {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(bytes));
            Deque<QName> path = new ArrayDeque<>();
            List<List<TimedRoutePoint>> segments = new ArrayList<>();
            List<TimedRoutePoint> segment = null;
            String namespace = null;
            QName pointName = null;
            double latitude = 0;
            double longitude = 0;
            Instant time = null;
            int count = 0;
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
                        throw new IllegalArgumentException("Invalid GPX document");
                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        if (path.size() >= 64) throw new IllegalArgumentException("GPX nesting is too deep");
                        String name = reader.getLocalName();
                        if (path.isEmpty()) {
                            namespace = reader.getNamespaceURI();
                            if (!"gpx".equals(name) || !("http://www.topografix.com/GPX/1/1".equals(namespace)
                                    || "http://www.topografix.com/GPX/1/0".equals(namespace))) {
                                throw new IllegalArgumentException("Invalid GPX document");
                            }
                        } else if (namespace.equals(reader.getNamespaceURI())) {
                            List<QName> parents = List.copyOf(path);
                            if (("trkseg".equals(name) && parents.equals(List.of(
                                    new QName(namespace, "trk"), new QName(namespace, "gpx"))))
                                    || ("rte".equals(name) && parents.equals(List.of(new QName(namespace, "gpx"))))) {
                                segment = new ArrayList<>();
                                segments.add(segment);
                            } else if (("trkpt".equals(name) && parents.equals(List.of(
                                    new QName(namespace, "trkseg"), new QName(namespace, "trk"), new QName(namespace, "gpx"))))
                                    || ("rtept".equals(name) && parents.equals(List.of(
                                    new QName(namespace, "rte"), new QName(namespace, "gpx"))))) {
                                latitude = coordinate(reader, "lat", 90);
                                longitude = coordinate(reader, "lon", 180);
                                pointName = reader.getName();
                                time = null;
                                if (++count > 100_000) throw new IllegalArgumentException("Too many GPX points");
                            } else if ("time".equals(name) && pointName != null && pointName.equals(path.peek())
                                    && path.size() == ("trkpt".equals(pointName.getLocalPart()) ? 4 : 3)) {
                                if (time != null) throw new IllegalArgumentException("Duplicate GPX point time");
                                time = OffsetDateTime.parse(reader.getElementText().trim()).toInstant();
                                continue;
                            }
                        }
                        path.push(reader.getName());
                    } else if (event == XMLStreamConstants.END_ELEMENT) {
                        if (pointName != null && reader.getName().equals(pointName) && path.size() <= 4) {
                            if (segment == null) throw new IllegalArgumentException("Invalid GPX segment");
                            if (time != null && !segment.isEmpty() && segment.getLast().time() != null
                                    && !time.isAfter(segment.getLast().time())) {
                                throw new IllegalArgumentException("Route times must be strictly increasing");
                            }
                            segment.add(new TimedRoutePoint(latitude, longitude, time));
                            pointName = null;
                        }
                        path.pop();
                    }
                }
            } finally {
                reader.close();
            }
            if (count < 2) throw new IllegalArgumentException("GPX must contain at least 2 route points");
            return segments.stream().map(List::copyOf).toList();
        } catch (XMLStreamException | DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid GPX document or point time", e);
        }
    }

    private static double coordinate(XMLStreamReader reader, String attribute, double limit) {
        try {
            double value = Double.parseDouble(reader.getAttributeValue(null, attribute));
            if (!Double.isFinite(value) || Math.abs(value) > limit) {
                throw new IllegalArgumentException("Invalid GPX coordinates");
            }
            return value;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid GPX coordinates", e);
        }
    }
}
