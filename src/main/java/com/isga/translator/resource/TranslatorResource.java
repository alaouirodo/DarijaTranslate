package com.isga.translator.resource;

import com.isga.translator.model.TranslationRequest;
import com.isga.translator.model.TranslationResponse;
import com.isga.translator.service.GeminiTranslationService;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/translate")
public class TranslatorResource {

    @Context
    private ServletContext servletContext;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response translate(TranslationRequest request) {
        if (request.getText() == null || request.getText().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Text field is required"))
                    .build();
        }

        try {
            String keyPath = getKeyPath();
            GeminiTranslationService svc = new GeminiTranslationService(keyPath);
            String[] result = svc.translate(request.getText());
            return Response.ok(new TranslationResponse(result[0], result[1])).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Translation failed: " + e.getMessage()))
                    .build();
        }
    }

    private String getKeyPath() {
        String path = System.getenv("VERTEX_KEY_PATH");
        if (path != null && !path.isBlank()) return path;
        return servletContext.getInitParameter("vertex.key.path");
    }
}
