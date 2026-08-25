package dev.geofencing;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeofenceResourceTest {

    private String createdId;
    
    @Test
    @Order(1)
    void createThenGetOne_returnsTheSameObject() {
	createdId =
	    given()
		.contentType("application/json")
		.body("""
			{
			    "name": "Port A", 
			    "description": "some description", 
			    "geom": {
				"type": "Polygon",
				"coordinates": [[
				    [8.47,53.47],
				    [8.48,53.47],
				    [8.48,53.48],
				    [8.47,53.48],
				    [8.47,53.47]
				]]
			    }
			}
		    """)
		.when()
		    .post("/geofences")
		.then()
		    .statusCode(201)
		    .body("name", equalTo("Port A"))
		    .body("description", equalTo("some description"))
		    .body("id", notNullValue())
		    .extract().path("id");

	given()
	    .when().get("/geofences/" + createdId)
	.then()
	    .statusCode(200)
	    .body("name", equalTo("Port A"))
	    .body("description", equalTo("some description"))
	    .body("geom.type", equalTo("Polygon"))
	    .body("geom.coordinates", contains(
			contains(
			    contains(8.47f, 53.47f),
			    contains(8.48f, 53.47f),
			    contains(8.48f, 53.48f),
			    contains(8.47f, 53.48f),
			    contains(8.47f, 53.47f)
			)
	    ));
    }

    @Test
    @Order(2)
    void createAnotherOne_listAll() {
	given()
	    .contentType("application/json")
	    .body("""
		    {
			"name": "Port B", 
			"description": "some description", 
			"geom": {
			    "type": "Polygon",
			    "coordinates": [[
				[8.47,53.47],
				[8.48,53.47],
				[8.48,53.48],
				[8.47,53.48],
				[8.47,53.47]
			    ]]
			}
		    }
	        """)
	    .when()
	        .post("/geofences")
	    .then()
	        .statusCode(201)
	        .body("name", equalTo("Port B"))
	        .body("description", equalTo("some description"))
	        .body("id", notNullValue());
	
	List<Object> geofences = 
	    given()
		.when().get("/geofences/")
	    .then()
		.statusCode(200)
		.extract().as(List.class);

	assertThat(geofences, hasSize(2));
    }

    @Test
    @Order(3)
    void updateAGeofence() {
	given()
	    .contentType("application/json")
	    .body("""
		    {
			"name": "NEW PORT A",
			"description": "new PORT A description",
			"geom": {
			    "type": "Polygon",
			    "coordinates": [[
				[8.45,53.41],
				[8.46,53.42],
				[8.47,53.43],
				[8.48,53.44],
				[8.49,53.45]
			    ]]
			},
			"active": true
		    }
	    """)
	.when()
	    .put("/geofences/" + createdId)
	.then()
	    .statusCode(200);
    }

    @Test
    @Order(4)
    void deleteAGeofence() {
	given()
	    .when()
		.delete("/geofences/" + createdId)
	.then()
	    .statusCode(204);
    }
}
