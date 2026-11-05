package com.house.expensetracker.security;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private FirebaseToken firebaseToken;

    private FirebaseTokenFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new FirebaseTokenFilter();
        ReflectionTestUtils.setField(filter, "firebaseAuth", firebaseAuth);
    }

    @Test
    @DisplayName("Request without Authorization header passes through without setting SecurityContext")
    void noAuthorizationHeader_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("Request with valid Bearer token populates SecurityContext with AuthenticatedUser (UID, email, displayName)")
    void validBearerToken_populatesSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-firebase-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        given(firebaseAuth.verifyIdToken("valid-firebase-token")).willReturn(firebaseToken);
        given(firebaseToken.getUid()).willReturn("usr_carlos_123");
        given(firebaseToken.getEmail()).willReturn("Carlos@Example.com");
        given(firebaseToken.getName()).willReturn("Carlos Mendoza");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("usr_carlos_123");

        AuthenticatedUser principal = AuthenticatedUser.from(SecurityContextHolder.getContext().getAuthentication());
        assertThat(principal.uid()).isEqualTo("usr_carlos_123");
        assertThat(principal.email()).isEqualTo("carlos@example.com");
        assertThat(principal.displayName()).isEqualTo("Carlos Mendoza");
    }

    @Test
    @DisplayName("Request with invalid Bearer token returns 401 Unauthorized")
    void invalidBearerToken_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        given(firebaseAuth.verifyIdToken("bad-token")).willThrow(new RuntimeException("Token expired"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }
}

