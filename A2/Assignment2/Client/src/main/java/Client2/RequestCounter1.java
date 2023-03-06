package Client2;

public class RequestCounter1 {
    private int successful_requests = 0;
    private int unsuccessful_requests = 0;


    public synchronized void incrementSuccessCount() {
        this.successful_requests++;
    }

    public synchronized void incrementUnsuccessfulCount() {
        this.unsuccessful_requests++;
    }

    public synchronized int getSuccessCount() {
        return this.successful_requests;
    }
    public synchronized int getUnsuccessfulCount() {
        return this.unsuccessful_requests;
    }
}
