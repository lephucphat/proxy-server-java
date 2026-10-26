package proxyapp;

public class TrafficHistory {

    private final int capacity;
    private final long[] down;
    private final long[] up;
    private int size;
    private int next;
    private long lastDown;
    private long lastUp;
    private boolean hasLast;

    public TrafficHistory(int capacity) {
        this.capacity = capacity;
        this.down = new long[capacity];
        this.up = new long[capacity];
    }

    public int capacity() {
        return capacity;
    }

    public synchronized void sample(long totalDown, long totalUp) {
        long d = 0;
        long u = 0;
        if (hasLast) {
            d = totalDown >= lastDown ? totalDown - lastDown : totalDown;
            u = totalUp >= lastUp ? totalUp - lastUp : totalUp;
        }
        lastDown = totalDown;
        lastUp = totalUp;
        hasLast = true;
        down[next] = d;
        up[next] = u;
        next = (next + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    public synchronized long[][] snapshot() {
        long[] d = new long[size];
        long[] u = new long[size];
        int start = (next - size + capacity) % capacity;
        for (int i = 0; i < size; i++) {
            d[i] = down[(start + i) % capacity];
            u[i] = up[(start + i) % capacity];
        }
        return new long[][]{d, u};
    }

    public synchronized void clear() {
        size = 0;
        next = 0;
        hasLast = false;
    }
}
