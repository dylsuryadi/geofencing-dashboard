package dev.geofencing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.agroal.DataSource;
import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.BadRequestException;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import java.time.OffsetDateTime;

record CreateGeofenceRequest(String name, String description, JsonNode geom) {}
record UpdateGeofenceRequest(String name, String description, JsonNode geom, Boolean active) {}
record Geofence(UUID id, String name, String description, JsonNode geom, boolean active, OffsetDateTime createdAt) {}

@Path("/geofences")
public class GeofenceResource {
    
    @Inject
    @DataSource("pg")
    javax.sql.DataSource pgDataSource;

    @Inject
    ObjectMapper objectMapper;

    private static final String SELECT_COLUMNS = 
	"id, name, description, ST_AsGeoJSON(geom) AS geom, active, created_at";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Geofence> listAll() throws Exception {
	String sql = "SELECT " + SELECT_COLUMNS + " FROM geofence ORDER BY created_at DESC";
	List<Geofence> results = new ArrayList<>();
	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql);
		ResultSet rs = stmt.executeQuery()) {
	    while (rs.next()) {
		results.add(mapRow(rs));
	    }

	}
	return results;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Geofence getOne(@PathParam("id") UUID id) throws Exception {
	String sql = "SELECT " + SELECT_COLUMNS + " FROM geofence WHERE id = ?";
	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setObject(1, id);
	    try (ResultSet rs = stmt.executeQuery()) {
		if (!rs.next()) {
		    throw new NotFoundException("No geofence with id=" + id);
		}
		return mapRow(rs);
	    }
	}
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(CreateGeofenceRequest request) throws Exception {
	validateGeom(request.geom());
	String sql = "INSERT INTO geofence (name, description, geom) " +
	    "VALUES (?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?::text), 4326)) " +
	    "RETURNING " + SELECT_COLUMNS;
	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setString(1, request.name());
	    stmt.setString(2, request.description());
	    stmt.setString(3, objectMapper.writeValueAsString(request.geom()));
	    try (ResultSet rs = stmt.executeQuery()) {
		if (rs.next()) {
		    Geofence created = mapRow(rs);
		    return Response.status(Response.Status.CREATED)
			.location(URI.create("/geofences/" + created.id()))
			.entity(created)
			.build();
		} else {
		    throw new WebApplicationException("Failed to create geofence name=" + request.name());
		}
	    }
	} catch (SQLException e) {
	    throw invalidGeometry(e);
	}
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") UUID id, UpdateGeofenceRequest request) throws Exception {
	validateGeom(request.geom());
	if (request.active() == null) {
	    throw new BadRequestException("active is required");
	}
	String sql = "UPDATE geofence SET name = ?, description = ?, " +
	    "geom = ST_SetSRID(ST_GeomFromGeoJSON(?::text), 4326), active = ? WHERE id = ?";
	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setString(1, request.name());
	    stmt.setString(2, request.description());
	    stmt.setString(3, objectMapper.writeValueAsString(request.geom()));
	    stmt.setBoolean(4, request.active());
	    stmt.setObject(5, id);
	    if (stmt.executeUpdate() == 1) {
		return Response.ok().build();
	    } else {
		throw new NotFoundException("No geofence with id=" + id);
	    }
	} catch (SQLException e) {
	    throw invalidGeometry(e);
	}
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") UUID id) throws Exception {
	String sql = "DELETE FROM geofence WHERE id = ?";
	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setObject(1, id);
	    if (stmt.executeUpdate() == 1) {
		return Response.noContent().build();
	    } else {
		throw new NotFoundException("No geofence with id=" + id);
	    }
	}
    }

    private void validateGeom(JsonNode geom) {
	if (geom == null || geom.isNull()) {
	    throw badRequest("geom is required");
	}
	JsonNode type = geom.get("type");
	if (type == null || !"Polygon".equals(type.asText())) {
	    throw badRequest("geom.type must be \"Polygon\"");
	}
    }

    private BadRequestException badRequest(String message) {
	return new BadRequestException(message,
		Response.status(Response.Status.BAD_REQUEST).entity(message).build());
    }

    private BadRequestException invalidGeometry(SQLException e) {
	String message = "Invalid geometry: " + e.getMessage();
	return new BadRequestException(message, 
		Response.status(Response.Status.BAD_REQUEST).entity(message).build());
    }

    private Geofence mapRow(ResultSet rs) throws Exception {
	JsonNode geom = objectMapper.readTree(rs.getString("geom"));
	return new Geofence(
		(UUID) rs.getObject("id"),
		rs.getString("name"),
		rs.getString("description"),
		geom,
		rs.getBoolean("active"),
		rs.getObject("created_at", OffsetDateTime.class)
	);
    }

}
