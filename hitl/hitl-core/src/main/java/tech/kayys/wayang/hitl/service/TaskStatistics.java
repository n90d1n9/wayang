package tech.kayys.wayang.hitl.service;

public record TaskStatistics(
    long activeTasks,
    long completedToday,
    long overdueTasks
) {}