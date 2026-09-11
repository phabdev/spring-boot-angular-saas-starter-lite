package com.phabdev.starter;

import static org.assertj.core.api.Assertions.*;

import com.phabdev.starter.auth.AuthRequestFilter;
import com.phabdev.starter.auth.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRequestFilterTest {
    @Test
    void limitsAuthRequestsAndIgnoresUntrustedForwardedHeaders() throws Exception {
        var filter = new AuthRequestFilter("http://localhost:4200", 1, false);
        var first = request("127.0.0.1");
        var accepted = new MockHttpServletResponse();
        filter.doFilter(
                first, accepted, (request, response) -> response.getWriter().write("accepted"));
        assertThat(accepted.getContentAsString()).isEqualTo("accepted");
        var second = request("127.0.0.1");
        second.addHeader("X-Forwarded-For", "203.0.113.99");
        var rejected = new MockHttpServletResponse();
        filter.doFilter(
                second,
                rejected,
                (request, response) -> {
                    throw new AssertionError("Rate limit was bypassed");
                });
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        var other = new MockHttpServletResponse();
        filter.doFilter(
                request("127.0.0.2"),
                other,
                (request, response) -> response.getWriter().write("accepted"));
        assertThat(other.getContentAsString()).isEqualTo("accepted");
    }

    @Test
    void productionOriginRequiresHttpsAndSecureCookiesAndSigningKeyMustBeStrong() {
        assertThatThrownBy(() -> new AuthRequestFilter("https://example.com", 30, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthRequestFilter("http://example.com", 30, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService("weak", 900))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MockHttpServletRequest request(String ip) {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(ip);
        request.addHeader("Origin", "http://localhost:4200");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        return request;
    }
}
