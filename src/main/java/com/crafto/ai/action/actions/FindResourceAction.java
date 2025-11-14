package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.exploration.NavigationPath;
import com.crafto.ai.exploration.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Действие для поиска и навигации к ближайшему ресурсу определенного типа
 */
public class FindResourceAction extends BaseAction {
    private final String resourceType;
    private NavigateToWaypointAction navigateAction;
    private boolean resourceFound = false;
    
    public FindResourceAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
        this.resourceType = task.getStringParameter("resource", "diamond_ore");
    }
    
    public FindResourceAction(CraftoEntity crafto, String resourceType) {
        super(crafto, createFindResourceTask(resourceType));
        this.resourceType = resourceType;
    }
    
    private static Task createFindResourceTask(String resourceType) {
        Task task = new Task("find_resource");
        task.addParameter("resource", resourceType);
        return task;
    }

    @Override
    public void start() {
        BlockPos currentPos = crafto.blockPosition();
        
        // Ищем ближайший ресурс
        List<ResourceLocation> resources = crafto.getExplorationSystem().getResourceLocations(resourceType);
        
        if (resources.isEmpty()) {
            String errorMsg = "Ресурс '" + resourceType + "' не найден в исследованных областях";
            crafto.sendChatMessage(errorMsg);
            setResult(ActionResult.failure(errorMsg));
            return;
        }
        
        // Найти ближайший ресурс
        ResourceLocation nearest = resources.stream()
            .min((r1, r2) -> Double.compare(
                calculateDistance(currentPos, r1.getPosition()),
                calculateDistance(currentPos, r2.getPosition())
            ))
            .orElse(null);
        
        if (nearest == null) {
            setResult(ActionResult.failure("Не удалось найти ближайший ресурс"));
            return;
        }
        
        double distance = calculateDistance(currentPos, nearest.getPosition());
        String message = String.format("Найден %s в %.1f блоках: (%d, %d, %d)", 
            resourceType, distance, 
            nearest.getPosition().getX(), nearest.getPosition().getY(), nearest.getPosition().getZ());
        
        crafto.sendChatMessage(message);
        
        // Проверяем, есть ли оптимальный путь через путевые точки
        Optional<NavigationPath> pathOpt = crafto.getWaypointSystem().findPath(currentPos, nearest.getPosition());
        
        if (pathOpt.isPresent()) {
            NavigationPath path = pathOpt.get();
            crafto.sendChatMessage(String.format("Найден оптимальный маршрут: %.1f блоков, ~%.1f сек", 
                path.getTotalDistance(), path.getEstimatedTime()));
        }
        
        // Создаем временную путевую точку для навигации
        String tempWaypointName = "temp_resource_" + System.currentTimeMillis();
        crafto.getWaypointSystem().createWaypoint(
            tempWaypointName,
            nearest.getPosition(),
            com.crafto.ai.exploration.WaypointType.RESOURCE_SITE,
            "Временная точка для " + resourceType
        );
        
        // Начинаем навигацию
        navigateAction = new NavigateToWaypointAction(crafto, tempWaypointName);
        navigateAction.start();
        resourceFound = true;
    }

    @Override
    public void tick() {
        if (navigateAction != null) {
            navigateAction.tick();
            
            if (navigateAction.isComplete()) {
                ActionResult navResult = navigateAction.getResult();
                
                if (navResult.isSuccess()) {
                    String successMsg = "Достигнут ресурс: " + resourceType;
                    crafto.sendChatMessage(successMsg);
                    setResult(ActionResult.success(successMsg));
                } else {
                    String errorMsg = "Не удалось достичь ресурса: " + navResult.getMessage();
                    crafto.sendChatMessage(errorMsg);
                    setResult(ActionResult.failure(errorMsg));
                }
                
                navigateAction = null;
            }
        }
    }

    @Override
    public void cancel() {
        if (navigateAction != null) {
            navigateAction.cancel();
            navigateAction = null;
        }
        crafto.sendChatMessage("Поиск ресурса '" + resourceType + "' отменен");
        setResult(ActionResult.failure("Поиск ресурса отменен"));
    }

    @Override
    public boolean isComplete() {
        return getResult() != null;
    }

    @Override
    public String getDescription() {
        return "Поиск и навигация к ресурсу: " + resourceType;
    }
    
    private double calculateDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(
            Math.pow(pos1.getX() - pos2.getX(), 2) +
            Math.pow(pos1.getY() - pos2.getY(), 2) +
            Math.pow(pos1.getZ() - pos2.getZ(), 2)
        );
    }
}