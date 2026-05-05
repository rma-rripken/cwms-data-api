package cwms.cda.api;

import static com.codahale.metrics.MetricRegistry.name;
import static cwms.cda.api.Controllers.*;
import static cwms.cda.data.dto.CwmsDTOPaginated.CURSOR_CHECK;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import cwms.cda.api.errors.CdaError;
import cwms.cda.data.dao.BlobAccess;
import cwms.cda.data.dao.BlobDao;
import cwms.cda.data.dao.JooqDao;
import cwms.cda.data.dao.ObjectStorageBlobDao;
import cwms.cda.data.dao.ObjectStorageConfig;
import cwms.cda.data.dao.StreamConsumer;
import cwms.cda.data.dto.Blob;
import cwms.cda.data.dto.Blobs;
import cwms.cda.features.CdaFeatures;
import cwms.cda.formatters.ContentType;
import cwms.cda.formatters.Formats;
import cwms.cda.formatters.FormattingException;
import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiFormParam;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.manager.FeatureManager;


/**
 *
 */
public class BlobController extends BaseCrudHandler {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String TAG = "Blob";

    public BlobController(MetricRegistry metrics) {
        super(metrics);
    }

    protected DSLContext getDslContext(Context ctx) {
        return JooqDao.getDslContext(ctx);
    }

    private BlobAccess chooseBlobAccess(DSLContext dsl) {
        boolean useObjectStore = false;
        try {
            FeatureManager featureManager = FeatureContext.getFeatureManager();
            useObjectStore = featureManager.isActive(CdaFeatures.USE_OBJECT_STORAGE_BLOBS);
        } catch (Throwable ignore) {
            // fall back to system/env property check
        }
        if (useObjectStore) {
            ObjectStorageConfig cfg = ObjectStorageConfig.fromSystem();
            return new ObjectStorageBlobDao(cfg);
        }
        return new BlobDao(dsl);
    }


    @OpenApi(
            queryParams = {
                    @OpenApiParam(name = OFFICE,
                            description = "Specifies the owning office. If this field is not "
                                    + "specified, matching information from all offices shall be "
                                    + "returned."),
                    @OpenApiParam(name = PAGE,
                            description = "This end point can return a lot of data, this "
                                    + "identifies where in the request you are. This is an opaque"
                                    + " value, and can be obtained from the 'next-page' value in "
                                    + "the response."),
                    @OpenApiParam(name = CURSOR, deprecated = true,
                            description = "This end point can return a lot of data, this "
                                    + "identifies where in the request you are. This is an opaque"
                                    + " value, and can be obtained from the 'next-page' value in "
                                    + "the response. Deprecated, use " + PAGE + " instead."),
                    @OpenApiParam(name = PAGE_SIZE,
                            type = Integer.class,
                            description = "How many entries per page returned. Default "
                                    + DEFAULT_PAGE_SIZE + "."),
                    @OpenApiParam(name = LIKE,
                            description = "Posix <a href=\"regexp.html\">regular expression</a> "
                                    + "describing the blob id's you want")
            },
            responses = {@OpenApiResponse(status = STATUS_200,
                    description = "A list of blobs.",
                    content = {
                            @OpenApiContent(type = Formats.JSON, from = Blobs.class),
                            @OpenApiContent(type = Formats.JSONV2, from = Blobs.class),
                    })
            },
            tags = {TAG}
    )
    @Override
    public void getAll(@NotNull Context ctx) {

        try (final Timer.Context ignored = markAndTime(GET_ALL)) {
            DSLContext dsl = getDslContext(ctx);
            String office = ctx.queryParam(OFFICE);

            String cursor = queryParamAsClass(ctx, new String[]{PAGE, CURSOR},
                    String.class, "", getMetrics(), name(BlobController.class.getName(), GET_ALL));

            if (Boolean.TRUE.equals(CURSOR_CHECK.invoke(cursor))) {
                int pageSize = queryParamAsClass(ctx, new String[]{PAGE_SIZE},
                        Integer.class, DEFAULT_PAGE_SIZE, getMetrics(),
                        name(BlobController.class.getName(), GET_ALL));

                String like = ctx.queryParamAsClass(LIKE, String.class).getOrDefault(".*");

                String formatHeader = ctx.header(Header.ACCEPT);
                ContentType contentType = Formats.parseHeader(formatHeader, Blobs.class);

                BlobAccess dao = chooseBlobAccess(dsl);
                Blobs blobs = dao.getBlobs(cursor, pageSize, office, like);

                String result = Formats.format(contentType, blobs);

                ctx.result(result);
                ctx.contentType(contentType.toString());
                updateResultSize(result.length());
            } else {
                ctx.json(new CdaError("cursor or page passed in but failed validation"))
                        .status(HttpCode.BAD_REQUEST);
            }

        }
    }

    @OpenApi(
            description = "Returns the binary value of the requested blob as a seekable stream with the "
                    + "appropriate media type.",
            pathParams = {
                    @OpenApiParam(name = BLOB_ID, description = "If the _query_ parameter is provided this _path_ parameter "
                            + "is ignored and the value of the query parameter is used.   "
                            + "Note: the _query_ parameter is necessary for id's that contain '/' or other special "
                            + "characters. This is due to limitations in path pattern matching. "
                            + "We will likely add support for encoding the ID in the path in the future. For now use the id field for those IDs. "
                            + "Client libraries should detect slashes and choose the appropriate field. \"ignored\" is suggested for the path endpoint."),
            },
            queryParams = {
                    @OpenApiParam(name = OFFICE, description = "Specifies the owning office."),
                    @OpenApiParam(name = BLOB_ID, description = "If this _query_ parameter is provided the id _path_ parameter "
                            + "is ignored and the value of the query parameter is used.   "
                            + "Note: this query parameter is necessary for id's that contain '/' or other special "
                            + "characters. This is due to limitations in path pattern matching. "
                            + "We will likely add support for encoding the ID in the path in the future. For now use the id field for those IDs. "
                            + "Client libraries should detect slashes and choose the appropriate field. \"ignored\" is suggested for the path endpoint."),
            },
            responses = {
                    @OpenApiResponse(status = STATUS_200,
                            description = "Returns requested blob.",
                            content = {
                                    @OpenApiContent(type = "application/octet-stream", from = byte[].class)
                            })
            },
            tags = {TAG}
    )
    @Override
    public void getOne(@NotNull Context ctx, @NotNull String blobId) {

        try (final Timer.Context ignored = markAndTime(GET_ONE)) {
            String idQueryParam = ctx.queryParam(BLOB_ID);
            if (idQueryParam != null) {
                blobId = idQueryParam;
            }
            DSLContext dsl = getDslContext(ctx);

            BlobAccess dao = chooseBlobAccess(dsl);
            String officeQP = ctx.queryParam(OFFICE);
            Optional<String> office = Optional.ofNullable(officeQP);


            final Long offset;
            final Long end;
            long[] ranges = RangeParser.parseFirstRange(ctx.header(io.javalin.core.util.Header.RANGE));
            if (ranges != null) {
                offset = ranges[0];
                end = ranges[1];
            } else {
                offset = null;
                end = null;
            }

            ctx.header(Header.ACCEPT_RANGES, "bytes");

            StreamConsumer consumer = (is, isPosition, mediaType, totalLength) -> {
                if (is == null) {
                    ctx.status(HttpServletResponse.SC_NOT_FOUND).json(new CdaError("Unable to find "
                            + "blob based on given parameters"));
                } else {
                    updateResultSize(totalLength);
                    // is  OracleBlobInputStream or something from MinIO
                    RangeRequestUtil.seekableStream(ctx, is, isPosition, mediaType, totalLength);
                }
            };

            if (office.isPresent()) {
                dao.getBlob(blobId, office.get(), consumer, offset, end);
            } else {
                dao.getBlob(blobId, null, consumer, offset, end);
            }
        }
    }


    @OpenApi(
            description = "Create new Blob",
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = Blob.class, type = Formats.JSONV2),
                            @OpenApiContent(from = Blob.class, type = Formats.JSON),
                            @OpenApiContent(type = MULTIPART_FORM_DATA)
                    },
                    required = true),
            queryParams = {
                    @OpenApiParam(name = FAIL_IF_EXISTS, type = Boolean.class,
                            description = "Create will fail if provided ID already exists. Default: true")
            },
            formParams = {
                    @OpenApiFormParam(name = "office-id"),
                    @OpenApiFormParam(name = "id"),
                    @OpenApiFormParam(name = "description"),
                    @OpenApiFormParam(name = "media-type-id"),
                    @OpenApiFormParam(name = "value", type = File.class)
            },
            method = HttpMethod.POST,
            tags = {TAG}
    )
    @Override
    public void create(@NotNull Context ctx) {
        try (final Timer.Context ignored = markAndTime(CREATE)) {
            DSLContext dsl = getDslContext(ctx);
            String reqContentType = ctx.req.getContentType();
            String formatHeader = reqContentType != null ? reqContentType : Formats.JSON;
            boolean failIfExists = ctx.queryParamAsClass(FAIL_IF_EXISTS, Boolean.class).getOrDefault(true);

            Blob blob;
            if (formatHeader.toLowerCase(Locale.ROOT).startsWith(MULTIPART_FORM_DATA)) {
                blob = parseMultipartBlob(ctx);
            } else {
                ContentType contentType = Formats.parseHeader(formatHeader, Blob.class);
                blob = Formats.parseContent(contentType, ctx.bodyAsInputStream(), Blob.class);
            }

            BlobAccess dao = chooseBlobAccess(dsl);
            dao.create(blob, failIfExists, false);
            ctx.status(HttpCode.CREATED);
        }
    }

    @OpenApi(
            description = "Update an existing Blob",
            pathParams = {
                    @OpenApiParam(name = BLOB_ID, description = "The blob identifier to be updated"),
            },
            requestBody = @OpenApiRequestBody(
                    content = {
                            @OpenApiContent(from = Blob.class, type = Formats.JSONV2),
                            @OpenApiContent(from = Blob.class, type = Formats.JSON),
                            @OpenApiContent(type = MULTIPART_FORM_DATA)
                    },
                    required = true),
            queryParams = {
                    @OpenApiParam(name = BLOB_ID, description = "If this _query_ parameter is provided the id _path_ parameter "
                            + "is ignored and the value of the query parameter is used.   "
                            + "Note: this query parameter is necessary for id's that contain '/' or other special "
                            + "characters. This is due to limitations in path pattern matching. "
                            + "We will likely add support for encoding the ID in the path in the future. For now use the id field for those IDs. "
                            + "Client libraries should detect slashes and choose the appropriate field. \"ignored\" is suggested for the path endpoint."),
            },
            formParams = {
                    @OpenApiFormParam(name = "office-id"),
                    @OpenApiFormParam(name = "id"),
                    @OpenApiFormParam(name = "description"),
                    @OpenApiFormParam(name = "media-type-id"),
                    @OpenApiFormParam(name = "value", type = File.class)
            },
            method = HttpMethod.PATCH,
            tags = {TAG}
    )
    @Override
    public void update(@NotNull Context ctx, @NotNull String blobId) {
        logUnusedPathParameter(ctx, BLOB_ID, "Body contains information");

        try (final Timer.Context ignored = markAndTime(UPDATE)) {
            String idQueryParam = ctx.queryParam(BLOB_ID);
            if (idQueryParam != null) {
                blobId = idQueryParam;
            }
            DSLContext dsl = getDslContext(ctx);

            String reqContentType = ctx.req.getContentType();
            String formatHeader = reqContentType != null ? reqContentType : Formats.JSON;

            Blob blob;
            if (formatHeader.toLowerCase(Locale.ROOT).startsWith(MULTIPART_FORM_DATA)) {
                blob = parseMultipartBlob(ctx);
            } else {
                ContentType contentType = Formats.parseHeader(formatHeader, Blob.class);
                blob = Formats.parseContent(contentType, ctx.bodyAsInputStream(), Blob.class);
            }

            if (blob.getOfficeId() == null) {
                throw new FormattingException("An officeId is required when updating a blob");
            }

            if (blob.getId() == null) {
                throw new FormattingException("An Id is required when updating a blob");
            }

            if (blob.getValue() == null) {
                throw new FormattingException("A non-empty value field is required when "
                        + "updating a blob");
            }

            if (!blob.getId().equals(blobId)) {
                throw new FormattingException("The blob id parameter does not match the blob id in the body. " +
                        "The blob end-point does not support renaming blobs.  " +
                        "Create a new blob with the new id and delete the old one.");
            }

            BlobAccess dao = chooseBlobAccess(dsl);
            dao.update(blob, false);
            ctx.status(HttpServletResponse.SC_OK);
        }
    }

    private Blob parseMultipartBlob(Context ctx) {
        ParsedMultipart parsedMultipart = parseMultipart(ctx);

        String officeId = firstNonBlank(
                parsedMultipart.field("office-id"),
                parsedMultipart.field(OFFICE),
                ctx.formParam("office-id"),
                ctx.formParam(OFFICE));
        String id = firstNonBlank(
                parsedMultipart.field("id"),
                parsedMultipart.field(BLOB_ID),
                ctx.formParam("id"),
                ctx.formParam(BLOB_ID));
        String description = firstNonBlank(
                parsedMultipart.field("description"),
                ctx.formParam("description"));
        String mediaTypeId = firstNonBlank(
                parsedMultipart.field("media-type-id"),
                ctx.formParam("media-type-id")
                );

        byte[] value = parsedMultipart.value();
        if (value == null) {
            value = readMultipartValue(ctx);
        }
        return new Blob(officeId, id, description, mediaTypeId, value);
    }

    private ParsedMultipart parseMultipart(Context ctx) {
        String contentType = ctx.req.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith(MULTIPART_FORM_DATA)) {
            return new ParsedMultipart(Map.of(), null);
        }

        String boundaryToken = null;
        for (String part : contentType.split(";")) {
            String token = part.trim();
            if (token.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
                boundaryToken = token.substring("boundary=".length());
                break;
            }
        }

        if (boundaryToken == null || boundaryToken.isBlank()) {
            throw new FormattingException("Unable to parse multipart form data: missing boundary");
        }

        String boundary = boundaryToken;
        if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() > 1) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }

        try {
            byte[] bodyBytes = ctx.bodyAsInputStream().readAllBytes();
            return parseMultipartBody(bodyBytes, boundary);
        } catch (IOException e) {
            throw new FormattingException("Unable to parse multipart form data", e);
        }
    }

    private ParsedMultipart parseMultipartBody(byte[] bodyBytes, String boundary) {
        String payload = new String(bodyBytes, StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;
        String[] segments = payload.split(java.util.regex.Pattern.quote(delimiter));

        Map<String, String> fields = new HashMap<>();
        byte[] value = null;

        for (String rawSegment : segments) {
            String segment = normalizeSegment(rawSegment);
            if (segment != null) {
                PartData part = parsePartData(segment);
                if (part != null && part.name != null) {
                    byte[] partBytes = part.body.getBytes(StandardCharsets.ISO_8859_1);
                    if (isValuePart(part.name, part.filePart)) {
                        value = partBytes;
                    } else {
                        fields.put(part.name, new String(partBytes, StandardCharsets.UTF_8));
                    }
                }

            }

        }

        return new ParsedMultipart(fields, value);
    }

    private String normalizeSegment(String rawSegment) {
        if (rawSegment == null || rawSegment.isBlank() || rawSegment.startsWith("--")) {
            return null;
        }

        if (rawSegment.startsWith("\r\n")) {
            return rawSegment.substring(2);
        }
        return rawSegment;
    }

    private PartData parsePartData(String segment) {
        int headerSeparator = segment.indexOf("\r\n\r\n");
        if (headerSeparator < 0) {
            return null;
        }

        String headers = segment.substring(0, headerSeparator);
        String body = segment.substring(headerSeparator + 4);
        if (body.endsWith("\r\n")) {
            body = body.substring(0, body.length() - 2);
        }

        String name = extractPartName(headers);
        boolean filePart = hasFileName(headers);
        return new PartData(name, filePart, body);
    }

    private String extractPartName(String headers) {
        for (String headerLine : headers.split("\r\n")) {
            String lower = headerLine.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("content-disposition:")) {
                continue;
            }

            for (String dispositionPart : headerLine.split(";")) {
                String trimmed = dispositionPart.trim();
                if (trimmed.startsWith("name=")) {
                    return stripQuotes(trimmed.substring(5));
                }
            }
        }

        return null;
    }

    private boolean hasFileName(String headers) {
        for (String headerLine : headers.split("\r\n")) {
            String lower = headerLine.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("content-disposition:")) {
                continue;
            }

            for (String dispositionPart : headerLine.split(";")) {
                if (dispositionPart.trim().startsWith("filename=")) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isValuePart(String name, boolean filePart) {
        return filePart || "value".equalsIgnoreCase(name)
                || "blob".equalsIgnoreCase(name)
                || "file".equalsIgnoreCase(name)
                || "content".equalsIgnoreCase(name);
    }

    private String stripQuotes(String input) {
        if (input == null) {
            return null;
        }

        String value = input.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private byte[] readMultipartValue(Context ctx) {
        String valueText = ctx.formParam("value");
        if (valueText != null) {
            return valueText.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static final class ParsedMultipart {
        private final Map<String, String> fields;
        private final byte[] value;

        ParsedMultipart(Map<String, String> fields, byte[] value) {
            this.fields = fields;
            this.value = value;
        }

        String field(String name) {
            return fields.get(name);
        }

        byte[] value() {
            return value;
        }
    }

    private static final class PartData {
        private final String name;
        private final boolean filePart;
        private final String body;

        PartData(String name, boolean filePart, String body) {
            this.name = name;
            this.filePart = filePart;
            this.body = body;
        }
    }

    @OpenApi(
            description = "Deletes requested blob",
            pathParams = {
                    @OpenApiParam(name = BLOB_ID, description = "The blob identifier to be deleted"),
            },
            queryParams = {
                    @OpenApiParam(name = OFFICE, required = true, description = "Specifies the "
                            + "owning office of the blob to be deleted"),
                    @OpenApiParam(name = BLOB_ID, description = "If this _query_ parameter is provided the id _path_ parameter "
                            + "is ignored and the value of the query parameter is used.   "
                            + "Note: this query parameter is necessary for id's that contain '/' or other special "
                            + "characters. This is due to limitations in path pattern matching. "
                            + "We will likely add support for encoding the ID in the path in the future. For now use the id field for those IDs. "
                            + "Client libraries should detect slashes and choose the appropriate field. \"ignored\" is suggested for the path endpoint."),
            },
            method = HttpMethod.DELETE,
            tags = {TAG}
    )
    @Override
    public void delete(@NotNull Context ctx, @NotNull String blobId) {
        try (Timer.Context ignored = markAndTime(DELETE)) {
            String idQueryParam = ctx.queryParam(BLOB_ID);
            if (idQueryParam != null) {
                blobId = idQueryParam;
            }
            DSLContext dsl = getDslContext(ctx);
            String office = requiredParam(ctx, OFFICE);
            BlobAccess dao = chooseBlobAccess(dsl);
            dao.delete(office, blobId);
            ctx.status(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
