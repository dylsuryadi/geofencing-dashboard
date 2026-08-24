package dev.geofencing;

import io.quarkus.agroal.DataSource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.ResultSet;

@Path("/db")
public class DbHealthResource {
    
    @Inject
    @DataSource("pg")
    javax.sql.DataSource pgDataSource;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String check() throws Exception {
        try (Connection conn = pgDataSource.getConnection();
                var stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT count(*) FROM tracked_object")) {
            rs.next();
            return "connected, tracked_object rows: " + rs.getInt(1);
        }
    }
}
