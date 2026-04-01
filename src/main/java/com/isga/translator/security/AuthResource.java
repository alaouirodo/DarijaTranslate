package com.isga.translator.security;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/auth")
public class AuthResource {

    @Context
    private ServletContext servletContext;

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(Map<String, String> creds) {
        String username = creds.get("username");
        String password = creds.get("password");

        String validUser = servletContext.getInitParameter("app.username");
        String validPass = servletContext.getInitParameter("app.password");

        if (validUser.equals(username) && validPass.equals(password)) {
            String token = JwtUtil.generateToken(username);
            return Response.ok(Map.of("token", token)).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Invalid credentials"))
                .build();
    }
}
