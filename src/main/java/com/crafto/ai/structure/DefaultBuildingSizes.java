package com.crafto.ai.structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Размеры по умолчанию для различных типов строений
 */
public class DefaultBuildingSizes {
    
    public static class BuildingSize {
        public final int width;
        public final int height;
        public final int depth;
        
        public BuildingSize(int width, int height, int depth) {
            this.width = width;
            this.height = height;
            this.depth = depth;
        }
    }
    
    private static final Map<String, BuildingSize> DEFAULT_SIZES = new HashMap<>();
    
    static {
        // Жилые здания
        DEFAULT_SIZES.put("house", new BuildingSize(12, 8, 10));
        DEFAULT_SIZES.put("home", new BuildingSize(12, 8, 10));
        DEFAULT_SIZES.put("mansion", new BuildingSize(20, 12, 16));
        DEFAULT_SIZES.put("villa", new BuildingSize(16, 8, 12));
        DEFAULT_SIZES.put("cottage", new BuildingSize(8, 6, 8));
        DEFAULT_SIZES.put("apartment", new BuildingSize(12, 16, 8));
        DEFAULT_SIZES.put("modern", new BuildingSize(14, 6, 10));
        DEFAULT_SIZES.put("modern_house", new BuildingSize(14, 6, 10));
        
        // Современные здания
        DEFAULT_SIZES.put("skyscraper", new BuildingSize(12, 32, 8));
        
        // Исторические строения
        DEFAULT_SIZES.put("castle", new BuildingSize(20, 12, 16));
        DEFAULT_SIZES.put("fort", new BuildingSize(20, 12, 16));
        DEFAULT_SIZES.put("tower", new BuildingSize(8, 20, 8));
        
        // Простые структуры
        DEFAULT_SIZES.put("wall", new BuildingSize(20, 6, 1));
        DEFAULT_SIZES.put("platform", new BuildingSize(10, 1, 10));
        DEFAULT_SIZES.put("barn", new BuildingSize(12, 8, 10));
        DEFAULT_SIZES.put("shed", new BuildingSize(12, 8, 10));
        DEFAULT_SIZES.put("box", new BuildingSize(5, 5, 5));
        DEFAULT_SIZES.put("cube", new BuildingSize(5, 5, 5));
    }
    
    /**
     * Получить размер по умолчанию для типа строения
     */
    public static BuildingSize getDefaultSize(String structureType) {
        BuildingSize size = DEFAULT_SIZES.get(structureType.toLowerCase());
        if (size != null) {
            return size;
        }
        
        // Размер по умолчанию для неизвестных типов
        return new BuildingSize(12, 8, 10);
    }
    
    /**
     * Проверить, поддерживается ли тип строения
     */
    public static boolean isSupported(String structureType) {
        return DEFAULT_SIZES.containsKey(structureType.toLowerCase());
    }
    
    /**
     * Получить все поддерживаемые типы строений
     */
    public static String[] getSupportedTypes() {
        return DEFAULT_SIZES.keySet().toArray(new String[0]);
    }
    
    /**
     * Применить размеры по умолчанию если параметры не заданы или равны 0
     */
    public static BuildingSize applyDefaults(String structureType, int width, int height, int depth) {
        BuildingSize defaultSize = getDefaultSize(structureType);
        
        int finalWidth = (width <= 0) ? defaultSize.width : width;
        int finalHeight = (height <= 0) ? defaultSize.height : height;
        int finalDepth = (depth <= 0) ? defaultSize.depth : depth;
        
        return new BuildingSize(finalWidth, finalHeight, finalDepth);
    }
}