package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.exploration.ExplorationResult;
import com.crafto.ai.exploration.ExplorationSystem;
import net.minecraft.core.BlockPos;

import java.util.concurrent.CompletableFuture;

/**
 * Действие для исследования области вокруг указанной позиции
 */
public class ExploreAreaAction extends BaseAction {
    private final BlockPos centerPos;
    private final int radius;
    private CompletableFuture<ExplorationResult> explorationFuture;
    private boolean isStarted = false;
    
    public ExploreAreaAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
        
        // Получаем параметры из задачи
        this.centerPos = new BlockPos(
            task.getIntParameter("x", crafto.blockPosition().getX()),
            task.getIntParameter("y", crafto.blockPosition().getY()),
            task.getIntParameter("z", crafto.blockPosition().getZ())
        );
        this.radius = task.getIntParameter("radius", 64);
    }
    
    public ExploreAreaAction(CraftoEntity crafto, BlockPos centerPos, int radius) {
        super(crafto, createExploreTask(centerPos, radius));
        this.centerPos = centerPos;
        this.radius = radius;
    }
    
    private static Task createExploreTask(BlockPos pos, int radius) {
        Task task = new Task("explore");
        task.addParameter("x", pos.getX());
        task.addParameter("y", pos.getY());
        task.addParameter("z", pos.getZ());
        task.addParameter("radius", radius);
        return task;
    }

    @Override
    public void start() {
        ExplorationSystem exploration = crafto.getExplorationSystem();
        
        crafto.sendChatMessage("Начинаю исследование области радиусом " + radius + " блоков...");
        
        // Запускаем асинхронное исследование
        explorationFuture = exploration.exploreArea(centerPos, radius);
        isStarted = true;
        
        // Добавляем callback для обработки результата
        explorationFuture.thenAccept(result -> {
            if (result.isSuccess()) {
                String message = String.format("Исследование завершено! Найдено: %d областей, %d ресурсов", 
                    result.getExploredAreas().size(), result.getNewResources().size());
                crafto.sendChatMessage(message);
                
                // Автоматически создаем путевые точки для ценных ресурсов
                createWaypointsForResources(result);
                
                setResult(ActionResult.success("Исследование области завершено успешно"));
            } else {
                crafto.sendChatMessage("Ошибка при исследовании: " + result.getErrorMessage());
                setResult(ActionResult.failure("Ошибка исследования: " + result.getErrorMessage()));
            }
        }).exceptionally(throwable -> {
            crafto.sendChatMessage("Критическая ошибка при исследовании!");
            setResult(ActionResult.failure("Критическая ошибка: " + throwable.getMessage()));
            return null;
        });
    }

    @Override
    public void tick() {
        if (!isStarted) {
            start();
        }
        
        // Проверяем, завершилось ли исследование
        if (explorationFuture != null && explorationFuture.isDone()) {
            // Результат уже обработан в callback'е
            return;
        }
        
        // Можно добавить периодические обновления прогресса
        if (tickCount % 100 == 0) { // каждые 5 секунд
            crafto.sendChatMessage("Исследование продолжается...");
        }
    }

    @Override
    public void cancel() {
        if (explorationFuture != null && !explorationFuture.isDone()) {
            explorationFuture.cancel(true);
            crafto.sendChatMessage("Исследование отменено");
        }
        setResult(ActionResult.failure("Исследование отменено"));
    }

    @Override
    public boolean isComplete() {
        return getResult() != null;
    }

    @Override
    public String getDescription() {
        return String.format("Исследование области в радиусе %d блоков от (%d, %d, %d)", 
            radius, centerPos.getX(), centerPos.getY(), centerPos.getZ());
    }
    
    /**
     * Автоматически создает путевые точки для найденных ценных ресурсов
     */
    private void createWaypointsForResources(ExplorationResult result) {
        result.getNewResources().stream()
            .filter(resource -> isValuableResource(resource.getResourceType()))
            .forEach(resource -> {
                String waypointName = resource.getResourceType().replace("minecraft:", "") + "_" + System.currentTimeMillis();
                crafto.getWaypointSystem().createWaypoint(
                    waypointName,
                    resource.getPosition(),
                    com.crafto.ai.exploration.WaypointType.RESOURCE_SITE,
                    String.format("%s (найдено: %d)", resource.getResourceType(), resource.getQuantity())
                );
                
                crafto.sendChatMessage("Создана путевая точка: " + waypointName + " в " + resource.getPosition());
            });
    }
    
    private boolean isValuableResource(String resourceType) {
        return resourceType.contains("diamond") || 
               resourceType.contains("emerald") || 
               resourceType.contains("gold") || 
               resourceType.contains("iron") ||
               resourceType.contains("ancient_debris") ||
               resourceType.contains("spawner");
    }
}