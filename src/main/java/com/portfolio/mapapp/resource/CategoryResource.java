package com.portfolio.mapapp.resource;

import com.portfolio.mapapp.db.CategoryDao;
import com.portfolio.mapapp.model.Category;
import com.portfolio.mapapp.model.CreateCategoryRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private final CategoryDao dao = new CategoryDao();

    @GET
    public List<Category> list() {
        return dao.findAll();
    }

    @POST
    public Response create(CreateCategoryRequest request, @Context UriInfo uriInfo) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "name は必須です"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        String color = (request.getColor() != null && !request.getColor().isBlank())
                ? request.getColor()
                : "#3B82F6";

        Category created = dao.insert(request.getName(), color);
        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(created.getId()))
                .build();
        return Response.created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Category update(@PathParam("id") long id, CreateCategoryRequest request) {
        if (id <= 2) {
            throw defaultCategoryError();
        }
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw badRequest("name は必須です");
        }
        String color = (request.getColor() != null && !request.getColor().isBlank())
                ? request.getColor()
                : "#3B82F6";
        return dao.update(id, request.getName(), color)
                .orElseThrow(() -> notFound(id));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        if (id <= 2) {
            throw defaultCategoryError();
        }
        boolean deleted = dao.delete(id);
        if (!deleted) {
            throw notFound(id);
        }
        return Response.noContent().build();
    }

    private WebApplicationException badRequest(String message) {
        return new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", message))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }

    private WebApplicationException defaultCategoryError() {
        return badRequest("デフォルトカテゴリは変更・削除できません");
    }

    private WebApplicationException notFound(long id) {
        return new WebApplicationException(
                Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "category(id=" + id + ") は存在しません"))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }
}
