package com.house.expensetracker.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extract the token from the Authorization header
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7);

        try {
            // 2. Verify the Firebase ID token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // 3. Create the Spring Security Authentication object
            // The UID becomes the "principal" name (Principal.getName() will return this UID)
            UsernamePasswordAuthenticationToken authentication
                    = new UsernamePasswordAuthenticationToken(uid, null, null);

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 4. Set the authentication in the Security Context
            // This is what makes 'Principal principal' NON-NULL in your Controller!
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Log the failure, clear context, and let the request fail authorization
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
