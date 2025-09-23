package com.salescode.dis.insights.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.salescode.dis.insights.InsightsApplication;
import com.salescode.dis.insights.entity.FileReportEntity;
import com.salescode.dis.insights.entity.InsightsMetadata;
import com.salescode.dis.insights.repository.FileReportRepository;
import com.salescode.dis.insights.repository.InsightsMetadataRepository;
import com.salescode.dis.insights.sse.SSEService;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;

import java.net.URL;
import java.util.*;

@Service
public class S3ExportService {

    private static final Logger logger = LoggerFactory.getLogger(S3ExportService.class);
    private static final int CHUNK_SIZE = 5000;

    private final S3Client s3Client;
    private final String bucketName;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;
    private final FileReportRepository fileReportRepository;
    private final SSEService sseService;
    private final S3Presigner s3Presigner;
    private final InsightsMetadataRepository metadataRepository;
    private static final String default_sql = """
            SELECT
                features,
                responses,
                fileid,
                lob,
                entity_name,
                "timestamp",
                recordstatus
            FROM ex_schema_dataintegration.integration_failure
            WHERE fileid = ? AND lob = ? AND entity_name = ?
            """;

    private static final String countSql = """
        SELECT COUNT(*)
        FROM ex_schema_dataintegration.integration_failure
        WHERE fileid = ?
        AND lob = ?
        AND entity_name = ?;
    """;

    public S3ExportService(
            S3Client s3Client,
            @Value("${spring.aws.s3.bucket-name}") String bucketName,
            @Value("${spring.redshift.jdbc-url}") String jdbcUrl,
            @Value("${spring.redshift.username}") String username,
            @Value("${spring.redshift.password}") String password,
            @Value("${spring.redshift.driver-class-name}") String driverClassName,
            FileReportRepository fileReportRepository,
            SSEService sseService,
            S3Presigner s3Presigner,
            InsightsMetadataRepository metadataRepository

    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.fileReportRepository = fileReportRepository;
        this.sseService = sseService;
        this.s3Presigner = s3Presigner;
        this.metadataRepository = metadataRepository;

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(5);
        this.jdbcTemplate = new JdbcTemplate(ds);
    }


    @Async()
    public void exportFailuresAsync(String fileId,String lob,String entity) {
        try {
            long timeoutMillis = 30 * 60 * 1000L;
            String fileUrl = exportFailures(fileId,lob,entity);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                    .withZone(ZoneId.of("UTC"));
            String formattedTime = formatter.format(Instant.now());
            String name = String.format("file-%s-%s.csv",fileId,formattedTime);
            fileReportRepository.findByFileId(fileId).ifPresentOrElse(r -> {
                r.setUrl(fileUrl);
                r.setName(name);
                r.setStatus("COMPLETED");
                r.setErrorMessage(null);
                FileReportEntity saved = fileReportRepository.save(r);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("id", saved.getId() != null ? saved.getId().toString() : null);
                payload.put("fileId", saved.getFileId());
                payload.put("url", saved.getUrl());
                payload.put("name", saved.getName());
                payload.put("status", saved.getStatus());
                sseService.broadcastReportEvent(fileId, "report-update", payload);

            }, () -> {
                // fallback if no entity exists
                sseService.broadcastReportEvent(fileId, "report-update", Map.of(
                        "fileId", fileId,
                        "message", "Completed but fileReport entry not found"
                ));
            });

        } catch (Exception ex) {
            Throwable rootCause = ex;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            String detailedErrorMessage = rootCause.getClass().getSimpleName() + ": " + rootCause.getMessage();

            Optional<FileReportEntity> updatedReport = fileReportRepository.findByFileId(fileId).map(r -> {
                r.setStatus("FAILED");
                r.setName(null);
                r.setErrorMessage(detailedErrorMessage);
                return fileReportRepository.save(r);
            });

            if (updatedReport.isPresent()) {
                sseService.broadcastReportEvent(fileId, "report-update", updatedReport.get());
            } else {
                sseService.broadcastReportEvent(fileId, "report-update", Map.of(
                        "fileId", fileId,
                        "message", "Failed but fileReport entry not found"
                ));
            }
            sseService.broadcastReportEvent(fileId, "error", Map.of("error", detailedErrorMessage));
            sseService.completeReportEmitters(fileId);
        }
    }

    public String exportFailures(String fileId,String lob,String entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.of("UTC"));

        String formattedTime = formatter.format(Instant.now());

        String s3Key = String.format("dataintegration/insights_reports/file-%s-%s.csv",
                fileId, formattedTime);
        logger.info("Starting multipart upload for fileId '{}' to S3 key 's3://{}/{}'", fileId, bucketName, s3Key);

        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("text/csv")
                .build();
        CreateMultipartUploadResponse initiatedUpload = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = initiatedUpload.uploadId();
        logger.info("Multipart upload initiated with uploadId: {}", uploadId);

        List<CompletedPart> completedParts = new ArrayList<>();
        int partNumber = 1;
        boolean isFirstChunk = true;
        List<String> csvHeaders = null;
        ByteArrayOutputStream currentPartData = new ByteArrayOutputStream();

        final long MIN_PART_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
        int totalExported = 0;
        Instant lastTimestamp = null;

        try {
            Long totalRecords = 0L;
            try {
                totalRecords = jdbcTemplate.queryForObject(
                        countSql,
                        new Object[]{fileId, lob, entity},
                        Long.class
                );
            } catch (Exception exCount) {
                logger.warn("Could not fetch total record count for fileId '{}'. Will fallback to sending raw exported counts. Reason: {}", fileId, exCount.getMessage());
                totalRecords = 0L;
            }
            while (true) {

                String baseSql = metadataRepository.findByKey("redshift_query")
                        .map(InsightsMetadata::getValue)
                        .orElse(default_sql);

                StringBuilder sqlBuilder = new StringBuilder();
                List<Object> queryParams = new ArrayList<>(List.of(fileId,lob,entity));


                sqlBuilder.append("SELECT * FROM (")
                        .append(baseSql)
                        .append(") sub");

                if (lastTimestamp != null) {
                    sqlBuilder.append(" WHERE sub.\"timestamp\" > ?");
                    queryParams.add(Timestamp.from(lastTimestamp));
                }

                sqlBuilder.append(" ORDER BY sub.\"timestamp\"");
                sqlBuilder.append(" LIMIT ?");
                queryParams.add(CHUNK_SIZE);

                String finalSql = sqlBuilder.toString();

                List<Map<String, Object>> rawRecords = jdbcTemplate.query(
                        finalSql,
                        queryParams.toArray(),
                        new ColumnMapRowMapper()
                );

                if (rawRecords.isEmpty()) {
                    logger.info("No more records found for fileId '{}'. Exiting fetch loop.", fileId);
                    break;
                }

                Map<String, Object> lastRecord = rawRecords.get(rawRecords.size() - 1);
                Object timestampObj = lastRecord.get("timestamp");
                if (timestampObj instanceof Timestamp) {
                    lastTimestamp = ((Timestamp) timestampObj).toInstant();
                } else {
                    // Handle cases where the timestamp might be in a different format if necessary
                    logger.warn("Timestamp format not as expected. Could not update lastTimestamp.");
                    break; // Or handle more gracefully
                }

                List<Map<String, Object>> processedRecords = processAndFlattenRecords(rawRecords);

                if (processedRecords.isEmpty()) {
                    continue;
                }


                CsvMapper csvMapper = new CsvMapper();
                byte[] csvBytes;

                if (isFirstChunk) {
                    csvHeaders = new ArrayList<>(processedRecords.get(0).keySet());
                    CsvSchema schemaWithHeader = buildCsvSchema(csvHeaders, true);
                    csvBytes = csvMapper.writer(schemaWithHeader).writeValueAsBytes(processedRecords);
                    isFirstChunk = false;
                } else {
                    CsvSchema schemaWithoutHeader = buildCsvSchema(csvHeaders, false);
                    csvBytes = csvMapper.writer(schemaWithoutHeader).writeValueAsBytes(processedRecords);
                }

                currentPartData.write(csvBytes);

                if (currentPartData.size() >= MIN_PART_SIZE_BYTES) {
                    byte[] partBytes = currentPartData.toByteArray();
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .uploadId(uploadId)
                            .partNumber(partNumber)
                            .build();

                    UploadPartResponse partResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(partBytes));

                    completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(partResponse.eTag()).build());
                    logger.info("Successfully uploaded part #{} ({} bytes) for uploadId '{}'. ETag: {}", partNumber, partBytes.length, uploadId, partResponse.eTag());

                    currentPartData.reset();
                    partNumber++;
                }

            totalExported += processedRecords.size();
                
             sendReportProgress(totalExported,totalRecords,partNumber,fileId,lastTimestamp);


            }

            if (currentPartData.size() > 0) {
                byte[] finalPartBytes = currentPartData.toByteArray();
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();

                UploadPartResponse partResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(finalPartBytes));

                completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(partResponse.eTag()).build());
                logger.info("Successfully uploaded final part #{} ({} bytes) for uploadId '{}'. ETag: {}", partNumber, finalPartBytes.length, uploadId, partResponse.eTag());
            }

            if (completedParts.isEmpty()) {
                abortMultipartUpload(s3Key, uploadId);
                logger.warn("No records found for fileId '{}'. Aborted multipart upload.", fileId);

                throw new RuntimeException("No records found for fileId. Aborted multipart upload."+ fileId);

//                return "No records found to export for the given fileId.";
            }

            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();

            s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            logger.info("Successfully completed multipart upload for file '{}'.", s3Key);

            GetUrlRequest getUrlRequest = GetUrlRequest.builder().bucket(bucketName).key(s3Key).build();
            URL fileUrl = s3Client.utilities().getUrl(getUrlRequest);
            sseService.broadcastReportEvent(fileId, "report-update", Map.of(
                    "fileUrl", fileUrl.toString(),
                    "parts", completedParts.size(),
                    "s3Key", s3Key
            ));

            return fileUrl.toString();

        } catch (Exception e) {
            logger.error("Error during multipart S3 upload for fileId '{}'. Aborting upload.", fileId, e);
            if (uploadId != null) {
                abortMultipartUpload(s3Key,uploadId);
            }
            throw new RuntimeException("Failed to export data to S3 due to an internal error.", e);

        }
    }


    private void abortMultipartUpload(String s3Key, String uploadId) {
        try {
            logger.warn("Aborting multipart upload with ID: {}", uploadId);
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .uploadId(uploadId)
                    .build());
        } catch (Exception e) {
            logger.error("Failed to abort multipart upload. Orphaned parts may exist.", e);
        }
    }

    private List<Map<String, Object>> processAndFlattenRecords(List<Map<String, Object>> rawRecords) {
        List<Map<String, Object>> processedList = new ArrayList<>();
        TypeReference<List<Map<String, Object>>> typeRef = new TypeReference<>() {};

        for (Map<String, Object> rawRecord : rawRecords) {
            Map<String, Object> newRecord = new LinkedHashMap<>();

            Object featuresObj = rawRecord.get("features");
            if (featuresObj instanceof String) {
                try {
                    List<Map<String, Object>> featuresList = objectMapper.readValue((String) featuresObj, typeRef);
                    if (!featuresList.isEmpty()) featuresList.get(0).forEach(newRecord::put);
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing 'features'. Skipping.", e);
                }
            }

            String errorMessage = "";
            String recordStatus = rawRecord.get("recordstatus") != null ? rawRecord.get("recordstatus").toString() : "unknown";

            Object responsesObj = rawRecord.get("responses");
            if (responsesObj instanceof String) {
                try {
                    List<Map<String, Object>> responsesList = objectMapper.readValue((String) responsesObj, typeRef);
                    if (!responsesList.isEmpty() && responsesList.get(0).get("message") != null) {
                        errorMessage = responsesList.get(0).get("message").toString();
                    }
                } catch (JsonProcessingException e) {
                    logger.error("Error parsing 'responses'.", e);
                    errorMessage = "Error parsing response data";
                }
            }

            newRecord.put("status", recordStatus);
            newRecord.put("errorMessage", errorMessage);
            processedList.add(newRecord);
        }
        return processedList;
    }

    private CsvSchema buildCsvSchema(List<String> headers, boolean withHeader) {
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        for (String header : headers) schemaBuilder.addColumn(header);
        return withHeader ? schemaBuilder.build().withHeader() : schemaBuilder.build().withoutHeader();
    }

    private void sendReportProgress(int totalExported,Long totalRecords,int partNumber,String fileId,Instant lastTimestamp){
        if (totalRecords != null && totalRecords > 0) {
                int percent = (int) Math.min(100, (totalExported * 100) / totalRecords);
                sseService.broadcastReportEvent(fileId, "report-progress", Map.of(
                        "fileId", fileId,
                        "exported", totalExported,
                        "percentage", percent,
                        "lastTimestamp", lastTimestamp.toString(),
                        "partsUploaded", partNumber - 1
                ));

        } else {
                sseService.broadcastReportEvent(fileId, "report-progress", Map.of(
                        "fileId", fileId,
                        "exported", totalExported,
                        "lastTimestamp", lastTimestamp.toString(),
                        "partsUploaded", partNumber - 1
                ));
            }

    }


    public URL generatePresignedUrl(String s3Path, long expirationMillis) {
        try {
            logger.info("Generating presigned URL for path: {} using AWS SDK v2", s3Path);

            URI uri = new URI(s3Path);
            String host = uri.getHost();

            int firstDotIndex = host.indexOf('.');
            if (firstDotIndex == -1) {
                throw new IllegalArgumentException("Invalid S3 URL: Cannot determine bucket name from host: " + host);
            }
            String bucketName = host.substring(0, firstDotIndex);
            String objectKey = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMillis(expirationMillis))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);
            URL url = presignedGetObjectRequest.url();

            logger.info("Successfully generated v2 presigned URL for key: {}", objectKey);
            return url;

        } catch (Exception e) {
            logger.error("Failed to generate v2 presigned URL for path: {}", s3Path, e);
            throw new RuntimeException("Error generating presigned URL. See server logs for details.", e);
        }
    }

}


