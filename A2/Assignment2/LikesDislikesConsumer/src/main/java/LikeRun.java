import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LikeRun implements Runnable{
    private static final String LIKE_QUEUE = "like_queue";
    private final static String FANOUT_EXCHANGE = "fanout_exchange";

    private Connection connection;
    private ConcurrentHashMap<String, int[]> likesDislikesMap;

    public LikeRun(Connection connection,
                      ConcurrentHashMap<String, int[]> likesDislikesMap) {
        this.connection = connection;
        this.likesDislikesMap = likesDislikesMap;
    }

    @Override
    public void run() {
        try {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(FANOUT_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(LIKE_QUEUE, true, false, false,null);
            channel.queueBind(LIKE_QUEUE, FANOUT_EXCHANGE, "");
            channel.basicQos(1);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                Gson gson = new Gson();
                SwipeDetails swipeDetails = gson.fromJson(message, SwipeDetails.class);
                String swiperId = swipeDetails.getSwiper();
                String swipeDirection = swipeDetails.getSwipeDirection();
                int[] countArray = likesDislikesMap.getOrDefault(swiperId, new int[]{0, 0});
                if (swipeDirection.equals("left")) {
                    countArray[0]++; // increment dislikes
                } else {
                    countArray[1]++; // increment likes
                }
                likesDislikesMap.put(swiperId, countArray);
                System.out.println(likesDislikesMap);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            channel.basicConsume(LIKE_QUEUE, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            Logger.getLogger(LikeRun.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
