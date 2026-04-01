package com.isga.translator.resource;

import com.isga.translator.model.TranscribeRequest;
import com.isga.translator.service.SpeechToTextService;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/transcribe")
public class TranscribeResource {

    @Context
    private ServletContext servletContext;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transcribe(TranscribeRequest request) {
        if (request.getAudio() == null || request.getAudio().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Audio field is required"))
                    .build();
        }

        try {
            String keyPath = getKeyPath();
            SpeechToTextService svc = new SpeechToTextService(keyPath);
            String transcript = svc.transcribe(request.getAudio());
            return Response.ok(Map.of("transcript", transcript)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Transcription failed: " + e.getMessage()))
                    .build();
        }
    }

    private String getKeyPath() {
        String path = System.getenv("VERTEX_KEY_PATH");
        if (path != null && !path.isBlank()) return path;
        return servletContext.getInitParameter("vertex.key.path");
    }
}
