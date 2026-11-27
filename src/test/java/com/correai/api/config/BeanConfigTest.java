package com.correai.api.config;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeanConfigTest {

    @Test
    void clock_shouldUseSaoPauloTimezone() {
        assertEquals(ZoneId.of("America/Sao_Paulo"), new BeanConfig().clock().getZone());
    }
}
