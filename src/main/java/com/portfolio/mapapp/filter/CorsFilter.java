package com.portfolio.mapapp.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOWED_ORIGIN = resolveOrigin();

    private static String resolveOrigin() {
        String origin = System.getenv("CORS_ORIGIN");
        return (origin == null || origin.isBlank()) ? "http://localhost:5173" : origin;
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            Response preflight = Response.ok()
                    .header("Access-Control-Allow-Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Allow-Headers", "Content-Type, Accept")
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                    .build();
            request.abortWith(preflight);
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
        response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, Accept");
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    }
}
