package tech.kayys.wayang.hitl.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@Disabled("Runtime module does not expose HITL task REST resources; covered by hitl-core")
class HumanTaskResourceTest {

    @Test
    void shouldListTasksSuccessfully() {
        given()
                .when().get("/api/v1/tasks")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldGetTaskByIdSuccessfully() {
        // This test assumes there's a task with ID "TASK-TEST" in the system
        // In a real scenario, we'd create a test task first
        given()
                .pathParam("taskId", "TASK-TEST")
                .when().get("/api/v1/tasks/{taskId}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldGetMyTasksSuccessfully() {
        given()
                .when().get("/api/v1/tasks/my-tasks")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldGetTasksForWorkflowSuccessfully() {
        given()
                .pathParam("workflowRunId", "RUN-TEST")
                .when().get("/api/v1/tasks/workflow/{workflowRunId}")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldGetTaskStatisticsSuccessfully() {
        given()
                .when().get("/api/v1/tasks/statistics")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldClaimTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-CLAIM" in the system
        given()
                .pathParam("taskId", "TASK-TO-CLAIM")
                .when().post("/api/v1/tasks/{taskId}/claim")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldReleaseTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-RELEASE" in the system
        given()
                .pathParam("taskId", "TASK-TO-RELEASE")
                .when().post("/api/v1/tasks/{taskId}/release")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldDelegateTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-DELEGATE" in the system
        given()
                .pathParam("taskId", "TASK-TO-DELEGATE")
                .contentType(ContentType.JSON)
                .body("{\"toUserId\":\"user2\",\"reason\":\"Going on vacation\"}")
                .when().post("/api/v1/tasks/{taskId}/delegate")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void shouldApproveTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-APPROVE" in the system
        given()
                .pathParam("taskId", "TASK-TO-APPROVE")
                .contentType(ContentType.JSON)
                .body("{\"comments\":\"Approved by reviewer\",\"data\":{\"result\":\"approved\"}}")
                .when().post("/api/v1/tasks/{taskId}/approve")
                .then()
                .statusCode(204); // No content expected
    }

    @Test
    void shouldRejectTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-REJECT" in the system
        given()
                .pathParam("taskId", "TASK-TO-REJECT")
                .contentType(ContentType.JSON)
                .body("{\"reason\":\"Does not meet criteria\",\"data\":{\"result\":\"rejected\"}}")
                .when().post("/api/v1/tasks/{taskId}/reject")
                .then()
                .statusCode(204); // No content expected
    }

    @Test
    void shouldCompleteTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-COMPLETE" in the system
        given()
                .pathParam("taskId", "TASK-TO-COMPLETE")
                .contentType(ContentType.JSON)
                .body("{\"outcome\":\"COMPLETED\",\"comments\":\"Task completed\",\"data\":{\"result\":\"completed\"}}")
                .when().post("/api/v1/tasks/{taskId}/complete")
                .then()
                .statusCode(204); // No content expected
    }

    @Test
    void shouldAddCommentToTaskSuccessfully() {
        // This test assumes there's a task with ID "TASK-TO-COMMENT" in the system
        given()
                .pathParam("taskId", "TASK-TO-COMMENT")
                .contentType(ContentType.JSON)
                .body("{\"comment\":\"This is a test comment\"}")
                .when().post("/api/v1/tasks/{taskId}/comments")
                .then()
                .statusCode(204); // No content expected
    }

    @Test
    void shouldGetTaskHistorySuccessfully() {
        // This test assumes there's a task with ID "TASK-WITH-HISTORY" in the system
        given()
                .pathParam("taskId", "TASK-WITH-HISTORY")
                .when().get("/api/v1/tasks/{taskId}/history")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }
}
