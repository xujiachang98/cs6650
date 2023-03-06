import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatchRun implements Runnable{
    private static final String MATCH_QUEUE = "match_queue";
    private final static String FANOUT_EXCHANGE = "fanout_exchange";

    private Connection connection;
    private SwipeMatch swipeMatch;

    public MatchRun(Connection connection,
                    SwipeMatch swipeMatch) {
        this.connection = connection;
        this.swipeMatch = swipeMatch;
    }

    @Override
    public void run() {
        try {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(FANOUT_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(MATCH_QUEUE, true, false, false,null);
            channel.queueBind(MATCH_QUEUE, FANOUT_EXCHANGE, "");
            channel.basicQos(1);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                Gson gson = new Gson();
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                SwipeDetails swipeDetails = gson.fromJson(message, SwipeDetails.class);
                String swiper = swipeDetails.getSwiper();
                String swipee = swipeDetails.getSwipee();
                String direction = swipeDetails.getSwipeDirection();
                if (direction.equals("right")) {
                    swipeMatch.addPotentialMatch(swiper, swipee);
                }
            };
            channel.basicConsume(MATCH_QUEUE, false, deliverCallback, consumerTag -> {});
        } catch (IOException e) {
            Logger.getLogger(MatchRun.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
