package fr.manu.app.concurrent;

public interface MonitoringThreadPoolExecutorMXBean {
    int getActiveCount();

    int getWaitingCount();

    long getLastWaitingTime();
}
