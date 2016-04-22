package me.anitas;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DynCountDownLatch  {

    private final AtomicInteger internal = new AtomicInteger(1);

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    public void register() {
        internal.incrementAndGet();
    }

    public void countDown() {
        int c = internal.decrementAndGet();

        lock.lock();
        if (c == 0) {
            condition.signal();
        }
        lock.unlock();
    }

    public void await() throws InterruptedException {
        countDown();

        lock.lock();
        while (internal.get() != 0){
            condition.await();
        }
        lock.unlock();
        register();
    }
}
