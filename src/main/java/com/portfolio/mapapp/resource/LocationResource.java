package com.portfolio.mapapp.resource;

import com.portfolio.mapapp.db.LocationDao;
import com.portfolio.mapapp.model.CreateLocationRequest;
import com.portfolio.mapapp.model.Location;
import com.portfolio.mapapp.model.UpdateLocationRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * locations テーブルに対する REST API。
 *
 * <ul>
 *   <li>GET  /locations       … 全件取得</li>
 *   <li>GET  /locations/{id}  … 1件取得</li>
 *   <li>POST /locations       … 新規登録</li>
 * </ul>
 */
@Path("/locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationResource {

    private final LocationDao dao = new LocationDao();

    @GET
    public List<Location> list() {
        return dao.findAll();
    }

    @GET
    @Path("/nearby")
    public List<Location> nearby(
            @QueryParam("lat") Double lat,
            @QueryParam("lng") Double lng,
            @QueryParam("radius") @DefaultValue("1000") double radius) {
        if (lat == null || lng == null) {
            throw badRequest("lat と lng は必須です");
        }
        return dao.findNearby(lat, lng, radius);
    }

    @GET
    @Path("/{id}")
    public Location get(@PathParam("id") long id) {
        return dao.findById(id)
                .orElseThrow(() -> new WebApplicationException(
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "location(id=" + id + ") は存在しません"))
                                .type(MediaType.APPLICATION_JSON)
                                .build()));
    }

    @PUT
    @Path("/{id}")
    public Location update(@PathParam("id") long id, UpdateLocationRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw badRequest("name は必須です");
        }
        return dao.update(id, request.getName(), request.getCategoryId())
                .orElseThrow(() -> new WebApplicationException(
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(Map.of("error", "location(id=" + id + ") は存在しません"))
                                .type(MediaType.APPLICATION_JSON)
                                .build()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        boolean deleted = dao.delete(id);
        if (!deleted) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "location(id=" + id + ") は存在しません"))
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
        return Response.noContent().build();
    }

    @POST
    public Response create(CreateLocationRequest request, @Context UriInfo uriInfo) {
        validate(request);

        Location created = dao.insert(request.getName(), request.getLatitude(), request.getLongitude(), request.getCategoryId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(created.getId()))
                .build();
        return Response.created(location).entity(created).build();
    }

    /** リクエストの必須項目と値域を検証し、不正なら 400 を返す。 */
    private void validate(CreateLocationRequest request) {
        if (request == null) {
            throw badRequest("リクエストボディが空です");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw badRequest("name は必須です");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw badRequest("latitude と longitude は必須です");
        }
        double lat = request.getLatitude();
        double lng = request.getLongitude();
        if (lat < -90 || lat > 90) {
            throw badRequest("latitude は -90〜90 の範囲で指定してください");
        }
        if (lng < -180 || lng > 180) {
            throw badRequest("longitude は -180〜180 の範囲で指定してください");
        }
    }

    private WebApplicationException badRequest(String message) {
        return new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", message))
                        .type(MediaType.APPLICATION_JSON)
                        .build());
    }
}
