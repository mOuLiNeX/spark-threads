package fr.manu.app.concurrent;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum EnumComputationStrategy {
    SEQUENTIAL {
        @Override
        public void process(IntStream sequence, Runnable action) {
            sequence.forEach(i -> action.run());
        }
    },
    FORKJOIN {
        @Override
        public void process(IntStream sequence, Runnable action) {
            CompletableFuture.allOf(
                sequence
                    .mapToObj(i -> CompletableFuture.runAsync(action))
                    .toArray(size -> new CompletableFuture[size]))
                .join();
        }
    },
    THREAD_PER_PROCESS {
        @Override
        public void process(IntStream sequence, Runnable action) {
            final ExecutorService newSingleThreadExecutor = createThreadsPool(1);
            process(sequence, action, newSingleThreadExecutor);
            newSingleThreadExecutor.shutdown();
        }
    },
    POOL_PER_PROCESS {
        @Override
        public void process(IntStream sequence, Runnable action) {
            ExecutorService threadsPool = Executors.newFixedThreadPool(3);
            process(sequence, action, threadsPool);
            threadsPool.shutdown();
        }
    },
    GLOBAL_POOL {
        @Override
        public void process(IntStream sequence, Runnable action) {
            process(sequence, action, workerPool);
        }
    },
    GLOBAL_POOL_WITH_FALLBACK_ON_POOL_FULL {
        @Override
        public void process(IntStream sequence, Runnable action) {
            if (workerPool.isFull()) {
                // Pool full, switching back to sequential
                SEQUENTIAL.process(sequence, action);
            } else {
                GLOBAL_POOL.process(sequence, action);
            }
        }
    },
    GLOBAL_POOL_WITH_FALLBACK_ON_WAITING_QUEUE_SIZE {
        @Override
        public void process(IntStream sequence, Runnable action) {
            if (workerPool.getWaitingCount() > workerPoolMaxWait) {
                // Too much threads waiting for pool, switching back to sequential
                SEQUENTIAL.process(sequence, action);
            } else {
                GLOBAL_POOL.process(sequence, action);
            }
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(EnumComputationStrategy.class);

    private static final MonitoringThreadPoolExecutor createThreadsPool(int capacity) {
        return new MonitoringThreadPoolExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(capacity));
    }

    private static void process(IntStream sequence, Runnable action, ExecutorService executor) {
        CompletableFuture.allOf(
            sequence
                .mapToObj(i -> CompletableFuture.runAsync(action, executor))
                .toArray(size -> new CompletableFuture[size]))
            .join();
    }

    static int workerPoolCapacity = 8;
    static int workerPoolMaxWait = 8;
    static MonitoringThreadPoolExecutor workerPool;

    static {
        try {
            workerPoolCapacity = Integer.parseInt(System.getenv("WORKER_POOL_CAPACITY"));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid custom WORKER_POOL_CAPACITY env var specified, switching to default value", e);
        }
        try {
            workerPoolMaxWait = Integer.parseInt(System.getenv("WORKER_POOL_MAX_WAIT"));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid custom WORKER_POOL_MAX_WAIT env var specified, switching to default value", e);
        }
        workerPool = createThreadsPool(workerPoolCapacity);
        LOGGER.info("Create global pool with {} threads", workerPoolCapacity);

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(workerPool,
                new ObjectName("fr.manu.spatk-threads:type=MonitoringThreadPoolExecutor"));
        } catch (Exception e) {
            LOGGER.error("Cannot register MBean to monitor thread pool", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> close()));
    }

    private static final void close() {
        workerPool.shutdownNow();
    }

    public abstract void process(IntStream sequence, Runnable action);
}
