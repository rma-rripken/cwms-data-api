package cwms.cda.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import fixtures.TestAccounts;
import fixtures.users.UserSpecSource;
import fixtures.users.annotation.AuthType;
import freemarker.template.Template;
import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static cwms.cda.data.dao.JsonRatingUtilsTest.loadResourceAsString;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("integration")
public class AccessManagerTestIT extends DataApiTestIT {

    @ParameterizedTest
    @ArgumentsSource(UserSpecSource.class)
    @AuthType(userTypes = {AuthType.UserType.GUEST_AND_PRIVS})
    public void can_getOne_with_user(String authType, TestAccounts.KeyUser user, RequestSpecification authSpec) {
        given()
			.log().ifValidationFails(LogDetail.ALL,true)
            .spec(authSpec)
            .contentType("application/json")
            .queryParam("office", "SPK")
            .queryParam("names", "AR*")
            .queryParam("unit", "EN")
        .when()
            .get(  "/locations")
        .then().assertThat()
			.log().ifValidationFails(LogDetail.ALL,true)
            .statusCode(is(HttpServletResponse.SC_OK));
    }

    @ParameterizedTest
    @ArgumentsSource(UserSpecSource.class)
    @AuthType(user = TestAccounts.KeyUser.GUEST)
    public void cant_create_without_user(String authType, TestAccounts.KeyUser user, RequestSpecification authSpec) throws Exception {
        Template jsonTemplate = loadTemplateFromResource("cwms/cda/api/location_create.json");
        Map<String, Object> root = new HashMap<>();
        // the office here isn't critical as the request should fail before it tries to write
        root.put("office", "SPK");
        root.put("boundingOffice", "SPK");
        StringWriter out = new StringWriter();
        jsonTemplate.process(root, out);

        final String json = out.toString();
        assertNotNull(json);

        given()
			.log().ifValidationFails(LogDetail.ALL,true)
			.spec(authSpec)
			.contentType("application/json")
			.queryParam("office", "SPK")
			.body(json)
		.when()
			.post(  "/locations")
		.then()
			.assertThat()
			.log().ifValidationFails(LogDetail.ALL,true)
			.statusCode(is(HttpServletResponse.SC_UNAUTHORIZED));
    }

    @ParameterizedTest
    @ArgumentsSource(UserSpecSource.class)
    @AuthType(userTypes = { AuthType.UserType.PRIVS }, forOffice = "SPK")
    public void can_create_with_user(String authType, TestAccounts.KeyUser user, RequestSpecification authSpec) throws IOException {
        String json = loadResourceAsString("cwms/cda/api/location_create_spk.json");
        assertNotNull(json);

        given()
			.log().ifValidationFails(LogDetail.ALL,true)
            .contentType("application/json")
            .queryParam("office", user.getOperatingOffice())
            .spec(authSpec)
            .body(json)
        .when()
            .post(  "/locations/")
        .then()
			.log().ifValidationFails(LogDetail.ALL,true)
            .assertThat()
			.statusCode(is(HttpServletResponse.SC_ACCEPTED));
    }

    @ParameterizedTest
    @ArgumentsSource(UserSpecSource.class)
    @AuthType(userTypes = { AuthType.UserType.NO_PRIVS })
    public void cant_create_with_user_without_role(String authType, TestAccounts.KeyUser user, RequestSpecification authSpec) throws Exception {
        Template jsonTemplate = loadTemplateFromResource("cwms/cda/api/location_create.json");
        Map<String, Object> root = new HashMap<>();
        // the office here isn't critical as the request should fail before it tries to write
        root.put("office", "SPK");
        root.put("boundingOffice", "SPK");
        StringWriter out = new StringWriter();
        jsonTemplate.process(root, out);

        final String json = out.toString();

        given()
			.log().ifValidationFails(LogDetail.ALL,true)
            .contentType("application/json")
            .queryParam("office", "SPK")
            .spec(authSpec)
            .body(json)
        .when()
            .post(  "/locations")
        .then()
			.log().ifValidationFails(LogDetail.ALL,true)
            .assertThat()
			.statusCode(is(HttpServletResponse.SC_FORBIDDEN));
    }
}
