package tech.kayys.wayang.hitl.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.hitl.service.HumanTaskService;
import tech.kayys.wayang.hitl.service.TaskStatistics;

@QuarkusTest
@TestSecurity(user = "test-user")
class HumanTaskResourceTest {

    @InjectMock
    HumanTaskService taskService;

    @Test
    void testGetStatisticsEndpoint() {
        // Mock the service response
        TaskStatistics mockStats = new TaskStatistics(5L, 10L, 2L);
        when(taskService.getUserTaskStatistics("test-user", "default-tenant"))
            .thenReturn(Uni.createFrom().item(mockStats));

        // Test the endpoint
        given()
          .when()
            .get("/api/v1/tasks/statistics")
          .then()
            .statusCode(200)
            .body("activeTasks", equalTo(5))
            .body("completedToday", equalTo(10))
            .body("overdueTasks", equalTo(2));
    }
}