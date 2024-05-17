package cwms.cda.api;

import static com.codahale.metrics.MetricRegistry.name;
import static cwms.cda.api.Controllers.FORMAT;
import static cwms.cda.api.Controllers.GET_ALL;
import static cwms.cda.api.Controllers.GET_ONE;
import static cwms.cda.api.Controllers.NOT_SUPPORTED_YET;
import static cwms.cda.api.Controllers.RESULTS;
import static cwms.cda.api.Controllers.SIZE;
import static cwms.cda.api.Controllers.STATUS_200;
import static cwms.cda.api.Controllers.STATUS_501;
import static cwms.cda.data.dao.JooqDao.getDslContext;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import cwms.cda.api.errors.CdaError;
import cwms.cda.data.dao.TimeZoneDao;
import cwms.cda.formatters.Formats;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.jooq.DSLContext;

public class TimeZoneController implements CrudHandler {
    private static final Logger logger = Logger.getLogger(TimeZoneController.class.getName());

    private final MetricRegistry metrics;

    private final Histogram requestResultSize;

    public TimeZoneController(MetricRegistry metrics) {
        this.metrics = metrics;
        String className = this.getClass().getName();

        requestResultSize = this.metrics.histogram((name(className, RESULTS, SIZE)));
    }

    private Timer.Context markAndTime(String subject) {
        return Controllers.markAndTime(metrics, getClass().getName(), subject);
    }

    @OpenApi(ignore = true)
    @Override
    public void create(Context ctx) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @OpenApi(ignore = true)
    @Override
    public void delete(Context ctx, String id) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

    @OpenApi(
            queryParams = {
                    @OpenApiParam(name = FORMAT, required = false, description = "Specifies the"
                            + " encoding format of the response. Valid value for the format field"
                            + " for this URI are:"
                            + "\n* `tab`  "
                            + "\n* `csv`  "
                            + "\n* `xml`  "
                            + "\n* `json`  (default)")
            },
            responses = {
                    @OpenApiResponse(status = STATUS_200),
                    @OpenApiResponse(status = STATUS_501, description = "The format requested is not "
                            + "implemented")
            },
            tags = {"TimeZones"}
    )
    @Override
    public void getAll(Context ctx) {
        try (Timer.Context timeContext = markAndTime(GET_ALL)) {
            DSLContext dsl = getDslContext(ctx);
            TimeZoneDao dao = new TimeZoneDao(dsl);
            String format = ctx.queryParamAsClass(FORMAT, String.class).getOrDefault("json");

            switch (format) {
                case "json": {
                    ctx.contentType(Formats.JSON);
                    break;
                }
                case "tab": {
                    ctx.contentType(Formats.TAB);
                    break;
                }
                case "csv": {
                    ctx.contentType(Formats.CSV);
                    break;
                }
                case "xml": {
                    ctx.contentType(Formats.XML);
                    break;
                }
                case "wml2": {
                    ctx.contentType(Formats.WML2);
                    break;
                }
                default: {
                    ctx.status(HttpServletResponse.SC_NOT_IMPLEMENTED)
                            .json(CdaError.notImplemented());
                    return;
                }
            }

            String results = dao.getTimeZones(format);
            requestResultSize.update(results.length());
            ctx.status(HttpServletResponse.SC_OK);
            ctx.result(results);
            requestResultSize.update(results.length());
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            ctx.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ctx.result("Failed to process request");
        }
    }

    @OpenApi(ignore = true)
    @Override
    public void getOne(Context ctx, String id) {
        try (Timer.Context timeContext = markAndTime(GET_ONE)) {
            throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
        }
    }

    @OpenApi(ignore = true)
    @Override
    public void update(Context ctx, String id) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
    }

}
