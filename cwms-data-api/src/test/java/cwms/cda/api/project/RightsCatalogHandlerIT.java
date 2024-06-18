/*
 * MIT License
 *
 * Copyright (c) 2024 Hydrologic Engineering Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cwms.cda.api.project;

import static cwms.cda.api.Controllers.APPLICATION_MASK;
import static cwms.cda.api.Controllers.OFFICE_MASK;
import static cwms.cda.api.Controllers.PROJECT_MASK;
import static cwms.cda.api.Controllers.USER_ID;
import static cwms.cda.data.dao.DaoTest.getDslContext;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.flogger.FluentLogger;
import cwms.cda.api.Controllers;
import cwms.cda.api.DataApiTestIT;
import cwms.cda.data.dao.DeleteRule;
import cwms.cda.data.dao.project.ProjectDao;
import cwms.cda.data.dao.project.ProjectLockDao;
import cwms.cda.data.dto.Location;
import cwms.cda.data.dto.project.Lock;
import cwms.cda.data.dto.project.LockRevokerRights;
import cwms.cda.data.dto.project.Project;
import cwms.cda.formatters.Formats;
import fixtures.CwmsDataApiSetupCallback;
import fixtures.TestAccounts;
import io.restassured.filter.log.LogDetail;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import mil.army.usace.hec.test.database.CwmsDatabaseContainer;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

public class RightsCatalogHandlerIT extends DataApiTestIT {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public static final String OFFICE = "SPK";


    @Test
    void test_rights_catalog() throws SQLException {
        CwmsDatabaseContainer<?> databaseLink = CwmsDataApiSetupCallback.getDatabaseLink();
        databaseLink.connection(c -> {
            DSLContext dsl = getDslContext(c, OFFICE);
            ProjectLockDao lockDao = new ProjectLockDao(dsl);
            ProjectDao prjDao = new ProjectDao(dsl);

            String projId = "catRightsIT";
            String appId = "test_catRights";
            String officeMask = OFFICE;

            Project testProject = buildTestProject(OFFICE, projId);
            prjDao.create(testProject);

            TestAccounts.KeyUser user = TestAccounts.KeyUser.SPK_NORMAL;
            String userName = user.getName();

            try {
                lockDao.removeAllLockRevokerRights(OFFICE, userName, appId, officeMask); // start fresh
                lockDao.updateLockRevokerRights(OFFICE, userName, projId, appId, officeMask, true);


                given()
                        .log().ifValidationFails(LogDetail.ALL, true)
                        .accept(Formats.JSON)
                        .queryParam(OFFICE_MASK, OFFICE)
                        .queryParam(PROJECT_MASK, projId)
                        .queryParam(APPLICATION_MASK, appId)
                    .when()
                        .redirects().follow(true)
                        .redirects().max(3)
                        .get("/project-lock-rights/")
                    .then()
                        .log().ifValidationFails(LogDetail.ALL, true)
                    .assertThat()
                        .statusCode(is(HttpServletResponse.SC_OK))
                        .body("$.size()", is(1))
                        .body("[0].office-id", equalTo(OFFICE))
                        .body("[0].project-id", equalTo(projId))
                        .body("[0].application-id", equalToIgnoringCase(appId))  // actually lowercases it.
                        .body("[0].user-id", equalTo(userName))
                ;


                // Now deny
                lockDao.updateLockRevokerRights(OFFICE, userName, projId, appId, officeMask, false);

                given()
                        .log().ifValidationFails(LogDetail.ALL, true)
                        .accept(Formats.JSON)
                        .queryParam(OFFICE_MASK, OFFICE)
                        .queryParam(PROJECT_MASK, projId)
                        .queryParam(APPLICATION_MASK, appId)
                    .when()
                        .redirects().follow(true)
                        .redirects().max(3)
                        .get("/project-lock-rights/")
                    .then()
                        .log().ifValidationFails(LogDetail.ALL, true)
                    .assertThat()
                        .body("$.size()", is(0))
                        .statusCode(is(HttpServletResponse.SC_OK))
                ;

            } finally {
                lockDao.removeAllLockRevokerRights(OFFICE, userName, appId, officeMask);
                deleteProject(prjDao, projId, lockDao, appId);
            }
        });

    }


    @Test
    void test_removeAll_rights() throws SQLException {
        CwmsDatabaseContainer<?> databaseLink = CwmsDataApiSetupCallback.getDatabaseLink();
        databaseLink.connection(c -> {
            DSLContext dsl = getDslContext(c, OFFICE);
            ProjectLockDao lockDao = new ProjectLockDao(dsl);
            ProjectDao prjDao = new ProjectDao(dsl);

            String projId = "remAllIT";
            String appId = "test_remAll";
            String officeMask = OFFICE;

            Project testProject = buildTestProject(OFFICE, projId);
            prjDao.create(testProject);

            TestAccounts.KeyUser user = TestAccounts.KeyUser.SPK_NORMAL;
            String userName = user.getName();

            try {

                // first removeALL
                given()
                        .log().ifValidationFails(LogDetail.ALL, true)
                        .accept(Formats.JSON)
                        .header("Authorization", user.toHeaderValue())
                        .queryParam(OFFICE_MASK, officeMask)
                        .queryParam(Controllers.OFFICE, OFFICE)
                        .queryParam(USER_ID, userName)
                        .queryParam(APPLICATION_MASK, appId)
                    .when()
                        .redirects().follow(true)
                        .redirects().max(3)
                        .post("/project-lock-rights/remove-all")
                    .then()
                        .log().ifValidationFails(LogDetail.ALL, true)
                    .assertThat()
                        .statusCode(is(HttpServletResponse.SC_OK))
                ;

                // Add an allow
                lockDao.updateLockRevokerRights(OFFICE, userName, projId, appId, officeMask, true);

                // make sure its there.
                List<LockRevokerRights> lockRevokerRights = lockDao.catLockRevokerRights(projId, appId, OFFICE);
                assertNotNull(lockRevokerRights);
                assertFalse(lockRevokerRights.isEmpty());

                // Now remove all again
                given()
                        .log().ifValidationFails(LogDetail.ALL, true)
                        .accept(Formats.JSON)
                        .header("Authorization", user.toHeaderValue())
                        .queryParam(Controllers.OFFICE, OFFICE)
                        .queryParam(OFFICE_MASK, officeMask)
                        .queryParam(USER_ID, userName)
                        .queryParam(APPLICATION_MASK, appId)
                    .when()
                        .redirects().follow(true)
                        .redirects().max(3)
                        .post("/project-lock-rights/remove-all")
                    .then()
                        .log().ifValidationFails(LogDetail.ALL, true)
                    .assertThat()
                        .statusCode(is(HttpServletResponse.SC_OK))
                ;

                // make sure its gone.
                lockRevokerRights = lockDao.catLockRevokerRights(projId, appId, OFFICE);
                assertNotNull(lockRevokerRights);
                assertTrue(lockRevokerRights.isEmpty());


            } finally {
                lockDao.removeAllLockRevokerRights(OFFICE, userName, appId, officeMask);
                deleteProject(prjDao, projId, lockDao, appId);
            }
        });

    }


        private static Project buildTestProject(String office, String prjId) {
            Location pbLoc = new Location.Builder(office,prjId + "-PB")
                    .withTimeZoneName(ZoneId.of("UTC"))
                    .withActive(null)
                    .build();
            Location ngLoc = new Location.Builder(office,prjId + "-NG")
                    .withTimeZoneName(ZoneId.of("UTC"))
                    .withActive(null)
                    .build();

            Location prjLoc = new Location.Builder(office, prjId)
                    .withTimeZoneName(ZoneId.of("UTC"))
                    .withActive(null)
                    .build();

            return new Project.Builder()
                    .withLocation(prjLoc)
                    .withProjectOwner("Project Owner")
                    .withAuthorizingLaw("Authorizing Law")
                    .withFederalCost(100.0)
                    .withNonFederalCost(50.0)
                    .withFederalOAndMCost(10.0)
                    .withNonFederalOAndMCost(5.0)
                    .withCostYear(Instant.now())
                    .withCostUnit("$")
                    .withYieldTimeFrameEnd(Instant.now())
                    .withYieldTimeFrameStart(Instant.now())
                    .withFederalOAndMCost(10.0)
                    .withNonFederalOAndMCost(5.0)
                    .withProjectRemarks("Remarks")
                    .withPumpBackLocation(pbLoc)
                    .withNearGageLocation(ngLoc)
                    .withBankFullCapacityDesc("Bank Full Capacity Description")
                    .withDownstreamUrbanDesc("Downstream Urban Description")
                    .withHydropowerDesc("Hydropower Description")
                    .withSedimentationDesc("Sedimentation Description")
                    .build();

        }

    private void deleteProject(ProjectDao prjDao, String projId, ProjectLockDao lockDao, String appId) {
        try {
            prjDao.delete(OFFICE, projId, DeleteRule.DELETE_ALL);
        } catch (Exception e) {
            logger.at(Level.WARNING).withCause(e).log("Failed to delete project: %s", projId);
            List<Lock> locks = lockDao.catLocks(projId, appId, TimeZone.getTimeZone("UTC"), OFFICE);
            locks.forEach(lock -> {
                logger.atFine().log("Remaining Locks: " + lock.getProjectId() + " " +
                        lock.getApplicationId() + " " + lock.getAcquireTime() + " " +
                        lock.getSessionUser() + " " + lock.getOsUser() + " " +
                        lock.getSessionProgram() + " " + lock.getSessionMachine());
            });
        }
    }

}
