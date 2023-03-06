import com.google.gson.Gson;
import com.rabbitmq.client.*;
import lombok.SneakyThrows;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "TwinderServlet", value = "/TwinderServlet")
public class TwinderServlet extends HttpServlet {
    private static final String LIKE_QUEUE = "like_queue";
    private static final String MATCH_QUEUE = "match_queue";
    private static final String FANOUT_EXCHANGE = "fanout_exchange";
    private ConnectionFactory factory;
    private BlockingQueue<Channel> channelPool;

    @Override
    public void init() throws ServletException {
        super.init();
        factory = new ConnectionFactory();
        factory.setHost("35.160.148.1");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection;
        try {
            connection = factory.newConnection();
            channelPool = new LinkedBlockingQueue<>(10);
            for (int i = 0; i < 10; i++) {
                Channel channel = connection.createChannel();
                channelPool.offer(channel);
            }
        } catch (TimeoutException | IOException e) {
            e.printStackTrace();
        }
    }


    @SneakyThrows
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("Missing Url");
        }
        String[] urlParts = urlPath.split("/");
        Gson gson = new Gson();
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ResponseMsg responseMsg = new ResponseMsg("Invalid Url");
            res.getWriter().write(gson.toJson(responseMsg));
            return;
        }

        SwipeDetails swipeDetails;
        String swiper;
        String swipee;
        String comment;
        try {
            StringBuilder requestData = new StringBuilder();
            String s;
            while ((s = req.getReader().readLine()) != null) {
                requestData.append(s);
            }
            swipeDetails = gson.fromJson(requestData.toString(), SwipeDetails.class);
            swipeDetails.setSwipeDirection(urlParts[1]);
            swiper = swipeDetails.getSwiper();
            swipee = swipeDetails.getSwipee();
            comment = swipeDetails.getComment();
            if (swiper == null || swipee == null) {
                throw new Exception("Missing parameters: both swiper and swipee are required.");
            }
            if (comment != null && comment.length() > 256) {
                throw new Exception("Message too long: the maximum length for a message is 256 characters.");
            }
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMsg responseMsg = new ResponseMsg("Invalid input: " + e.getMessage());
            res.getWriter().write(gson.toJson(responseMsg));
            return;
        }
        sendPayloadToQueue(gson.toJson(swipeDetails));
    }

    private boolean isUrlValid(String[] urlParts) {
        if (urlParts.length != 2) {
            return false;
        }
        return urlParts[1].equals("left") || urlParts[1].equals("right");
    }

    private void sendPayloadToQueue(String payload) {
        Channel channel = null;
        try {
            channel = channelPool.take();
            channel.exchangeDeclare(FANOUT_EXCHANGE, BuiltinExchangeType.FANOUT);
            channel.queueDeclare(LIKE_QUEUE, true, false, false, null);
            channel.queueDeclare(MATCH_QUEUE, true, false, false, null);
            channel.queueBind(LIKE_QUEUE, FANOUT_EXCHANGE, "");
            channel.queueBind(MATCH_QUEUE, FANOUT_EXCHANGE, "");
            channel.basicPublish(FANOUT_EXCHANGE, "", null, payload.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (channel != null) {
                    channelPool.put(channel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
