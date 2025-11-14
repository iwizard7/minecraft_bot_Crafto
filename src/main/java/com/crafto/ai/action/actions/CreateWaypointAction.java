package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.exploration.Waypoint;
import com.crafto.ai.exploration.WaypointType;
import net.minecraft.core.BlockPos;

/**
 * Действие для создания путевой точки
 */
public class CreateWaypointAction extends BaseAction {
    private final String waypointName;
    private final BlockPos position;
    private final WaypointType type;
    private final String description;
    
    public CreateWaypointAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
        
        this.waypointName = task.getStringParameter("name", "Waypoint_" + System.currentTimeMillis());
        this.position = new BlockPos(
            task.getIntParameter("x", crafto.blockPosition().getX()),
            task.getIntParameter("y", crafto.blockPosition().getY()),
            task.getIntParameter("z", crafto.blockPosition().getZ())
        );
        
        String typeStr = task.getStringParameter("type", "LANDMARK");
        this.type = parseWaypointType(typeStr);
        this.description = task.getStringParameter("description", "Создано ботом " + crafto.getCraftoName());
    }
    
    public CreateWaypointAction(CraftoEntity crafto, String name, BlockPos pos, WaypointType type, String description) {
        super(crafto, createWaypointTask(name, pos, type, description));
        this.waypointName = name;
        this.position = pos;
        this.type = type;
        this.description = description;
    }
    
    private static Task createWaypointTask(String name, BlockPos pos, WaypointType type, String description) {
        Task task = new Task("create_waypoint");
        task.addParameter("name", name);
        task.addParameter("x", pos.getX());
        task.addParameter("y", pos.getY());
        task.addParameter("z", pos.getZ());
        task.addParameter("type", type.name());
        task.addParameter("description", description);
        return task;
    }

    @Override
    public void start() {
        try {
            Waypoint waypoint = crafto.getWaypointSystem().createWaypoint(waypointName, position, type, description);
            
            String message = String.format("Создана путевая точка '%s' типа %s в позиции (%d, %d, %d)", 
                waypointName, type.name(), position.getX(), position.getY(), position.getZ());
            
            crafto.sendChatMessage(message);
            setResult(ActionResult.success("Путевая точка создана: " + waypointName));
            
        } catch (Exception e) {
            String errorMsg = "Ошибка при создании путевой точки: " + e.getMessage();
            crafto.sendChatMessage(errorMsg);
            setResult(ActionResult.failure(errorMsg));
        }
    }

    @Override
    public void tick() {
        // Действие выполняется мгновенно в start()
    }

    @Override
    public void cancel() {
        setResult(ActionResult.failure("Создание путевой точки отменено"));
    }

    @Override
    public boolean isComplete() {
        return getResult() != null;
    }

    @Override
    public String getDescription() {
        return String.format("Создание путевой точки '%s' типа %s", waypointName, type.name());
    }
    
    private WaypointType parseWaypointType(String typeStr) {
        try {
            return WaypointType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Попробуем найти подходящий тип по ключевым словам
            String lower = typeStr.toLowerCase();
            if (lower.contains("base") || lower.contains("home") || lower.contains("база")) {
                return WaypointType.BASE;
            } else if (lower.contains("mine") || lower.contains("шахта") || lower.contains("руда")) {
                return WaypointType.MINE;
            } else if (lower.contains("farm") || lower.contains("ферма")) {
                return WaypointType.FARM;
            } else if (lower.contains("trade") || lower.contains("торговля")) {
                return WaypointType.TRADING_POST;
            } else if (lower.contains("danger") || lower.contains("опасность")) {
                return WaypointType.DANGER_ZONE;
            } else if (lower.contains("resource") || lower.contains("ресурс")) {
                return WaypointType.RESOURCE_SITE;
            } else if (lower.contains("portal") || lower.contains("портал")) {
                return WaypointType.PORTAL;
            } else {
                return WaypointType.LANDMARK; // по умолчанию
            }
        }
    }
}