import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GenericPool<R> {

    private boolean isOpen;
    private Map<R, Boolean> map = new ConcurrentHashMap<>();

    public GenericPool() {
    }

    synchronized public void open() {
        if (!this.isOpen) {
            this.isOpen = true;
        }
    }

    synchronized public boolean add(R r) {
        if (this.map.containsKey(r)) {
            return false;
        }
        this.map.put(r, false);
        notifyAll();
        return true;
    }

    synchronized public R acquire() throws InterruptedException {
        if (!this.isOpen) {
            return null;
        }
        R r = this.findAnyNotAcquired();
        while (r == null) {
            wait();
            r = this.findAnyNotAcquired();
        }
        this.map.put(r, true);
        notifyAll();
        return r;
    }

    private R findAnyNotAcquired() {
        for (Map.Entry<R, Boolean> entry : this.map.entrySet()) {
            if (!entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    synchronized public boolean release(R r) {
        if (this.map.containsKey(r) && this.map.get(r)) {
            this.map.put(r, false);
            notifyAll();
            return true;
        }
        return false;
    }

    synchronized public void close() throws InterruptedException {
        if (!this.isOpen) {
            return;
        }
        while (this.map.values().stream().anyMatch(b -> b)) {
            wait();
        }
        this.isOpen = false;
    }

    synchronized public void closeNow() {
        if (!this.isOpen) {
            this.isOpen = false;
        }
    }

}