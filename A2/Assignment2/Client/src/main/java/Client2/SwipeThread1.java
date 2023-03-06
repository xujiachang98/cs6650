package Client2;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SwipeThread1 extends Thread {
    RequestCounter1 counter1;
    private Queue<Record> queue;

    public SwipeThread1(RequestCounter1 counter1, Queue<Record> queue) {
        this.counter1 = counter1;
        this.queue = queue;
    }

    public void run() {
        long start = System.currentTimeMillis();
        ApiClient client = new ApiClient();
        client.setBasePath("http://34.218.246.5:8080/Servlet_war/");
        SwipeApi swipeApi = new SwipeApi(client);
        SwipeDetails swipeDetails = new SwipeDetails();

        String leftOrRight = ThreadLocalRandom.current().nextInt(0, 2) == 0 ? "left" : "right";
        String swiperId = String.valueOf(ThreadLocalRandom.current().nextInt(1, 5001));
        String swipeeId = String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000001));
        String comment = generateRandomString(20);
        swipeDetails.setSwiper(swiperId);
        swipeDetails.setSwipee(swipeeId);
        swipeDetails.setComment(comment);

        int response_code = 0;

        for (int i = 0; i < 5; i++) {
            try {
                ApiResponse res = swipeApi.swipeWithHttpInfo(swipeDetails, leftOrRight);
                response_code = res.getStatusCode();
                if (response_code == 201) {
                    counter1.incrementSuccessCount();
                    break;
                }
            } catch (ApiException e) {
                if (i == 4) {
                    counter1.incrementUnsuccessfulCount();
                    System.out.println(e);
                    System.out.println(e.getCode());
                }
            }
        }
        long end = System.currentTimeMillis();
        queue.add(new Record(start, end,"POST", end - start, response_code));
    }

    private static String generateRandomString(int length) {
        StringBuilder builder = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < length; i++) {
            char c = (char) (rand.nextInt(26) + 'a');
            builder.append(c);
        }
        return builder.toString();
    }
}
