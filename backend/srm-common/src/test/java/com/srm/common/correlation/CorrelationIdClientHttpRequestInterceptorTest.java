package com.srm.common.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

class CorrelationIdClientHttpRequestInterceptorTest {

    private final CorrelationIdClientHttpRequestInterceptor interceptor =
            new CorrelationIdClientHttpRequestInterceptor();

    @Test
    void propagatesCorrelationIdFromMdcToOutgoingRequest() throws Exception {
        CorrelationIds.set("cid-propagated");
        MockClientHttpRequest request =
                new MockClientHttpRequest(
                        HttpMethod.GET, URI.create("http://currency-service/rates"));
        MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);

        ClientHttpResponse result =
                interceptor.intercept(request, new byte[0], (req, body) -> response);

        assertThat(request.getHeaders().getFirst(CorrelationIds.HEADER))
                .isEqualTo("cid-propagated");
        assertThat(result).isSameAs(response);
    }

    @Test
    void doesNotOverrideExistingCorrelationIdHeader() throws Exception {
        CorrelationIds.set("cid-from-mdc");
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://svc"));
        request.getHeaders().set(CorrelationIds.HEADER, "cid-already-set");

        interceptor.intercept(
                request,
                new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        assertThat(request.getHeaders().getFirst(CorrelationIds.HEADER))
                .isEqualTo("cid-already-set");
    }

    @Test
    void skipsHeaderWhenNoCorrelationIdInMdc() throws Exception {
        CorrelationIds.clear();
        MockClientHttpRequest request =
                new MockClientHttpRequest(HttpMethod.GET, URI.create("http://svc"));

        interceptor.intercept(
                request,
                new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        assertThat(request.getHeaders().containsKey(CorrelationIds.HEADER)).isFalse();
    }
}
