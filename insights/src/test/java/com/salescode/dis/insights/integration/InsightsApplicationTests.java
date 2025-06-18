package com.salescode.dis.insights.integration;

import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.enums.ProgressStage;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
@ActiveProfiles({"postgres", "dev", "info", "kafka"})
//@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Use PER_CLASS if Kafka setup is expensive
class InsightsApplicationTests {

    // Make static if using TestInstance.Lifecycle.PER_CLASS
    @Container
//    static final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    // --- Test Constants ---
    private static final String KAFKA_TOPIC = "file-progress-updates-test";

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
//        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
//        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        // Optional: Add other dynamic properties if needed (e.g., database from Testcontainers)
        registry.add("file-status-scheduler.stale-threshold-seconds", () -> 60);
        registry.add("file-status-scheduler.too-old-threshold-seconds", () -> 600);
        registry.add("file-status-scheduler.rate-millis", () -> 5_000);
    }

    @BeforeAll
    static void setupKafkaTopic() {
//     //    Optional: Pre-create topic if auto-creation is disabled or unreliable
//         try (AdminClient adminClient = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()))) {
//             adminClient.createTopics(Collections.singletonList(new NewTopic(KAFKA_TOPIC, 1, (short) 1)));
//             System.out.println("Kafka topic created: " + KAFKA_TOPIC);
//         } catch (Exception e) {
//             System.err.println("Failed to create Kafka topic: " + e.getMessage());
//         }
    }


    @Test
    void testKafkaEventsUpdateFileAndJobProgress() throws Exception { // Added throws Exception
        String TEST_GROUP_ID = "test-job-" + UUID.randomUUID(); // Unique Job ID per test run
        String TEST_FILE_ID = "test-file-" + UUID.randomUUID(); // Unique File ID per test run

        // Expected counts for each stage
        final int PUBLISH_SUCCESS = 10;
        final int READ_SUCCESS = 5;
        final int QUEUE_SUCCESS = 7;
        final int PROCESS_SUCCESS = 12;
        final int SAVE_SUCCESS = 8;

        final int FAIL_COUNT = 1; // Example failure count for some stages

        // Arrange: Prepare data generator for all stages
        DataGenerator dataGenerator = DataGenerator.builder()
                .fileId(TEST_FILE_ID)
                .jobId(TEST_GROUP_ID)
                .masterName(TEST_MASTER_NAME)
                .stagesToGenerate(List.of(ProgressStage.READ, ProgressStage.PUBLISH, ProgressStage.QUEUE, ProgressStage.PROCESS, ProgressStage.SAVE))
                .eventSuppliers(Map.of(
//                        ProgressStage.READ, () -> createProgressRequest(ProgressStage.READ, READ_SUCCESS, 0, 10, 100),
//                        ProgressStage.PUBLISH, () -> createProgressRequest(ProgressStage.PUBLISH, PUBLISH_SUCCESS, FAIL_COUNT, 20, 200),
                        ProgressStage.QUEUE, () -> createProgressRequest(ProgressStage.QUEUE, QUEUE_SUCCESS, 0, 30, 300),
                        ProgressStage.PROCESS, () -> createProgressRequest(ProgressStage.PROCESS, PROCESS_SUCCESS, FAIL_COUNT, 40, 400),
                        ProgressStage.SAVE, () -> createProgressRequest(ProgressStage.SAVE, SAVE_SUCCESS, 0, 50, 500)
                ))
                .kafkaTemplate(kafkaTemplate)
                .topic(KAFKA_TOPIC)
                .build();

        Map<ProgressStage, List<FileProgressEvent>> result;

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Map<ProgressStage, List<FileProgressEvent>>> submit = executorService.submit(dataGenerator);
            result = submit.get(30, TimeUnit.SECONDS);
            assertNotNull(result, "Data generation result should not be null");

            // Verify the number of events sent for each stage
//            assertEquals(READ_SUCCESS, result.getOrDefault(ProgressStage.READ, List.of()).size());
//            assertEquals(PUBLISH_SUCCESS, result.getOrDefault(ProgressStage.PUBLISH, List.of()).size());
            assertEquals(QUEUE_SUCCESS, result.getOrDefault(ProgressStage.QUEUE, List.of()).size());
            assertEquals(PROCESS_SUCCESS + FAIL_COUNT, result.getOrDefault(ProgressStage.PROCESS, List.of()).size());
            assertEquals(SAVE_SUCCESS, result.getOrDefault(ProgressStage.SAVE, List.of()).size());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Waiting for Kafka events to be processed...");
        Awaitility.await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            FileEntity file = fileRepository.findByFileIdAndMaster(TEST_FILE_ID, TEST_MASTER_NAME).orElse(null);
            assertNotNull(file, "FileEntity should exist");

            // Assertions for all stages
//            assertStageMetrics(file, ProgressStage.READ, READ_SUCCESS, 0, 10, 100);
//            assertStageMetrics(file, ProgressStage.PUBLISH, PUBLISH_SUCCESS, FAIL_COUNT, 20, 200);
            assertStageMetrics(file, ProgressStage.QUEUE, QUEUE_SUCCESS, 0, 30, 300);
            assertStageMetrics(file, ProgressStage.PROCESS, PROCESS_SUCCESS, FAIL_COUNT, 40, 400);
            assertStageMetrics(file, ProgressStage.SAVE, SAVE_SUCCESS, 0, 50, 500);

            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertNotNull(job, "JobEntity should exist");
        });

        System.out.println("Awaitility condition met. Performing final assertions.");

        FileEntity finalFile = fileService.get(TEST_FILE_ID, TEST_MASTER_NAME);
        JobEntity finalJob = jobService.getJob(TEST_GROUP_ID);

        assertNotNull(finalFile, "Final FileEntity check failed");
        assertNotNull(finalJob, "Final JobEntity check failed");

        assertStageMetrics(finalFile, ProgressStage.READ, READ_SUCCESS, 0, 10, 100);
        assertStageMetrics(finalFile, ProgressStage.PUBLISH, PUBLISH_SUCCESS, FAIL_COUNT, 20, 200);
        assertStageMetrics(finalFile, ProgressStage.QUEUE, QUEUE_SUCCESS, 0, 30, 300);
        assertStageMetrics(finalFile, ProgressStage.PROCESS, PROCESS_SUCCESS, FAIL_COUNT, 40, 400);
        assertStageMetrics(finalFile, ProgressStage.SAVE, SAVE_SUCCESS, 0, 50, 500);

        assertEquals(TEST_GROUP_ID, finalFile.getJob().getId(), "File should be linked to the correct job");
        assertEquals(TEST_LOB, finalJob.getLob(), "Job LOB mismatch");

        Awaitility.await().atMost(Duration.ofSeconds(300)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertEquals(ProgressStatus.COMPLETED, job.getStatus());
        });

        fileRepository.deleteById(TEST_FILE_ID);
        jobRepo.deleteById(TEST_GROUP_ID);
    }

    @Test
    void testKafkaEventsUpdateMultipleFilesAndJobProgress() throws Exception {
        String TEST_GROUP_ID = "test-job-" + UUID.randomUUID();
        String TEST_FILE_ID_1 = "test-file-1-" + UUID.randomUUID();
        String TEST_FILE_ID_2 = "test-file-2-" + UUID.randomUUID();

        final int PUBLISH_SUCCESS_PER_FILE = 5;
        final int READ_SUCCESS_PER_FILE = 3;
        final int QUEUE_SUCCESS_PER_FILE = 4;
        final int PROCESS_SUCCESS_PER_FILE = 6;
        final int SAVE_SUCCESS_PER_FILE = 2;

        final int FAIL_COUNT_PER_FILE = 1;

        // Arrange: Prepare data generators for two files, covering all stages
        List<ProgressStage> allStages = List.of(ProgressStage.READ, ProgressStage.PUBLISH, ProgressStage.QUEUE, ProgressStage.PROCESS, ProgressStage.SAVE);

        DataGenerator dataGenerator1 = DataGenerator.builder()
                .fileId(TEST_FILE_ID_1)
                .jobId(TEST_GROUP_ID)
                .masterName(TEST_MASTER_NAME)
                .stagesToGenerate(allStages)
                .eventSuppliers(Map.of(
                        ProgressStage.READ, () -> createProgressRequest(ProgressStage.READ, READ_SUCCESS_PER_FILE, 0, 10, 100),
                        ProgressStage.PUBLISH, () -> createProgressRequest(ProgressStage.PUBLISH, PUBLISH_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 20, 200),
                        ProgressStage.QUEUE, () -> createProgressRequest(ProgressStage.QUEUE, QUEUE_SUCCESS_PER_FILE, 0, 30, 300),
                        ProgressStage.PROCESS, () -> createProgressRequest(ProgressStage.PROCESS, PROCESS_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 40, 400),
                        ProgressStage.SAVE, () -> createProgressRequest(ProgressStage.SAVE, SAVE_SUCCESS_PER_FILE, 0, 50, 500)
                ))
                .kafkaTemplate(kafkaTemplate)
                .topic(KAFKA_TOPIC)
                .build();

        DataGenerator dataGenerator2 = DataGenerator.builder()
                .fileId(TEST_FILE_ID_2)
                .jobId(TEST_GROUP_ID)
                .masterName(TEST_MASTER_NAME)
                .stagesToGenerate(allStages)
                .eventSuppliers(Map.of(
                        ProgressStage.READ, () -> createProgressRequest(ProgressStage.READ, READ_SUCCESS_PER_FILE, 0, 11, 101),
                        ProgressStage.PUBLISH, () -> createProgressRequest(ProgressStage.PUBLISH, PUBLISH_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 21, 201),
                        ProgressStage.QUEUE, () -> createProgressRequest(ProgressStage.QUEUE, QUEUE_SUCCESS_PER_FILE, 0, 31, 301),
                        ProgressStage.PROCESS, () -> createProgressRequest(ProgressStage.PROCESS, PROCESS_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 41, 401),
                        ProgressStage.SAVE, () -> createProgressRequest(ProgressStage.SAVE, SAVE_SUCCESS_PER_FILE, 0, 51, 501)
                ))
                .kafkaTemplate(kafkaTemplate)
                .topic(KAFKA_TOPIC)
                .build();

        Map<ProgressStage, List<FileProgressEvent>> result1;
        Map<ProgressStage, List<FileProgressEvent>> result2;

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Map<ProgressStage, List<FileProgressEvent>>> submit1 = executorService.submit(dataGenerator1);
            Future<Map<ProgressStage, List<FileProgressEvent>>> submit2 = executorService.submit(dataGenerator2);

            result1 = submit1.get(15, TimeUnit.SECONDS);
            result2 = submit2.get(15, TimeUnit.SECONDS);

            assertNotNull(result1, "Data generation result for file 1 should not be null");
            assertNotNull(result2, "Data generation result for file 2 should not be null");

            // Verify the number of events sent for each stage for file 1
            assertEquals(READ_SUCCESS_PER_FILE, result1.getOrDefault(ProgressStage.READ, List.of()).size());
            assertEquals(PUBLISH_SUCCESS_PER_FILE, result1.getOrDefault(ProgressStage.PUBLISH, List.of()).size());
            assertEquals(QUEUE_SUCCESS_PER_FILE, result1.getOrDefault(ProgressStage.QUEUE, List.of()).size());
            assertEquals(PROCESS_SUCCESS_PER_FILE, result1.getOrDefault(ProgressStage.PROCESS, List.of()).size());
            assertEquals(SAVE_SUCCESS_PER_FILE, result1.getOrDefault(ProgressStage.SAVE, List.of()).size());

            // Verify the number of events sent for each stage for file 2
            assertEquals(READ_SUCCESS_PER_FILE, result2.getOrDefault(ProgressStage.READ, List.of()).size());
            assertEquals(PUBLISH_SUCCESS_PER_FILE, result2.getOrDefault(ProgressStage.PUBLISH, List.of()).size());
            assertEquals(QUEUE_SUCCESS_PER_FILE, result2.getOrDefault(ProgressStage.QUEUE, List.of()).size());
            assertEquals(PROCESS_SUCCESS_PER_FILE, result2.getOrDefault(ProgressStage.PROCESS, List.of()).size());
            assertEquals(SAVE_SUCCESS_PER_FILE, result2.getOrDefault(ProgressStage.SAVE, List.of()).size());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Waiting for Kafka events to be processed for multiple files...");
        Awaitility.await().atMost(Duration.ofSeconds(300)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            FileEntity file1 = fileRepository.findByFileIdAndMaster(TEST_FILE_ID_1, TEST_MASTER_NAME).orElse(null);
            assertNotNull(file1, "FileEntity 1 should exist");
            assertStageMetrics(file1, ProgressStage.READ, READ_SUCCESS_PER_FILE, 0, 10, 100);
            assertStageMetrics(file1, ProgressStage.PUBLISH, PUBLISH_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 20, 200);
            assertStageMetrics(file1, ProgressStage.QUEUE, QUEUE_SUCCESS_PER_FILE, 0, 30, 300);
            assertStageMetrics(file1, ProgressStage.PROCESS, PROCESS_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 40, 400);
            assertStageMetrics(file1, ProgressStage.SAVE, SAVE_SUCCESS_PER_FILE, 0, 50, 500);

            FileEntity file2 = fileRepository.findByFileIdAndMaster(TEST_FILE_ID_2, TEST_MASTER_NAME).orElse(null);
            assertNotNull(file2, "FileEntity 2 should exist");
            assertStageMetrics(file2, ProgressStage.READ, READ_SUCCESS_PER_FILE, 0, 11, 101);
            assertStageMetrics(file2, ProgressStage.PUBLISH, PUBLISH_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 21, 201);
            assertStageMetrics(file2, ProgressStage.QUEUE, QUEUE_SUCCESS_PER_FILE, 0, 31, 301);
            assertStageMetrics(file2, ProgressStage.PROCESS, PROCESS_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 41, 401);
            assertStageMetrics(file2, ProgressStage.SAVE, SAVE_SUCCESS_PER_FILE, 0, 51, 501);

            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertNotNull(job, "JobEntity should exist");
        });

        System.out.println("Awaitility condition met for multiple files. Performing final assertions.");

        FileEntity finalFile1 = fileService.get(TEST_FILE_ID_1, TEST_MASTER_NAME);
        FileEntity finalFile2 = fileService.get(TEST_FILE_ID_2, TEST_MASTER_NAME);
        JobEntity finalJob = jobService.getJob(TEST_GROUP_ID);

        assertNotNull(finalFile1, "Final FileEntity 1 check failed");
        assertNotNull(finalFile2, "Final FileEntity 2 check failed");
        assertNotNull(finalJob, "Final JobEntity check failed");

        assertStageMetrics(finalFile1, ProgressStage.READ, READ_SUCCESS_PER_FILE, 0, 10, 100);
        assertStageMetrics(finalFile1, ProgressStage.PUBLISH, PUBLISH_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 20, 200);
        assertStageMetrics(finalFile1, ProgressStage.QUEUE, QUEUE_SUCCESS_PER_FILE, 0, 30, 300);
        assertStageMetrics(finalFile1, ProgressStage.PROCESS, PROCESS_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 40, 400);
        assertStageMetrics(finalFile1, ProgressStage.SAVE, SAVE_SUCCESS_PER_FILE, 0, 50, 500);

        assertStageMetrics(finalFile2, ProgressStage.READ, READ_SUCCESS_PER_FILE, 0, 11, 101);
        assertStageMetrics(finalFile2, ProgressStage.PUBLISH, PUBLISH_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 21, 201);
        assertStageMetrics(finalFile2, ProgressStage.QUEUE, QUEUE_SUCCESS_PER_FILE, 0, 31, 301);
        assertStageMetrics(finalFile2, ProgressStage.PROCESS, PROCESS_SUCCESS_PER_FILE, FAIL_COUNT_PER_FILE, 41, 401);
        assertStageMetrics(finalFile2, ProgressStage.SAVE, SAVE_SUCCESS_PER_FILE, 0, 51, 501);

        assertEquals(TEST_GROUP_ID, finalFile1.getJob().getId(), "File 1 should be linked to the correct job");
        assertEquals(TEST_GROUP_ID, finalFile2.getJob().getId(), "File 2 should be linked to the correct job");

        assertEquals(TEST_LOB, finalJob.getLob(), "Job LOB mismatch");

        Awaitility.await().atMost(Duration.ofSeconds(300)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            JobEntity job = jobService.getJob(TEST_GROUP_ID);
            assertEquals(ProgressStatus.COMPLETED, job.getStatus());
        });

        fileRepository.deleteById(TEST_FILE_ID_1);
        fileRepository.deleteById(TEST_FILE_ID_2);
        jobRepo.deleteById(TEST_GROUP_ID);
    }


    // Helper method to create FileProgressRequest for any stage
    private FileProgressRequest createProgressRequest(ProgressStage stage, long success, long failure, int minProcessingTime, int maxProcessingTime) {
        FileProgressRequest fileProgressRequest = new FileProgressRequest();
        fileProgressRequest.setStageName(stage);
        fileProgressRequest.setSuccessCount(success);
        fileProgressRequest.setFailureCount(failure);
        fileProgressRequest.setMinProcessingTimeMs(minProcessingTime);
        fileProgressRequest.setMaxProcessingTimeMs(maxProcessingTime);
        return fileProgressRequest;
    }

    // Helper method to assert stage metrics
    private void assertStageMetrics(FileEntity file, ProgressStage stage, long expectedSuccess, long expectedFailure, Integer expectedMinTime, Integer expectedMaxTime) {
        Optional<FileStageMetrics> metrics = file.getFileStageMetrics().stream()
                .filter(m -> stage.equals(m.getProgressStage()))
                .findFirst();
        assertTrue(metrics.isPresent(), String.format("Stage %s metrics not found", stage.name()));
        assertEquals(expectedSuccess, metrics.get().getSuccessCount(), String.format("Stage %s success count mismatch", stage.name()));
        assertEquals(expectedFailure, metrics.get().getFailureCount(), String.format("Stage %s failure count mismatch", stage.name()));
        assertEquals(expectedMinTime, metrics.get().getMinProcessingTimeMs(), String.format("Stage %s min processing time mismatch", stage.name()));
        assertEquals(expectedMaxTime, metrics.get().getMaxProcessingTimeMs(), String.format("Stage %s max processing time mismatch", stage.name()));
    }

    @Data
    @Builder
    public static class DataGenerator implements Callable<Map<ProgressStage, List<FileProgressEvent>>> {

        private final String fileId;
        private final String masterName;
        private final String jobId;
        private final List<ProgressStage> stagesToGenerate;
        private final Map<ProgressStage, Supplier<FileProgressRequest>> eventSuppliers;
        private final KafkaTemplate<String, FileProgressEvent> kafkaTemplate;
        private final String topic;

        @Override
        public Map<ProgressStage, List<FileProgressEvent>> call() throws Exception {
            Map<ProgressStage, List<FileProgressEvent>> sentEvents = new HashMap<>();

            for (ProgressStage stage : stagesToGenerate) {
                List<FileProgressEvent> stageEvents = new ArrayList<>();
                Supplier<FileProgressRequest> supplier = eventSuppliers.get(stage);
                if (supplier == null) {
                    System.err.println("No event supplier found for stage: " + stage);
                    continue;
                }
                // Determine how many events to send for this stage based on the successCount in the request
                FileProgressRequest sampleRequest = supplier.get();
                long numEventsToSend = sampleRequest.getSuccessCount() + sampleRequest.getFailureCount();

                CompletableFuture<?>[] futures = new CompletableFuture[(int)numEventsToSend];
                for (int i = 0; i < numEventsToSend; i++) {
                    FileProgressEvent event = createEvent(supplier.get());
                    futures[i] = kafkaTemplate.send(topic, fileId, event)
                            .thenAccept(result -> stageEvents.add(event))
                            .exceptionally(ex -> {
                                System.err.println("Failed to send event for stage " + stage + ": " + ex.getMessage());
                                return null;
                            });
                }
                CompletableFuture.allOf(futures).join();
                sentEvents.put(stage, stageEvents);
            }

            return sentEvents;
        }

        FileProgressEvent createEvent(FileProgressRequest fileProgressRequest) {
            FileProgressEvent fileProgressEvent = new FileProgressEvent();
            fileProgressEvent.setEventId(UUID.randomUUID().toString());
            fileProgressEvent.setJobId(jobId);
            fileProgressEvent.setFileId(fileId);
            fileProgressEvent.setLob(TEST_LOB);
            fileProgressEvent.setMasterName(TEST_MASTER_NAME);
            fileProgressEvent.setProgress(fileProgressRequest);
            return fileProgressEvent;
        }
    }
}