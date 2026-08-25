package dev.geofencing;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class TrackedObjectResourceTest {

    @Test
    void createThenGetOne_returnsTheSameObject() {
	String id =
	    given()
		.contentType("application/json")
		.body("""
			{"name": "Van 12", "label": "vehicle"}
		    """)
	    .when()
		.post("/objects")
	    .then()
		.statusCode(201)
		.body("name", equalTo("Van 12"))
		.body("label", equalTo("vehicle"))
		.body("id", notNullValue())
		.extract().path("id");
	
	given()
	    .when().get("/objects/" + id)
	.then()
	    .statusCode(200)
	    .body("name", equalTo("Van 12"))
	    .body("label", equalTo("vehicle"));
    }
}
