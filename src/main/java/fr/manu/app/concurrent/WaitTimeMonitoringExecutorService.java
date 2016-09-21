package fr.manu.app.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Inspired by http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html
public class WaitTimeMonitoringExecutorService implements ExecutorService {

    private final ThreadPoolExecutor delegate;
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitTimeMonitoringExecutorService.class);

    public WaitTimeMonitoringExecutorService(ThreadPoolExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        final Instant startTime = Instant.now();
        return getDelegate().submit(() -> {
            final long queueDuration = Duration.between(startTime, Instant.now()).toMillis();
            LOGGER.info("Task {} spent {}ms in queue", task, queueDuration);
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
            final long queueDuration = Duration.between(startTime, Instant.now()).toMillis();
            LOGGER.info("Command {} spent {}ms in queue", command, queueDuration);
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
}