import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//  GenericPool class, with several methods fine tuned to avoid thread issues, like race conditions on shared data.
public class GenericPool<R> {

    // We choose locks instead of 'syncronized' keyword, because then we can use conditions.
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock writeLock = lock.writeLock();

    // Lock conditions are an essential part of the control process, allowing the communication between threads
    // to check shared data updated status
    private final Condition waitForSomeResourceNotAcquired = this.writeLock.newCondition();
    private final Condition waitForNoResourceAcquired = this.writeLock.newCondition();
    private final Condition waitForThisResourceNotAcquired = this.writeLock.newCondition();

    private final Map<R, Boolean> map = new HashMap<>();

    volatile private boolean isOpen;

    public GenericPool() {
    }

    public void open() {
        // isOpen is declared volatile, so we don't need a write lock here.
        if (!this.isOpen) {
            this.isOpen = true;
        }
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public boolean add(R r) {
        this.writeLock.lock();
        try {
            if (this.map.containsKey(r)) {
                return false;
            }
            this.map.put(r, false);
            // this.map (the shared data between threads) has changed. So we must signal all conditions active on other threads, wich depends on
            // changes on this.map
            this.conditionsSignalAll();
            return true;
        } finally {
            this.writeLock.unlock();
        }
    }

    public boolean remove(R r) throws InterruptedException {
        this.writeLock.lock();
        try {
            if (this.map.containsKey(r)) {
                while (this.map.containsKey(r) && this.map.get(r)) {
                    // if the resource is acquired, we must wait for another thread to modify the status of this resource
                    // that could have changed acquired status to not acquired
                    this.waitForThisResourceNotAcquired.await();
                }
                this.map.remove(r);
                this.conditionsSignalAll();
                return true;
            }
            return false;
        } finally {
            this.writeLock.unlock();
        }
    }

    public boolean removeNow(R r) {
        this.writeLock.lock();
        try {
            if (this.map.containsKey(r)) {
                this.map.remove(r);
                this.conditionsSignalAll();
                return true;
            }
            return false;
        } finally {
            this.writeLock.unlock();
        }
    }

    public R acquire() throws InterruptedException {
        this.writeLock.lock();
        try {
            if (!this.isOpen) {
                return null;
            }
            if (this.map.isEmpty()) {
                return null;
            }
            R r = this.findAnyNotAcquired();
            while (r == null) {
                // if there is no resource not acquired, we must wait for another thread to update this situation
                // meaning changing the status of some resource from acquired to not acquired, or adding some new resource in
                // not acquired state
                this.waitForSomeResourceNotAcquired.await();
                r = this.findAnyNotAcquired();
            }
            this.map.put(r, true);
            this.conditionsSignalAll();
            return r;
        } finally {
            this.writeLock.unlock();
        }
    }

    public R acquire(Long timeout, TimeUnit unit) throws InterruptedException {
        if (!this.isOpen) {
            return null;
        }
        this.writeLock.lock();
        try {
            R r = this.findAnyNotAcquired();
            while (r == null) {
                if (!this.waitForSomeResourceNotAcquired.await(timeout, unit)) {
                    return null;
                }
                r = this.findAnyNotAcquired();
            }
            this.map.put(r, true);
            this.conditionsSignalAll();
            return r;
        } finally {
            this.writeLock.unlock();
        }
    }

    public boolean release(R r) {
        this.writeLock.lock();
        try {
            if (this.map.containsKey(r) && this.map.get(r)) {
                this.map.put(r, false);
                this.conditionsSignalAll();
                return true;
            }
            return false;
        } finally {
            this.writeLock.unlock();
        }
    }

    public void close() throws InterruptedException {
        this.writeLock.lock();
        try {
            if (!this.isOpen) {
                return;
            }
            while (this.isThereAnyAcquired()) {
                // to close the pool, must be no resource with acquired status. That will depend on changes made by other threads, so
                // the thread executing this code, will wait to other threads signal, alerting to recheck the above condition
                this.waitForNoResourceAcquired.await();
            }
            this.conditionsSignalAll();
        } finally {
            this.writeLock.unlock();
        }
        this.isOpen = false;
    }

    public void closeNow() {
        this.writeLock.lock();
        try {
            if (!this.isOpen) {
                this.isOpen = false;
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    private R findAnyNotAcquired() {
        for (Map.Entry<R, Boolean> entry : this.map.entrySet()) {
            if (!entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isThereAnyAcquired() {
        for (Map.Entry<R, Boolean> entry : this.map.entrySet()) {
            if (entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    private void conditionsSignalAll() {
        // A bit of exaggeration, but simpler than deciding in each case with condition must be signaled
        // (wich could lead to endless block situations)
        this.waitForSomeResourceNotAcquired.signalAll();
        this.waitForThisResourceNotAcquired.signalAll();
        this.waitForNoResourceAcquired.signalAll();
    }

}