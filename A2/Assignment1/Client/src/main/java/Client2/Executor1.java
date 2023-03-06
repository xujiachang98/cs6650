package Client2;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Executor1 {
    final static int POOL_SIZE = 200;
    final static int NUM_REQUEST = 500000;

    public static void main(String[] args) {
        RequestCounter1 counter1 = new RequestCounter1();
        ArrayBlockingQueue<Record> queue = new ArrayBlockingQueue<>(NUM_REQUEST);
        ExecutorService executor1 = Executors.newFixedThreadPool(POOL_SIZE);
        Long program_start = System.currentTimeMillis();

        for (int i = 0; i < NUM_REQUEST; i++) {
            executor1.execute(new SwipeThread1(counter1, queue));
        }

        executor1.shutdown();
        try {
            executor1.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Long program_end = System.currentTimeMillis();

        List<Record> data = new ArrayList<>();
        queue.drainTo(data);

        Collections.sort(data, (o1, o2) -> (int) (o1.getStartTime() - o2.getStartTime()));
        try (FileWriter writer = new FileWriter("record_data.csv")) {
            writer.append("StartTime");
            writer.append(",");
            writer.append("RequestType");
            writer.append(",");
            writer.append("Latency/ms");
            writer.append(",");
            writer.append("ResponseCode");
            writer.append("\n");
            for (Record record : data) {
                Instant instant = Instant.ofEpochMilli(record.getStartTime());
                ZonedDateTime zdt = instant.atZone(ZoneId.of("America/Los_Angeles"));
                writer.append(String.valueOf(zdt));
                writer.append(",");
                writer.append(record.getRequestType());
                writer.append(",");
                writer.append(String.valueOf(record.getLatency()));
                writer.append(",");
                writer.append(String.valueOf(record.getResponseCode()));
                writer.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // generate record throughput
        try (FileWriter writer = new FileWriter("record_throughput.csv")) {
            writer.append("second");
            writer.append(",");
            writer.append("throughput");
            writer.append("\n");
            long offset = data.get(0).getStartTime();
            Map<Long, Integer> map = new HashMap<>();
            for (Record record : data) {
                long recordEnd = record.getEndTime() - offset;
                long second = recordEnd / 1000;
                if (map.containsKey(second)) {
                    if (record.getResponseCode() == 201) {
                        map.put(second, map.get(second) + 1);
                    }
                } else {
                    map.put(second, 1);
                }
            }
            for (Map.Entry<Long, Integer> entry : map.entrySet()) {
                writer.append(String.valueOf(entry.getKey()));
                writer.append(",");
                writer.append(String.valueOf(entry.getValue()));
                writer.append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(data, (o1, o2) -> (int) (o1.getLatency() - o2.getLatency()));
        long wall_time = program_end - program_start;
        long total_response_time = 0;
        double mean_response_time = 0;
        long median_response_time = 0;
        long min_response_time = 0;
        long max_response_time = 0;
        long percentile_99 = 0;
        int count = 0;
        for (Record record : data) {
            count += 1;
            total_response_time += record.getLatency();
            if (count == NUM_REQUEST / 2) {
                median_response_time = record.getLatency();
            }
            if (count == (int)(NUM_REQUEST * 0.99)) {
                percentile_99 = record.getLatency();
            }
        }
        mean_response_time = total_response_time / NUM_REQUEST;
        min_response_time = data.get(0).getLatency();
        max_response_time = data.get(data.size() - 1).getLatency();
        wall_time /= 1000;
        long throughput = NUM_REQUEST / wall_time;
        System.out.println("Mean response time: " + mean_response_time + "ms");
        System.out.println("Median response time: " + median_response_time + "ms");
        System.out.println("Throughput: " + throughput + " requests/second");
        System.out.println("p99 response time: " + percentile_99 + "ms");
        System.out.println("Min response time: " + min_response_time + "ms");
        System.out.println("Max response time: " + max_response_time + "ms");
        System.out.println("Total run time (wall time): " + wall_time + " seconds");
    }

}



