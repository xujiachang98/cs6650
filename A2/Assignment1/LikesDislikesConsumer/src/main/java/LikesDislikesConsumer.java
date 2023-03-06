import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.*;

public class LikesDislikesConsumer {
    final static int POOL_SIZE = 100;
    final static int NUM_REQUEST = 1000;


    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        ConcurrentHashMap<String, int[]> likesDislikesMap = new ConcurrentHashMap<>();
        factory.setHost("35.160.148.1");
        factory.setUsername("guest");
        factory.setPassword("guest");
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);

        Connection connection = factory.newConnection();
        for (int i = 0; i < NUM_REQUEST; i++) {
            pool.execute(new LikeRun(connection, likesDislikesMap));
        }
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
