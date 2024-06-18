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

import static com.codahale.metrics.MetricRegistry.name;
import static cwms.cda.api.Controllers.GET_ALL;
import static cwms.cda.api.Controllers.REVOKE_EXISTING;
import static cwms.cda.api.Controllers.REVOKE_TIMEOUT;
import static cwms.cda.api.Controllers.STATUS_200;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.flogger.FluentLogger;
import cwms.cda.api.Controllers;
import cwms.cda.api.errors.CdaError;
import cwms.cda.data.dao.JooqDao;
import cwms.cda.data.dao.project.ProjectLockDao;
import cwms.cda.data.dto.project.Id;
import cwms.cda.data.dto.project.Lock;
import cwms.cda.formatters.ContentType;
import cwms.cda.formatters.Formats;
import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;

public class LockRequest implements Handler {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final MetricRegistry metrics;
    private final Histogram requestResultSize;

    private Timer.Context markAndTime(String subject) {
        return Controllers.markAndTime(metrics, getClass().getName(), subject);
    }

    public LockRequest(MetricRegistry metrics) {
        this.metrics = metrics;
        requestResultSize = this.metrics.histogram((name(LockRequest.class, Controllers.RESULTS,
                Controllers.SIZE)));
    }


    @OpenApi(
            description = "Requests the creation of a new Reservoir Project Lock",
            requestBody = @OpenApiRequestBody(
                    description = "Users must provide a Lock object specifying the projectId,"
                            + " applicationId and officeId. Other fields will be ignored.",
                    content = {
                        @OpenApiContent(from = Lock.class, type = Formats.JSON),
                    },
                    required = true),
            queryParams = {
                @OpenApiParam(name = REVOKE_EXISTING, type = Boolean.class,
                        description = "If an existing lock is found should a revoke be "
                                + "attempted? Default: false"),
                @OpenApiParam(name = REVOKE_TIMEOUT, type = Integer.class,
                        description = "time in seconds to wait for existing lock to be "
                                + "revoked. Default: 10")
            },
            responses = {
                @OpenApiResponse(status = STATUS_200, content = {
                    @OpenApiContent(type = Formats.JSON, from = Id.class)}
                )},
            method = HttpMethod.POST,
            path = "/project-locks/",
            tags = {"Project Locks"}
    )
    @Override
    public void handle(@NotNull Context ctx) throws Exception {

        try (Timer.Context timer = markAndTime(GET_ALL)) {
            ProjectLockDao lockDao = new ProjectLockDao(JooqDao.getDslContext(ctx));

            String reqContentType = ctx.req.getContentType();
            String formatHeader = reqContentType != null ? reqContentType : Formats.JSON;
            ContentType contentType = Formats.parseHeader(formatHeader, Lock.class);
            Lock lock = Formats.parseContent(contentType, ctx.bodyAsInputStream(), Lock.class);
            boolean revokeExisting =
                    ctx.queryParamAsClass(REVOKE_EXISTING, Boolean.class).getOrDefault(false);
            int revokeTimeout =
                    ctx.queryParamAsClass(REVOKE_TIMEOUT, Integer.class).getOrDefault(10);

            String lockId = lockDao.requestLock(lock.getOfficeId(), lock.getProjectId(), lock.getApplicationId(),
                    revokeExisting, revokeTimeout);
            if (lockId != null) {
                Id id = new Id(lockId);
                String acceptHeader = ctx.header(Header.ACCEPT);
                ContentType responseType = Formats.parseHeader(acceptHeader, Id.class);
                String result = Formats.format(responseType, id);
                ctx.result(result);
                ctx.contentType(responseType.toString());
                requestResultSize.update(result.length());
                ctx.status(HttpServletResponse.SC_CREATED);
            } else {
                // We don't have any idea exactly why the create failed - right?
                // we could try and see if its already locked.

                boolean alreadyLocked = lockDao.isLocked(lock.getOfficeId(), lock.getProjectId(),
                        lock.getApplicationId());

                // Might be able to see if we have revoker rights.
                String userId = getUser(ctx).orElse(null);
                lockDao.hasLockRevokerRights(lock.getOfficeId(), userId, lock.getProjectId(), lock.getApplicationId()
                );

                // or see what the locs are:
                List<Lock> locks = lockDao.catLocks(lock.getProjectId(), lock.getApplicationId(),
                        TimeZone.getTimeZone("UTC"), lock.getOfficeId());

                ctx.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                CdaError re =
                        new CdaError("Requested lock was not retrieved. Already locked: "
                                + alreadyLocked + ", locks: " + locks, true);
                ctx.json(re);
            }

        }


    }

    private static Optional<String> getUser(Context ctx) {
        Optional<String> retval = Optional.empty();
        if (ctx != null && ctx.req != null && ctx.req.getUserPrincipal() != null) {
            retval = Optional.of(ctx.req.getUserPrincipal().getName());
        } else {
            logger.atFine().log("No user principal found in request.");
        }
        return retval;
    }

}
