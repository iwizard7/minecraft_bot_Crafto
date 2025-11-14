package com.crafto.ai.exploration;

import java.util.Objects;

/**
 * Координаты чанка для системы исследования
 */
public class ChunkCoordinate {
    public final int x;
    public final int z;
    
    public ChunkCoordinate(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChunkCoordinate that = (ChunkCoordinate) obj;
        return x == that.x && z == that.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
    
    @Override
    public String toString() {
        return String.format("ChunkCoordinate{x=%d, z=%d}", x, z);
    }
    
    /**
     * Вычисляет расстояние до другой координаты чанка
     */
    public double distanceTo(ChunkCoordinate other) {
        double dx = this.x - other.x;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Получает соседние координаты чанков
     */
    public ChunkCoordinate[] getNeighbors() {
        return new ChunkCoordinate[] {
            new ChunkCoordinate(x + 1, z),     // Восток
            new ChunkCoordinate(x - 1, z),     // Запад
            new ChunkCoordinate(x, z + 1),     // Юг
            new ChunkCoordinate(x, z - 1),     // Север
            new ChunkCoordinate(x + 1, z + 1), // Юго-восток
            new ChunkCoordinate(x - 1, z + 1), // Юго-запад
            new ChunkCoordinate(x + 1, z - 1), // Северо-восток
            new ChunkCoordinate(x - 1, z - 1)  // Северо-запад
        };
    }
}