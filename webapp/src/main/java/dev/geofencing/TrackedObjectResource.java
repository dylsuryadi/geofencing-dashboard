package dev.geofencing;

import io.quarkus.agroal.DataSource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
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
import java.util.ArrayList;
import java.util.List;

import java.time.OffsetDateTime;
import java.util.UUID;

record CreateTrackedObjectRequest(String name, String label) {}
record UpdateTrackedObjectRequest(String name, String label) {}
record TrackedObject(UUID id, String name, String label, OffsetDateTime createdAt) {}

@Path("/objects")
public class TrackedObjectResource {
    
    @Inject
    @DataSource("pg")
    javax.sql.DataSource pgDataSource;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrackedObject> listAll() throws Exception {
	String sql = "SELECT id, name, label, created_at FROM tracked_object ORDER BY created_at DESC";
	List<TrackedObject> results = new ArrayList<>();

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
    public TrackedObject getOne(@PathParam("id") UUID id) throws Exception {
	String sql = "SELECT id, name, label, created_at FROM tracked_object WHERE id = ?";

	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setObject(1, id);
	    try (ResultSet rs = stmt.executeQuery()) {
		if (!rs.next()) {
		    throw new NotFoundException("No tracked_object with id " + id);
		}
		return mapRow(rs);
	    }
	}
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(CreateTrackedObjectRequest request) throws Exception {
	String sql = "INSERT INTO tracked_object (name, label) VALUES (?, ?) " + 
	    "RETURNING id, name, label, created_at";

	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setString(1, request.name());
	    stmt.setString(2, request.label());

	    try (ResultSet rs = stmt.executeQuery()) {
		if (rs.next()) {
		    TrackedObject created = mapRow(rs);
		    return Response.status(Response.Status.CREATED)
			.location(URI.create("/objects/" + created.id()))
			.entity(created)
			.build();
		} else {
		    throw new WebApplicationException("Failed to create tracked_object name=" + request.name() + " label=" + request.label());
		}
	    }
	}
    }
    
    private TrackedObject mapRow(ResultSet rs) throws Exception {
	return new TrackedObject(
		(UUID) rs.getObject("id"),
		rs.getString("name"),
		rs.getString("label"),
		rs.getObject("created_at", OffsetDateTime.class)
	);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") UUID id, UpdateTrackedObjectRequest request) throws Exception {
	String sql = "UPDATE tracked_object SET name = ?, label= ? WHERE id = ?";

	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setString(1, request.name());
	    stmt.setString(2, request.label());
	    stmt.setObject(3, id);

	    if (stmt.executeUpdate() == 1) {
		return Response.ok().build();
	    } else {
		throw new NotFoundException("No tracked_object with id " + id);
	    }
	    
	}
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") UUID id) throws Exception {
	String sql = "DELETE FROM tracked_object WHERE id = ?";

	try (Connection conn = pgDataSource.getConnection();
		PreparedStatement stmt = conn.prepareStatement(sql)) {
	    stmt.setObject(1, id);

	    if (stmt.executeUpdate() == 1) {
		return Response.noContent().build();
	    } else {
		throw new NotFoundException("No tracked_object with id " + id);
	    }
	}
    }

}
