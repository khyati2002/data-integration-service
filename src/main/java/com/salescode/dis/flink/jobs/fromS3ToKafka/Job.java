package com.salescode.dis.flink.jobs.fromS3ToKafka;

import java.time.Duration;
import java.util.Properties;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestartStrategyOptions;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineFormat;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * This is just an example not a working code
 */

@Component
public class Job {

    @Value("${app.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.producer-txn-timeout}")
    private String producerTxnTimeOut;

    @Value("${app.jobs.from-s3-to-kafka.bucket-name}")
    private String bucketName;

    @Value("${app.jobs.from-s3-to-kafka.file-key}")
    private String fileKey;

    @Value("${app.jobs.from-s3-to-kafka.topic}")
    private String kafkaTopic;

    @Value("${app.jobs.from-s3-to-kafka.dead-letter-topic}")
    private String deadLetterTopic;

    @Value("${app.jobs.from-s3-to-kafka.flink.restart.strategy}")
    private String restartStrategy;

    @Value("${app.jobs.from-s3-to-kafka.flink.restart.attempts}")
    private int restartAttempts;

    @Value("${app.jobs.from-s3-to-kafka.flink.restart.delay-seconds}")
    private int restartDelaySeconds;
  
    @Value("${app.jobs.from-s3-to-kafka.flink.parallelism.main-topic}")
    private int parallelismMainTopic;

    @Value("${app.jobs.from-s3-to-kafka.flink.parallelism.dead-letter-topic}")
    private int parallelismDeadLetterTopic;
    
    // private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);

    public void executeJob() throws Exception {
        // LOGGER.info("Starting job: FromS3ToKafka");

        // LOGGER.info("Initializing Flink job with restart attempts: {} and delay: {} seconds", jobConfig.getRestartAttempts(), jobConfig.getRestartDelaySeconds());
        Configuration config = new Configuration();
        config.set(RestartStrategyOptions.RESTART_STRATEGY, restartStrategy);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS, restartAttempts);
        config.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY, Duration.ofSeconds(restartDelaySeconds));
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);

        
        // LOGGER.info("Reading data from S3 bucket: {}, file: {}", jobConfig.getBucketName(), jobConfig.getFileKey());
        // Use Flink's FileSource API to read from S3
        FileSource<String> source = FileSource.forRecordStreamFormat(new TextLineFormat(), new Path("s3://" + bucketName + "/" + fileKey)).build();

        // Create DataStream from FileSource
        DataStream<String> s3DataStream = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "S3 File Source"
        );

        // Define the output tag for failed messages (dead-letter)
        final OutputTag<String> deadLetterTag = new OutputTag<String>("dead-letter") {};

        // Transform the data and handle errors
        SingleOutputStreamOperator<String> transformedStream = s3DataStream.process(new DataTransformWithErrorHandling(deadLetterTag));

        // LOGGER.info("Setting up Kafka producer with bootstrap servers: {}", jobConfig.getBootstrapServers());
        // Set up Kafka producer properties
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", bootstrapServers);

        // Add sink to publish to Kafka
        // LOGGER.info("Adding sink to publish messages to Kafka topic: {}", jobConfig.getKafkaTopic());
        KafkaSink<String> mainSink = buildKafkaSink(kafkaTopic);
        transformedStream.sinkTo(mainSink)
            .setParallelism(parallelismMainTopic);

        // Publish failed messages to the dead-letter topic
        // LOGGER.info("Adding sink to publish failed messages to dead-letter Kafka topic: {}", jobConfig.getDeadLetterTopic());
        KafkaSink<String> errSink = buildKafkaSink(deadLetterTopic);
        transformedStream.getSideOutput(deadLetterTag).sinkTo(errSink)
            .setParallelism(parallelismDeadLetterTopic);

        try {
            env.execute("FromS3ToKafka Job");
            // LOGGER.info("Job executed successfully");
        } catch (Exception e) {
            // LOGGER.error("Error during job execution", e);
            throw e;
        }
    }

    private KafkaSink<String> buildKafkaSink(String topic){
        return KafkaSink.<String>builder()
            .setBootstrapServers(bootstrapServers)
            .setProperty("transaction.timeout.ms", producerTxnTimeOut)
            .setRecordSerializer(
                    KafkaRecordSerializationSchema.builder()
                            .setTopic(topic)
                            .setValueSerializationSchema(new SimpleStringSchema())
                            .build()
            )
            .setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
            .build();
    }
}