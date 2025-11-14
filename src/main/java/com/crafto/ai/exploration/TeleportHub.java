package com.crafto.ai.exploration;

import net.minecraft.core.BlockPos;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Телепортационный хаб для быстрого перемещения
 */
public class TeleportHub {
    private String hubId;
    private BlockPos position;
    private String name;
    private String description;
    private LocalDateTime creationTime;
    private LocalDateTime lastUsed;
    private String createdBy;
    private boolean active;
    private int usageCount;
    private List<String> connectedHubs;
    private int maxConnections;
    private String hubType; // BASIC, ADVANCED, MASTER
    private boolean requiresPermission;
    private List<String> authorizedUsers;
    
    // Конструктор по умолчанию для JSON десериализации
    public TeleportHub() {
        this.connectedHubs = new ArrayList<>();
        this.authorizedUsers = new ArrayList<>();
    }
    
    public TeleportHub(String hubId, BlockPos position, String name) {
        this();
        this.hubId = hubId;
        this.position = position;
        this.name = name;
        this.creationTime = LocalDateTime.now();
        this.active = true;
        this.usageCount = 0;
        this.maxConnections = 5; // Базовое количество соединений
        this.hubType = "BASIC";
        this.requiresPermission = false;
    }
    
    /**
     * Добавляет соединение с другим хабом
     */
    public boolean addConnection(String otherHubId) {
        if (connectedHubs.size() >= maxConnections) {
            return false; // Достигнут лимит соединений
        }
        
        if (!connectedHubs.contains(otherHubId)) {
            connectedHubs.add(otherHubId);
            return true;
        }
        
        return false; // Соединение уже существует
    }
    
    /**
     * Удаляет соединение с другим хабом
     */
    public boolean removeConnection(String otherHubId) {
        return connectedHubs.remove(otherHubId);
    }
    
    /**
     * Отмечает использование хаба
     */
    public void markUsed(String userName) {
        this.lastUsed = LocalDateTime.now();
        this.usageCount++;
        
        // Автоматически добавляем пользователя в авторизованные, если хаб не требует разрешений
        if (!requiresPermission && !authorizedUsers.contains(userName)) {
            authorizedUsers.add(userName);
        }
    }
    
    /**
     * Проверяет, может ли пользователь использовать хаб
     */
    public boolean canUse(String userName) {
        if (!active) {
            return false;
        }
        
        if (!requiresPermission) {
            return true;
        }
        
        return authorizedUsers.contains(userName) || userName.equals(createdBy);
    }
    
    /**
     * Добавляет пользователя в список авторизованных
     */
    public void authorizeUser(String userName) {
        if (!authorizedUsers.contains(userName)) {
            authorizedUsers.add(userName);
        }
    }
    
    /**
     * Удаляет пользователя из списка авторизованных
     */
    public void revokeUser(String userName) {
        authorizedUsers.remove(userName);
    }
    
    /**
     * Улучшает хаб до следующего уровня
     */
    public boolean upgrade() {
        switch (hubType) {
            case "BASIC":
                hubType = "ADVANCED";
                maxConnections = 10;
                return true;
            case "ADVANCED":
                hubType = "MASTER";
                maxConnections = 20;
                return true;
            case "MASTER":
                return false; // Уже максимальный уровень
            default:
                return false;
        }
    }
    
    /**
     * Вычисляет расстояние до указанной позиции
     */
    public double getDistanceFrom(BlockPos fromPos) {
        if (position == null || fromPos == null) return Double.MAX_VALUE;
        
        double dx = position.getX() - fromPos.getX();
        double dy = position.getY() - fromPos.getY();
        double dz = position.getZ() - fromPos.getZ();
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Получает стоимость телепортации (в условных единицах)
     */
    public int getTeleportCost(BlockPos destination) {
        double distance = getDistanceFrom(destination);
        int baseCost = 10;
        
        // Стоимость зависит от расстояния
        int distanceCost = (int) (distance / 100);
        
        // Скидка для продвинутых хабов
        double discount = 1.0;
        switch (hubType) {
            case "ADVANCED":
                discount = 0.8;
                break;
            case "MASTER":
                discount = 0.6;
                break;
        }
        
        return (int) ((baseCost + distanceCost) * discount);
    }
    
    /**
     * Получает эффективность хаба (использований на день)
     */
    public double getEfficiency() {
        if (creationTime == null) return 0.0;
        
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(creationTime, LocalDateTime.now());
        if (daysSinceCreation == 0) daysSinceCreation = 1;
        
        return (double) usageCount / daysSinceCreation;
    }
    
    /**
     * Проверяет, является ли хаб популярным
     */
    public boolean isPopular() {
        return getEfficiency() > 5.0 || usageCount > 100;
    }
    
    /**
     * Получает полное описание хаба
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("=== ").append(name).append(" ===\n");
        desc.append("ID: ").append(hubId).append("\n");
        desc.append("Тип: ").append(getHubTypeDescription()).append("\n");
        desc.append("Позиция: ").append(formatPosition()).append("\n");
        desc.append("Создан: ").append(creationTime).append("\n");
        
        if (createdBy != null) {
            desc.append("Создатель: ").append(createdBy).append("\n");
        }
        
        if (description != null && !description.isEmpty()) {
            desc.append("Описание: ").append(description).append("\n");
        }
        
        desc.append("Использований: ").append(usageCount).append("\n");
        desc.append("Эффективность: ").append(String.format("%.1f", getEfficiency())).append(" исп/день\n");
        desc.append("Соединений: ").append(connectedHubs.size()).append("/").append(maxConnections).append("\n");
        
        if (lastUsed != null) {
            desc.append("Последнее использование: ").append(lastUsed).append("\n");
        }
        
        if (requiresPermission) {
            desc.append("Требует разрешение: Да\n");
            desc.append("Авторизованных пользователей: ").append(authorizedUsers.size()).append("\n");
        }
        
        desc.append("Статус: ").append(active ? "Активен" : "Неактивен");
        
        return desc.toString();
    }
    
    /**
     * Получает описание типа хаба
     */
    private String getHubTypeDescription() {
        switch (hubType) {
            case "BASIC": return "Базовый";
            case "ADVANCED": return "Продвинутый";
            case "MASTER": return "Мастер";
            default: return "Неизвестный";
        }
    }
    
    /**
     * Форматирует позицию для отображения
     */
    private String formatPosition() {
        if (position == null) return "неизвестно";
        return String.format("(%d, %d, %d)", position.getX(), position.getY(), position.getZ());
    }
    
    // Геттеры и сеттеры
    
    public String getHubId() {
        return hubId;
    }
    
    public void setHubId(String hubId) {
        this.hubId = hubId;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setPosition(BlockPos position) {
        this.position = position;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreationTime() {
        return creationTime;
    }
    
    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public List<String> getConnectedHubs() {
        return connectedHubs;
    }
    
    public void setConnectedHubs(List<String> connectedHubs) {
        this.connectedHubs = connectedHubs;
    }
    
    public int getMaxConnections() {
        return maxConnections;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public String getHubType() {
        return hubType;
    }
    
    public void setHubType(String hubType) {
        this.hubType = hubType;
    }
    
    public boolean isRequiresPermission() {
        return requiresPermission;
    }
    
    public void setRequiresPermission(boolean requiresPermission) {
        this.requiresPermission = requiresPermission;
    }
    
    public List<String> getAuthorizedUsers() {
        return authorizedUsers;
    }
    
    public void setAuthorizedUsers(List<String> authorizedUsers) {
        this.authorizedUsers = authorizedUsers;
    }
    
    @Override
    public String toString() {
        return String.format("TeleportHub{id='%s', name='%s', type='%s', active=%s}", 
                           hubId, name, hubType, active);
    }
}