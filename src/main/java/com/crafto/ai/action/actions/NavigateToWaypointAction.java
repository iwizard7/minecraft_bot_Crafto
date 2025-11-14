package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.exploration.NavigationPath;
import com.crafto.ai.exploration.Waypoint;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * Действие для навигации к путевой точке
 */
public class NavigateToWaypointAction extends BaseAction {
    private final String waypointName;
    private NavigationPath navigationPath;
    private List<BlockPos> waypoints;
    private int currentWaypointIndex = 0;
    private PathfindAction currentPathfindAction;
    
    public NavigateToWaypointAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
        this.waypointName = task.getStringParameter("waypoint", "");
    }
    
    public NavigateToWaypointAction(CraftoEntity crafto, String waypointName) {
        super(crafto, createNavigateTask(waypointName));
        this.waypointName = waypointName;
    }
    
    private static Task createNavigateTask(String waypointName) {
        Task task = new Task("navigate_to_waypoint");
        task.addParameter("waypoint", waypointName);
        return task;
    }

    @Override
    public void start() {
        if (waypointName.isEmpty()) {
            setResult(ActionResult.failure("Не указано имя путевой точки"));
            return;
        }
        
        // Найти путевую точку
        Optional<Waypoint> waypoint = crafto.getWaypointSystem().getWaypoint(waypointName);
        if (!waypoint.isPresent()) {
            String errorMsg = "Путевая точка '" + waypointName + "' не найдена";
            crafto.sendChatMessage(errorMsg);
            setResult(ActionResult.failure(errorMsg));
            return;
        }
        
        BlockPos destination = waypoint.get().getPosition();
        BlockPos currentPos = crafto.blockPosition();
        
        // Найти оптимальный путь
        Optional<NavigationPath> pathOpt = crafto.getWaypointSystem().findPath(currentPos, destination);
        
        if (!pathOpt.isPresent()) {
            // Если нет оптимального пути через дороги, идем напрямую
            crafto.sendChatMessage("Прямой путь к '" + waypointName + "' в " + destination);
            currentPathfindAction = new PathfindAction(crafto, createPathfindTask(destination));
            currentPathfindAction.start();
            return;
        }
        
        navigationPath = pathOpt.get();
        waypoints = navigationPath.getWaypoints();
        
        String message = String.format("Навигация к '%s': %d точек маршрута, расстояние %.1f блоков, время ~%.1f сек", 
            waypointName, waypoints.size(), navigationPath.getTotalDistance(), navigationPath.getEstimatedTime());
        
        crafto.sendChatMessage(message);
        
        // Начинаем движение к первой точке маршрута
        if (!waypoints.isEmpty()) {
            moveToNextWaypoint();
        } else {
            setResult(ActionResult.failure("Пустой маршрут"));
        }
    }

    @Override
    public void tick() {
        if (currentPathfindAction != null) {
            currentPathfindAction.tick();
            
            if (currentPathfindAction.isComplete()) {
                ActionResult pathResult = currentPathfindAction.getResult();
                
                if (pathResult.isSuccess()) {
                    // Достигли текущей точки маршрута
                    currentWaypointIndex++;
                    
                    if (currentWaypointIndex < waypoints.size()) {
                        // Есть еще точки в маршруте
                        moveToNextWaypoint();
                    } else {
                        // Достигли конечной точки
                        String successMsg = "Достигнута путевая точка '" + waypointName + "'";
                        crafto.sendChatMessage(successMsg);
                        setResult(ActionResult.success(successMsg));
                    }
                } else {
                    // Ошибка при движении к точке
                    String errorMsg = "Не удалось достичь точки маршрута: " + pathResult.getMessage();
                    crafto.sendChatMessage(errorMsg);
                    setResult(ActionResult.failure(errorMsg));
                }
                
                currentPathfindAction = null;
            }
        }
    }

    @Override
    public void cancel() {
        if (currentPathfindAction != null) {
            currentPathfindAction.cancel();
            currentPathfindAction = null;
        }
        crafto.sendChatMessage("Навигация к '" + waypointName + "' отменена");
        setResult(ActionResult.failure("Навигация отменена"));
    }

    @Override
    public boolean isComplete() {
        return getResult() != null;
    }

    @Override
    public String getDescription() {
        return "Навигация к путевой точке '" + waypointName + "'";
    }
    
    private void moveToNextWaypoint() {
        if (currentWaypointIndex >= waypoints.size()) {
            return;
        }
        
        BlockPos nextPos = waypoints.get(currentWaypointIndex);
        currentPathfindAction = new PathfindAction(crafto, createPathfindTask(nextPos));
        currentPathfindAction.start();
        
        crafto.sendChatMessage(String.format("Движение к точке %d/%d: (%d, %d, %d)", 
            currentWaypointIndex + 1, waypoints.size(), nextPos.getX(), nextPos.getY(), nextPos.getZ()));
    }
    
    private Task createPathfindTask(BlockPos pos) {
        Task task = new Task("pathfind");
        task.addParameter("x", pos.getX());
        task.addParameter("y", pos.getY());
        task.addParameter("z", pos.getZ());
        return task;
    }
}