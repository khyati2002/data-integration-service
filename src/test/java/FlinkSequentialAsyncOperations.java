import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.AsyncFunction;
import org.apache.flink.streaming.api.functions.async.ResultFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class FlinkSequentialAsyncOperations {

    public static void main(String[] args) throws Exception {
        // Set up the Flink execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Create a simple DataStream for testing
        DataStream<String> inputStream = env.fromElements("key1", "key2", "key3");

        // Apply the first async operation
        DataStream<String> asyncOp1Stream = AsyncDataStream.orderedWait(
                inputStream,
                new AsyncOp1(),
                1000, // Timeout for async operation 1
                TimeUnit.MILLISECONDS,
                10 // Parallelism
        );

        // Apply the second async operation after the first one
        DataStream<String> asyncOp2Stream = AsyncDataStream.orderedWait(
                asyncOp1Stream,
                new AsyncOp2(),
                1000, // Timeout for async operation 2
                TimeUnit.MILLISECONDS,
                10 // Parallelism
        );

        // Apply the third async operation after the second one
        DataStream<String> asyncOp3Stream = AsyncDataStream.orderedWait(
                asyncOp2Stream,
                new AsyncOp3(),
                1000, // Timeout for async operation 3
                TimeUnit.MILLISECONDS,
                10 // Parallelism
        );

        // Output the final results of all async operations
        asyncOp3Stream.print();

        // Execute the Flink job
        env.execute("Flink Sequential Async Operations Example");
    }

    // First Async Operation - Simulating external API call
    public static class AsyncOp1 implements AsyncFunction<String, String> {
        @Override
        public void asyncInvoke(String input, ResultFuture<String> resultFuture) {
            // Simulate an async API call with CompletableFuture
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate delay for the first service
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "Fetched from Service1 for: " + input;
            });

            future.thenAccept(result -> {
                // Send result to the next stage of the pipeline
                resultFuture.complete(List.of(result));
            });
        }
    }

    // Second Async Operation - Simulating a second external API call
    public static class AsyncOp2 implements AsyncFunction<String, String> {
        @Override
        public void asyncInvoke(String input, ResultFuture<String> resultFuture) {
            // Simulate a second async API call
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate delay for the second service
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "Fetched from Service2 for: " + input;
            });

            future.thenAccept(result -> {
                // Send the result of AsyncOp2 to the next stage of the pipeline
                resultFuture.complete(List.of(result));
            });
        }
    }

    // Third Async Operation - Simulating a third external API call
    public static class AsyncOp3 implements AsyncFunction<String, String> {
        @Override
        public void asyncInvoke(String input, ResultFuture<String> resultFuture) {
            // Simulate a third async API call
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Simulate delay for the third service
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "Fetched from Service3 for: " + input;
            });

            future.thenAccept(result -> {
                // Send the final result after all async operations
                resultFuture.complete(List.of(result));
            });
        }
    }
}