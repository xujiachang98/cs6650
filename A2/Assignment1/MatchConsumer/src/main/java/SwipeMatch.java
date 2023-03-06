import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SwipeMatch {
    private Map<String, Deque<String>> potentialMatches;

    public SwipeMatch() {
        potentialMatches = new ConcurrentHashMap<>();
    }
    // FIFO
    public void addPotentialMatch(String swiperId, String swipee) {
        if (!potentialMatches.containsKey(swiperId)) {
            potentialMatches.put(swiperId, new ArrayDeque<>());
        }
        Deque<String> swipeeQueue = potentialMatches.get(swiperId);
        if (swipeeQueue.size() == 100) {
            swipeeQueue.poll();
        }
        swipeeQueue.add(swipee);
    }

    public List<String> getPotentialMatches(String swiperId) {
        if (!potentialMatches.containsKey(swiperId)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(potentialMatches.get(swiperId));
    }
}
