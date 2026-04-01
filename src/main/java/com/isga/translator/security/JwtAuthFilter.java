package com.isga.translator.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

@Provider
public class JwtAuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String path = ctx.getUriInfo().getPath();

        if (path.startsWith("auth/") || "OPTIONS".equalsIgnoreCase(ctx.getMethod())) {
            return;
        }

        String authHeader = ctx.getHeaderString("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity(Map.of("error", "Missing or invalid Authorization header"))
                            .build());
            return;
        }

        String token = authHeader.substring(7);
        try {
            String username = JwtUtil.validateToken(token);
            ctx.setProperty("username", username);
        } catch (Exception e) {
            ctx.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity(Map.of("error", "Invalid or expired token"))
                            .build());
        }
    }
}
