package com.salescode.dis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlinkApplication implements CommandLineRunner {

    @Autowired
    private com.salescode.dis.flink.jobs.fromS3ToKafka.Job fromS3ToKafka;

    @Autowired
    private com.salescode.dis.flink.jobs.fromKafkaToDB.KafkaConsumerJob fromKafkaToDB;

    public static void main(String[] args) {
        // if (args.length == 0) {
        //     throw new IllegalArgumentException("No job specified. Please provide a job name to execute.");
        // }
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        SpringApplication.run(FlinkApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        // Execute the job based on an argument or configuration
        // String jobName = args[0];
        String jobName = "FromKafkaToDB";
        switch (jobName) {
            case "FromS3ToKafka":
                fromS3ToKafka.executeJob();
                break;
            case "FromKafkaToDB":
                fromKafkaToDB.executeJob();
                break;
                // Add cases for other jobs in the future
            default:
                throw new IllegalArgumentException("Unknown job: " + jobName);
        }
    }
}