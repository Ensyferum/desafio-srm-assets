package com.srm.common.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void reusesIncomingCorrelationIdAndEchoesInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIds.HEADER, "cid-incoming");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInside = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> seenInside.set(CorrelationIds.get()));

        assertThat(seenInside.get()).isEqualTo("cid-incoming");
        assertThat(response.getHeader(CorrelationIds.HEADER)).isEqualTo("cid-incoming");
    }

    @Test
    void generatesNewCorrelationIdWhenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(CorrelationIds.HEADER)).isNotBlank();
    }

    @Test
    void clearsMdcAfterRequestFinishes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIds.HEADER, "cid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request, response, (req, res) -> assertThat(CorrelationIds.get()).isEqualTo("cid"));

        assertThat(CorrelationIds.get()).isNull();
    }
}
