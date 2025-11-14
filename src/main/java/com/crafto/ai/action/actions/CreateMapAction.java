package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import com.crafto.ai.exploration.SharedMap;
import net.minecraft.core.BlockPos;

/**
 * Действие для создания и экспорта карты области
 */
public class CreateMapAction extends BaseAction {
    private final BlockPos centerPos;
    private final int radius;
    private final String format;
    
    public CreateMapAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
        
        this.centerPos = new BlockPos(
            task.getIntParameter("x", crafto.blockPosition().getX()),
            task.getIntParameter("y", crafto.blockPosition().getY()),
            task.getIntParameter("z", crafto.blockPosition().getZ())
        );
        this.radius = task.getIntParameter("radius", 200);
        this.format = task.getStringParameter("format", "text");
    }
    
    public CreateMapAction(CraftoEntity crafto, BlockPos centerPos, int radius, String format) {
        super(crafto, createMapTask(centerPos, radius, format));
        this.centerPos = centerPos;
        this.radius = radius;
        this.format = format;
    }
    
    private static Task createMapTask(BlockPos pos, int radius, String format) {
        Task task = new Task("create_map");
        task.addParameter("x", pos.getX());
        task.addParameter("y", pos.getY());
        task.addParameter("z", pos.getZ());
        task.addParameter("radius", radius);
        task.addParameter("format", format);
        return task;
    }

    @Override
    public void start() {
        try {
            crafto.sendChatMessage("Создаю карту области радиусом " + radius + " блоков...");
            
            // Создаем карту
            String mapName = "Map_" + System.currentTimeMillis();
            SharedMap map = crafto.getMapSystem().createSharedMap(mapName, centerPos, radius);
            
            // Экспортируем в нужном формате
            String mapData;
            if ("json".equalsIgnoreCase(format)) {
                mapData = crafto.getMapSystem().exportMapAsJson(map.getMapId());
                crafto.sendChatMessage("Карта создана в JSON формате (ID: " + map.getMapId() + ")");
            } else {
                mapData = crafto.getMapSystem().exportMapAsText(map.getMapId());
                crafto.sendChatMessage("Карта области:");
                
                // Разбиваем большую карту на части для чата
                String[] lines = mapData.split("\n");
                for (int i = 0; i < Math.min(lines.length, 20); i++) { // Показываем только первые 20 строк
                    crafto.sendChatMessage(lines[i]);
                }
                
                if (lines.length > 20) {
                    crafto.sendChatMessage("... (карта обрезана, полная версия сохранена)");
                }
            }
            
            // Получаем статистику карты
            com.crafto.ai.exploration.MapStats stats = crafto.getMapSystem().getMapStats();
            String statsMsg = String.format("Статистика: %d карт, %d маркеров, исследовано %d блоков²", 
                stats.getTotalMaps(), stats.getTotalMarkers(), stats.getExploredArea());
            
            crafto.sendChatMessage(statsMsg);
            setResult(ActionResult.success("Карта создана: " + mapName));
            
        } catch (Exception e) {
            String errorMsg = "Ошибка при создании карты: " + e.getMessage();
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
        setResult(ActionResult.failure("Создание карты отменено"));
    }

    @Override
    public boolean isComplete() {
        return getResult() != null;
    }

    @Override
    public String getDescription() {
        return String.format("Создание карты области радиусом %d блоков в формате %s", radius, format);
    }
}