package com.salescode.dis.insights.integration;

import com.salescode.dis.insights.dto.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.kafka.FileProgressEvent;
import com.salescode.dis.insights.repository.FileRepository;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.JobService;
import lombok.Builder;
import lombok.Data;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
//@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Use PER_CLASS if Kafka setup is expensive
class InsightsApplicationTests {

    // Make static if using TestInstance.Lifecycle.PER_CLASS
    @Container
    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    // --- Test Constants ---
    private static final String KAFKA_TOPIC = "file-progress-updates";

    private static final String TEST_LOB = "test-lob";
    private static final String TEST_MASTER_NAME = "test-masterName";

    @Autowired
    private KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
    @Autowired
    private FileService fileService;
    @Autowired
    private JobService jobService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private JobRepository jobRepo;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        // Optional: Add other dynamic properties if needed (e.g., database from Testcontainers)
        registry.add("file-status-scheduler.stale-threshold-seconds", () -> 10);
        registry.add("file-status-scheduler.too-old-threshold-seconds", () -> 100);
        registry.add("file-status-scheduler.rate-millis", () -> 5_000);
    }

    @BeforeAll
    static void setupKafkaTopic() {
        // Optional: Pre-create topic if auto-creation is disabled or unreliable
        // try (AdminClient adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {
        //     adminClient.createTopics(Collections.singletonList(new NewTopic(KAFKA_TOPIC, 1, (short) 1)));
        //     System.out.println("Kafka topic created: " + KAFKA_TOPIC);
        // } catch (Exception e) {
        //     System.err.println("Failed to create Kafka topic: " + e.getMessage());
        // }
    }


    @Test
    void testKafkaEventsUpdateFileAndJobProgress() throws Exception { // Added throws Exception
        String TEST_GROUP_ID = "test-job-" + UUID.randomUUID(); // Unique Job ID per test run
        String TEST_FILE_ID = "test-file-" + UUID.randomUUID(); // Unique File ID per test run

        int EXPECTED_PUBLISH_SUCCESS = 10;
        int EXPECTED_CONSUME_SUCCESS = 10;

        // Arrange: Prepare data generator
        DataGenerator dataGenerator = DataGenerator.builder()
                .fileId(TEST_FILE_ID)
                .jobId(TEST_GROUP_ID) // Pass Job ID to generator
                .countOfRecordsToPublish(EXPECTED_PUBLISH_SUCCESS)
                .countOfRecordsToConsume(EXPECTED_CONSUME_SUCCESS)
                .kafkaTemplate(kafkaTemplate)
                .topic(KAFKA_TOPIC)
                .publishEventSupplier(() -> createPublisherMetrics(1, 0))
                .consumeEventSupplier(() -> createConsumerMetrics(1, 0, 0))
                .build();

        Map<String, List<FileProgressEvent>> result;

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Map<String, List<FileProgressEvent>>> submit = executorService.submit(dataGenerator);
            // Wait for the submission task itself to complete (sending messages)
            result = submit.get(15, TimeUnit.SECONDS); // Add timeout to sending phase
            assertNotNull(result, "Data generation result should not be null");
            assertEquals(EXPECTED_PUBLISH_SUCCESS, result.getOrDefault("publishedEvents", List.of()).size());
            assertEquals(EXPECTED_CONSUME_SUCCESS, result.getOrDefault("consumedEvents", List.of()).size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Waiting for Kafka events to be processed...");
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            // Check FileEntity state
            FileEntity file = fileService.getOrReturnNull(TEST_FILE_ID);
            assertNotNull(file, "FileEntity should exist");
            assertTrue(file.getConsumedSuccessCount() >= EXPECTED_CONSUME_SUCCESS && file.getPublishedSuccessCount() >= EXPECTED_PUBLISH_SUCCESS, "File counts should reach expected values. Found Consumed=" + file.getConsumedSuccessCount() + ", Published=" + file.getPublishedSuccessCount());
            // Check JobEntity state
            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertNotNull(job, "JobEntity should exist");
            assertEquals(1, job.getTotalFileCount(), "Job total file count should be 1");
        });

        System.out.println("Awaitility condition met. Performing final assertions.");

        FileEntity finalFile = fileService.get(TEST_FILE_ID);
        JobEntity finalJob = jobService.getJob(TEST_GROUP_ID);

        // final verification
        assertNotNull(finalFile, "Final FileEntity check failed");
        assertNotNull(finalJob, "Final JobEntity check failed");
        assertEquals(EXPECTED_CONSUME_SUCCESS, finalFile.getConsumedSuccessCount(), "Final consumed success count mismatch");
        assertEquals(0, finalFile.getConsumedFailCount(), "Final consumed fail count should be 0");
        assertEquals(EXPECTED_PUBLISH_SUCCESS, finalFile.getPublishedSuccessCount(), "Final published success count mismatch");
        assertEquals(0, finalFile.getPublishedFailCount(), "Final published fail count should be 0");
        // Check total count for API-based files (set in updateProgress;
        assertEquals(EXPECTED_PUBLISH_SUCCESS, finalFile.getTotalCount(), "Final total count mismatch");
        assertEquals(TEST_GROUP_ID, finalFile.getJob().getId(), "File should be linked to the correct job");
        assertEquals(TEST_LOB, finalJob.getLob(), "Job LOB mismatch");
        assertEquals(TEST_MASTER_NAME, finalJob.getMaster(), "Job Master mismatch");

        Awaitility.await().atMost(Duration.ofSeconds(300)).pollInterval(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Optional: Add assertions for File/Job status if completion logic is tested
            FileEntity refreshedFile = fileService.get(TEST_FILE_ID);
            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertEquals(FileStatus.COMPLETED, refreshedFile.getConsumedStatus());
            assertEquals(FileStatus.COMPLETED, refreshedFile.getPublishedStatus());
            assertEquals(JobStatus.COMPLETED, job.getStatus());
        });

        // Cleanup: Delete the created file and job after the test
        fileRepository.deleteById(TEST_FILE_ID);
        jobRepo.deleteById(TEST_GROUP_ID);
    }

    @Test
    void testKafkaEventsUpdateMultipleFilesAndJobProgress() throws Exception {
        String TEST_GROUP_ID = "test-job-" + UUID.randomUUID();
        String TEST_FILE_ID_1 = "test-file-1-" + UUID.randomUUID();
        String TEST_FILE_ID_2 = "test-file-2-" + UUID.randomUUID();

        int EXPECTED_PUBLISH_SUCCESS_PER_FILE = 5;
        int EXPECTED_CONSUME_SUCCESS_PER_FILE = 5;

        // Arrange: Prepare data generators for two files
        DataGenerator dataGenerator1 = DataGenerator.builder()
                .fileId(TEST_FILE_ID_1)
                .jobId(TEST_GROUP_ID)
                .countOfRecordsToPublish(EXPECTED_PUBLISH_SUCCESS_PER_FILE)
                .countOfRecordsToConsume(EXPECTED_CONSUME_SUCCESS_PER_FILE)
                .kafkaTemplate(kafkaTemplate)
                .topic(KAFKA_TOPIC)
                .publishEventSupplier(() -> createPublisherMetrics(1, 0))
                .consumeEventSupplier(() -> createConsumerMetrics(1, 0, 0))
                .build();

        DataGenerator dataGenerator2 = DataGenerator.builder()
                .fileId(TEST_FILE_ID_2)
                .jobId(TEST_GROUP_ID)
                .countOfRecordsToPublish(EXPECTED_PUBLISH_SUCCESS_PER_FILE)
                .countOfRecordsToConsume(EXPECTED_CONSUME_SUCCESS_PER_FILE)
                .kafkaTemplate(kafkaTemplate)
                .topic(KAFKA_TOPIC)
                .publishEventSupplier(() -> createPublisherMetrics(1, 0))
                .consumeEventSupplier(() -> createConsumerMetrics(1, 0, 0))
                .build();

        Map<String, List<FileProgressEvent>> result1;
        Map<String, List<FileProgressEvent>> result2;

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Map<String, List<FileProgressEvent>>> submit1 = executorService.submit(dataGenerator1);
            Future<Map<String, List<FileProgressEvent>>> submit2 = executorService.submit(dataGenerator2);

            result1 = submit1.get(15, TimeUnit.SECONDS);
            result2 = submit2.get(15, TimeUnit.SECONDS);

            assertNotNull(result1, "Data generation result for file 1 should not be null");
            assertNotNull(result2, "Data generation result for file 2 should not be null");

            assertEquals(EXPECTED_PUBLISH_SUCCESS_PER_FILE, result1.getOrDefault("publishedEvents", List.of()).size());
            assertEquals(EXPECTED_CONSUME_SUCCESS_PER_FILE, result1.getOrDefault("consumedEvents", List.of()).size());
            assertEquals(EXPECTED_PUBLISH_SUCCESS_PER_FILE, result2.getOrDefault("publishedEvents", List.of()).size());
            assertEquals(EXPECTED_CONSUME_SUCCESS_PER_FILE, result2.getOrDefault("consumedEvents", List.of()).size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Waiting for Kafka events to be processed for multiple files...");
        Awaitility.await().atMost(Duration.ofSeconds(300)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            // Check FileEntity states
            FileEntity file1 = fileService.getOrReturnNull(TEST_FILE_ID_1);
            assertNotNull(file1, "FileEntity 1 should exist");
            assertTrue(file1.getConsumedSuccessCount() >= EXPECTED_CONSUME_SUCCESS_PER_FILE && file1.getPublishedSuccessCount() >= EXPECTED_PUBLISH_SUCCESS_PER_FILE, "File 1 counts should reach expected values. Found Consumed=" + file1.getConsumedSuccessCount() + ", Published=" + file1.getPublishedSuccessCount());

            FileEntity file2 = fileService.getOrReturnNull(TEST_FILE_ID_2);
            assertNotNull(file2, "FileEntity 2 should exist");
            assertTrue(file2.getConsumedSuccessCount() >= EXPECTED_CONSUME_SUCCESS_PER_FILE && file2.getPublishedSuccessCount() >= EXPECTED_PUBLISH_SUCCESS_PER_FILE, "File 2 counts should reach expected values. Found Consumed=" + file2.getConsumedSuccessCount() + ", Published=" + file2.getPublishedSuccessCount());

            // Check JobEntity state
            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertNotNull(job, "JobEntity should exist");
            assertEquals(2, job.getTotalFileCount(), "Job total file count should be 2");
        });

        System.out.println("Awaitility condition met for multiple files. Performing final assertions.");

        FileEntity finalFile1 = fileService.get(TEST_FILE_ID_1);
        FileEntity finalFile2 = fileService.get(TEST_FILE_ID_2);
        JobEntity finalJob = jobService.getJob(TEST_GROUP_ID);

        // final verification
        assertNotNull(finalFile1, "Final FileEntity 1 check failed");
        assertNotNull(finalFile2, "Final FileEntity 2 check failed");
        assertNotNull(finalJob, "Final JobEntity check failed");

        assertEquals(EXPECTED_CONSUME_SUCCESS_PER_FILE, finalFile1.getConsumedSuccessCount(), "Final consumed success count mismatch for file 1");
        assertEquals(0, finalFile1.getConsumedFailCount(), "Final consumed fail count should be 0 for file 1");
        assertEquals(EXPECTED_PUBLISH_SUCCESS_PER_FILE, finalFile1.getPublishedSuccessCount(), "Final published success count mismatch for file 1");
        assertEquals(0, finalFile1.getPublishedFailCount(), "Final published fail count should be 0 for file 1");
        assertEquals(EXPECTED_PUBLISH_SUCCESS_PER_FILE, finalFile1.getTotalCount(), "Final total count mismatch for file 1");
        assertEquals(TEST_GROUP_ID, finalFile1.getJob().getId(), "File 1 should be linked to the correct job");

        assertEquals(EXPECTED_CONSUME_SUCCESS_PER_FILE, finalFile2.getConsumedSuccessCount(), "Final consumed success count mismatch for file 2");
        assertEquals(0, finalFile2.getConsumedFailCount(), "Final consumed fail count should be 0 for file 2");
        assertEquals(EXPECTED_PUBLISH_SUCCESS_PER_FILE, finalFile2.getPublishedSuccessCount(), "Final published success count mismatch for file 2");
        assertEquals(0, finalFile2.getPublishedFailCount(), "Final published fail count should be 0 for file 2");
        assertEquals(EXPECTED_PUBLISH_SUCCESS_PER_FILE, finalFile2.getTotalCount(), "Final total count mismatch for file 2");
        assertEquals(TEST_GROUP_ID, finalFile2.getJob().getId(), "File 2 should be linked to the correct job");

        assertEquals(TEST_LOB, finalJob.getLob(), "Job LOB mismatch");
        assertEquals(TEST_MASTER_NAME, finalJob.getMaster(), "Job Master mismatch");

        Awaitility.await().atMost(Duration.ofSeconds(300)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            // Optional: Add assertions for File/Job status if completion logic is tested
            FileEntity refreshedFile1 = fileService.get(TEST_FILE_ID_1);
            FileEntity refreshedFile2 = fileService.get(TEST_FILE_ID_2);
            JobEntity job = jobService.getJob(TEST_GROUP_ID);

            assertEquals(FileStatus.COMPLETED, refreshedFile1.getConsumedStatus());
            assertEquals(FileStatus.COMPLETED, refreshedFile1.getPublishedStatus());

            assertEquals(FileStatus.COMPLETED, refreshedFile2.getConsumedStatus());
            assertEquals(FileStatus.COMPLETED, refreshedFile2.getPublishedStatus());

            assertEquals(JobStatus.COMPLETED, job.getStatus());
        });

        // Cleanup: Delete the created files and job after the test
        fileRepository.deleteById(TEST_FILE_ID_1);
        fileRepository.deleteById(TEST_FILE_ID_2);
        jobRepo.deleteById(TEST_GROUP_ID);
    }





    // Helper methods to create metrics DTOs (unchanged)
    FileProgressRequest createPublisherMetrics(int success, int failure) {
        FileProgressRequest.PublisherMetrics publisherMetrics = new FileProgressRequest.PublisherMetrics();
        publisherMetrics.setSuccessCount(success);
        publisherMetrics.setFailCount(failure);
        FileProgressRequest fileProgressRequest = new FileProgressRequest();
        fileProgressRequest.setPublisher(publisherMetrics);
        return fileProgressRequest;
    }

    FileProgressRequest createConsumerMetrics(int success, int serverFail, int logicalFail) {
        FileProgressRequest.ConsumerMetrics consumerMetrics = new FileProgressRequest.ConsumerMetrics();
        consumerMetrics.setSuccessCount(success);
        consumerMetrics.setServerFailCount(serverFail);
        consumerMetrics.setLogicalFailCount(logicalFail);

        FileProgressRequest fileProgressRequest = new FileProgressRequest();
        fileProgressRequest.setConsumer(consumerMetrics);
        return fileProgressRequest;
    }

    // Inner DataGenerator class (minor improvements)
    @Data
    @Builder
    public static class DataGenerator implements Callable<Map<String, List<FileProgressEvent>>> {

        private final String fileId;
        private final String jobId; // Added Job ID
        private final int countOfRecordsToPublish;
        private final int countOfRecordsToConsume;
        private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
        private final String topic; // Added topic
        private final Supplier<FileProgressRequest> publishEventSupplier;
        private final Supplier<FileProgressRequest> consumeEventSupplier;

        @Override
        public Map<String, List<FileProgressEvent>> call() throws Exception { // Added throws
            List<FileProgressEvent> publishedEvents = new ArrayList<>();
            List<FileProgressEvent> consumedEvents = new ArrayList<>();

            // Publish events
            CompletableFuture<?>[] publishFutures = new CompletableFuture[countOfRecordsToPublish];
            for (int i = 0; i < countOfRecordsToPublish; i++) {
                FileProgressEvent event = createEvent(publishEventSupplier.get());
                publishFutures[i] = kafkaTemplate.send(topic, fileId, event)
                        .thenAccept(result -> publishedEvents.add(event)) // Add on success
                        .exceptionally(ex -> {
                            System.err.println("Failed to send publish event: " + ex.getMessage());
                            return null; // Handle exception
                        });
            }
            CompletableFuture.allOf(publishFutures).join(); // Wait for all sends to complete

            // Consume events
            CompletableFuture<?>[] consumeFutures = new CompletableFuture[countOfRecordsToConsume];
            for (int i = 0; i < countOfRecordsToConsume; i++) {
                FileProgressEvent event = createEvent(consumeEventSupplier.get());
                consumeFutures[i] = kafkaTemplate.send(topic, fileId, event)
                        .thenAccept(result -> consumedEvents.add(event)) // Add on success
                        .exceptionally(ex -> {
                            System.err.println("Failed to send consume event: " + ex.getMessage());
                            return null; // Handle exception
                        });
            }
            CompletableFuture.allOf(consumeFutures).join(); // Wait for all sends to complete

            // Basic check if sends were successful (could be more robust)
            if (publishedEvents.size() != countOfRecordsToPublish || consumedEvents.size() != countOfRecordsToConsume) {
                System.err.println("Warning: Not all Kafka messages may have been sent successfully.");
            }

            return Map.of("publishedEvents", publishedEvents, "consumedEvents", consumedEvents);
        }

        // Creates the event object
        FileProgressEvent createEvent(FileProgressRequest fileProgressRequest) {
            FileProgressEvent fileProgressEvent = new FileProgressEvent();
            fileProgressEvent.setEventId(UUID.randomUUID().toString());
            fileProgressEvent.setJobId(jobId); // Use jobId passed to builder
            fileProgressEvent.setFileId(fileId);
            fileProgressEvent.setLob(TEST_LOB); // Use constants
            fileProgressEvent.setMasterName(TEST_MASTER_NAME); // Use constants
            fileProgressEvent.setProgress(fileProgressRequest);
            return fileProgressEvent;
        }
    }
}