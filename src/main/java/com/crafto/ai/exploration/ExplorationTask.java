package com.crafto.ai.exploration;

import java.time.LocalDateTime;

/**
 * Задача исследования для очереди
 */
public class ExplorationTask implements Comparable<ExplorationTask> {
    private ChunkCoordinate coordinate;
    private int priority;
    private LocalDateTime creationTime;
    private String taskType;
    private String assignedTo;
    private boolean completed;
    
    public ExplorationTask(ChunkCoordinate coordinate, int priority) {
        this.coordinate = coordinate;
        this.priority = priority;
        this.creationTime = LocalDateTime.now();
        this.taskType = "STANDARD";
        this.completed = false;
    }
    
    public ExplorationTask(ChunkCoordinate coordinate, int priority, String taskType) {
        this(coordinate, priority);
        this.taskType = taskType;
    }
    
    @Override
    public int compareTo(ExplorationTask other) {
        // Сортировка по приоритету (больший приоритет = выше в очереди)
        int priorityCompare = Integer.compare(other.priority, this.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        
        // При равном приоритете - по времени создания (раньше = выше)
        return this.creationTime.compareTo(other.creationTime);
    }
    
    /**
     * Отмечает задачу как выполненную
     */
    public void markCompleted(String completedBy) {
        this.completed = true;
        this.assignedTo = completedBy;
    }
    
    /**
     * Проверяет, устарела ли задача
     */
    public boolean isExpired() {
        // Задачи устаревают через 1 час
        return creationTime.isBefore(LocalDateTime.now().minusHours(1));
    }
    
    /**
     * Получает описание задачи
     */
    public String getDescription() {
        return String.format("Исследовать область %s (приоритет: %d, тип: %s)", 
                           coordinate, priority, taskType);
    }
    
    // Геттеры и сеттеры
    
    public ChunkCoordinate getCoordinate() {
        return coordinate;
    }
    
    public void setCoordinate(ChunkCoordinate coordinate) {
        this.coordinate = coordinate;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public String getTaskType() {
        return taskType;
    }
    
    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
    
    public String getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    @Override
    public String toString() {
        return String.format("ExplorationTask{coordinate=%s, priority=%d, type='%s', completed=%s}", 
                           coordinate, priority, taskType, completed);
    }
}