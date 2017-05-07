package fr.manu.app.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

// Inspired by http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html
public class MonitoringThreadPoolExecutor implements ExecutorService, MonitoringThreadPoolExecutorMXBean {

    private final ThreadPoolExecutor delegate;
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringThreadPoolExecutor.class);
    private long lastWaitingTime;

    public MonitoringThreadPoolExecutor(ThreadPoolExecutor delegate) {
        this.delegate = delegate;
        this.lastWaitingTime = 0;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        final Instant startTime = Instant.now();
        return getDelegate().submit(() -> {
            lastWaitingTime = Duration.between(startTime, Instant.now()).toMillis();
            LOGGER.info("Task {} spent {}ms in queue", task, lastWaitingTime);
            return task.call();
        });
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(() -> {
            task.run();
            return result;
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(() -> {
            task.run();
            return null;
        });
    }

    @Override
    public void shutdown() {
        getDelegate().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return getDelegate().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getDelegate().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getDelegate().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getDelegate().awaitTermination(timeout, unit);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getDelegate().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return getDelegate().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getDelegate().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return getDelegate().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        final Instant startTime = Instant.now();
        getDelegate().execute(() -> {
            lastWaitingTime = Duration.between(startTime, Instant.now()).toMillis();
            LOGGER.info("Command {} spent {}ms in queue", command, lastWaitingTime);
            command.run();
        });
    }

    public boolean isFull() {
        return getDelegate().getActiveCount() == getDelegate().getCorePoolSize();
    }

    public int getWaitingCount() {
        return getDelegate().getQueue().size();
    }

    public ThreadPoolExecutor getDelegate() {
        return delegate;
    }

    @Override
    public int getActiveCount() {
        return getDelegate().getActiveCount();
    }

    @Override
    public long getLastWaitingTime() {
        return lastWaitingTime;
    }
}