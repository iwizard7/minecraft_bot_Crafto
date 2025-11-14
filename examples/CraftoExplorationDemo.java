package examples;

/**
 * Демонстрация использования системы исследования через бота Crafto
 * 
 * Этот пример показывает, как игроки могут использовать систему исследования
 * через естественные команды боту Crafto.
 */
public class CraftoExplorationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ДЕМОНСТРАЦИЯ СИСТЕМЫ ИССЛЕДОВАНИЯ CRAFTO ===\n");
        
        demonstrateBasicUsage();
        demonstrateAICommands();
        demonstrateDirectCommands();
        demonstratePracticalScenarios();
    }
    
    /**
     * Базовое использование
     */
    private static void demonstrateBasicUsage() {
        System.out.println("1. БАЗОВОЕ ИСПОЛЬЗОВАНИЕ");
        System.out.println("========================");
        System.out.println();
        
        System.out.println("Шаг 1: Создание бота");
        System.out.println("Команда: /crafto spawn Explorer");
        System.out.println("Результат: Создан бот 'Explorer' с системой исследования");
        System.out.println();
        
        System.out.println("Шаг 2: Первая команда исследования");
        System.out.println("Команда: /crafto tell Explorer исследуй область");
        System.out.println("Результат: Бот начинает исследование 64 блоков вокруг");
        System.out.println("- Сканирует блоки по чанкам");
        System.out.println("- Находит ресурсы и структуры");
        System.out.println("- Оценивает уровень опасности");
        System.out.println("- Автоматически создает путевые точки для ценных ресурсов");
        System.out.println();
        
        System.out.println("Шаг 3: Просмотр результатов");
        System.out.println("Команда: /crafto stats Explorer");
        System.out.println("Результат: Показывает статистику исследования");
        System.out.println("- Количество исследованных областей");
        System.out.println("- Найденные ресурсы по типам");
        System.out.println("- Созданные путевые точки");
        System.out.println();
    }
    
    /**
     * AI команды (естественный язык)
     */
    private static void demonstrateAICommands() {
        System.out.println("2. AI КОМАНДЫ (ЕСТЕСТВЕННЫЙ ЯЗЫК)");
        System.out.println("==================================");
        System.out.println();
        
        System.out.println("Исследование:");
        System.out.println("/crafto tell Explorer исследуй область");
        System.out.println("/crafto tell Explorer explore area");
        System.out.println("/crafto tell Explorer исследуй 200 блоков");
        System.out.println();
        
        System.out.println("Поиск ресурсов:");
        System.out.println("/crafto tell Explorer найди алмазы");
        System.out.println("/crafto tell Explorer find diamonds");
        System.out.println("/crafto tell Explorer найди железо");
        System.out.println();
        
        System.out.println("Путевые точки:");
        System.out.println("/crafto tell Explorer создай точку дом");
        System.out.println("/crafto tell Explorer create waypoint mine");
        System.out.println("/crafto tell Explorer иди домой");
        System.out.println("/crafto tell Explorer go to mine");
        System.out.println();
        
        System.out.println("Карты:");
        System.out.println("/crafto tell Explorer создай карту");
        System.out.println("/crafto tell Explorer create map");
        System.out.println();
    }
    
    /**
     * Прямые команды (точный контроль)
     */
    private static void demonstrateDirectCommands() {
        System.out.println("3. ПРЯМЫЕ КОМАНДЫ (ТОЧНЫЙ КОНТРОЛЬ)");
        System.out.println("===================================");
        System.out.println();
        
        System.out.println("Исследование:");
        System.out.println("/crafto explore Explorer 128    # Исследовать радиус 128 блоков");
        System.out.println("/crafto explore Explorer 500    # Большая область");
        System.out.println();
        
        System.out.println("Путевые точки:");
        System.out.println("/crafto waypoint create Explorer home BASE");
        System.out.println("/crafto waypoint create Explorer mine MINE");
        System.out.println("/crafto waypoint create Explorer farm FARM");
        System.out.println("/crafto waypoint navigate Explorer home");
        System.out.println();
        
        System.out.println("Поиск ресурсов:");
        System.out.println("/crafto find Explorer diamond_ore");
        System.out.println("/crafto find Explorer iron_ore");
        System.out.println("/crafto find Explorer gold_ore");
        System.out.println();
        
        System.out.println("Карты и статистика:");
        System.out.println("/crafto map create Explorer 300");
        System.out.println("/crafto stats Explorer");
        System.out.println();
    }
    
    /**
     * Практические сценарии
     */
    private static void demonstratePracticalScenarios() {
        System.out.println("4. ПРАКТИЧЕСКИЕ СЦЕНАРИИ");
        System.out.println("========================");
        System.out.println();
        
        System.out.println("СЦЕНАРИЙ 1: Первое исследование новой области");
        System.out.println("----------------------------------------------");
        System.out.println("1. /crafto spawn Explorer");
        System.out.println("2. /crafto tell Explorer создай точку спавн");
        System.out.println("3. /crafto tell Explorer исследуй область радиусом 200 блоков");
        System.out.println("4. /crafto stats Explorer");
        System.out.println("5. /crafto tell Explorer создай карту");
        System.out.println();
        System.out.println("Результат: Полное исследование области с картой и статистикой");
        System.out.println();
        
        System.out.println("СЦЕНАРИЙ 2: Поиск алмазов для крафта");
        System.out.println("------------------------------------");
        System.out.println("1. /crafto tell Explorer найди алмазы");
        System.out.println("2. Если не найдены: /crafto tell Explorer исследуй область радиусом 500 блоков");
        System.out.println("3. /crafto find Explorer diamond_ore");
        System.out.println("4. /crafto tell Explorer иди к ближайшим алмазам");
        System.out.println();
        System.out.println("Результат: Бот найдет и проведет к ближайшим алмазам");
        System.out.println();
        
        System.out.println("СЦЕНАРИЙ 3: Создание навигационной сети");
        System.out.println("---------------------------------------");
        System.out.println("1. /crafto waypoint create Explorer spawn BASE");
        System.out.println("2. /crafto waypoint create Explorer mine MINE");
        System.out.println("3. /crafto waypoint create Explorer farm FARM");
        System.out.println("4. /crafto tell Explorer иди к шахте");
        System.out.println("5. /crafto tell Explorer иди на ферму");
        System.out.println("6. /crafto tell Explorer иди домой");
        System.out.println();
        System.out.println("Результат: Сеть путевых точек с оптимальной навигацией");
        System.out.println();
        
        System.out.println("СЦЕНАРИЙ 4: Автоматическое картографирование");
        System.out.println("--------------------------------------------");
        System.out.println("1. /crafto tell Explorer исследуй область радиусом 1000 блоков");
        System.out.println("2. Ждем завершения (бот будет периодически сообщать прогресс)");
        System.out.println("3. /crafto map create Explorer 1000");
        System.out.println("4. /crafto stats Explorer");
        System.out.println();
        System.out.println("Результат: Подробная карта большой области с всеми ресурсами");
        System.out.println();
        
        System.out.println("АВТОМАТИЧЕСКИЕ ФУНКЦИИ:");
        System.out.println("- Бот автоматически создает путевые точки для ценных ресурсов");
        System.out.println("- Обновляет карты при каждом исследовании");
        System.out.println("- Сохраняет все данные в JSON файлы");
        System.out.println("- Находит оптимальные пути между точками");
        System.out.println("- Избегает опасных зон при навигации");
        System.out.println();
        
        System.out.println("ИНТЕГРАЦИЯ С ДРУГИМИ СИСТЕМАМИ:");
        System.out.println("- Долгосрочная память: запоминает успешные стратегии исследования");
        System.out.println("- Планировщик задач: автоматически планирует исследования");
        System.out.println("- Система строительства: использует найденные ресурсы");
        System.out.println();
    }
}

/**
 * Пример интеграции с игровым циклом
 */
class GameLoopIntegration {
    
    /**
     * Пример того, как система исследования работает в игровом цикле
     */
    public void demonstrateGameLoop() {
        System.out.println("=== ИНТЕГРАЦИЯ С ИГРОВЫМ ЦИКЛОМ ===");
        System.out.println();
        
        System.out.println("1. Игрок создает бота:");
        System.out.println("   /crafto spawn MyExplorer");
        System.out.println();
        
        System.out.println("2. Игрок дает команду:");
        System.out.println("   /crafto tell MyExplorer исследуй область");
        System.out.println();
        
        System.out.println("3. Система обрабатывает команду:");
        System.out.println("   - TaskPlanner парсит команду в JSON");
        System.out.println("   - ActionExecutor создает ExploreAreaAction");
        System.out.println("   - ExploreAreaAction запускает ExplorationSystem.exploreArea()");
        System.out.println();
        
        System.out.println("4. Исследование выполняется асинхронно:");
        System.out.println("   - Сканирование блоков по чанкам");
        System.out.println("   - Поиск ресурсов и структур");
        System.out.println("   - Оценка опасностей");
        System.out.println("   - Создание путевых точек");
        System.out.println();
        
        System.out.println("5. Результаты сохраняются:");
        System.out.println("   - config/crafto/exploration/explored_areas.json");
        System.out.println("   - config/crafto/exploration/resource_locations.json");
        System.out.println("   - config/crafto/waypoints/waypoints.json");
        System.out.println();
        
        System.out.println("6. Игрок получает уведомления:");
        System.out.println("   - \"Исследование завершено! Найдено: 25 областей, 47 ресурсов\"");
        System.out.println("   - \"Создана путевая точка: diamond_ore_1234 в (123, 45, 678)\"");
        System.out.println();
        
        System.out.println("7. Игрок может использовать результаты:");
        System.out.println("   /crafto tell MyExplorer найди алмазы");
        System.out.println("   /crafto tell MyExplorer иди к diamond_ore_1234");
        System.out.println("   /crafto stats MyExplorer");
        System.out.println();
    }
}

/**
 * Примеры команд для копирования
 */
class CommandExamples {
    
    public static void printAllCommands() {
        System.out.println("=== КОМАНДЫ ДЛЯ КОПИРОВАНИЯ ===");
        System.out.println();
        
        System.out.println("# Создание и управление ботом");
        System.out.println("/crafto spawn Explorer");
        System.out.println("/crafto list");
        System.out.println("/crafto remove Explorer");
        System.out.println();
        
        System.out.println("# AI команды (естественный язык)");
        System.out.println("/crafto tell Explorer исследуй область");
        System.out.println("/crafto tell Explorer найди алмазы");
        System.out.println("/crafto tell Explorer создай точку дом");
        System.out.println("/crafto tell Explorer иди домой");
        System.out.println("/crafto tell Explorer создай карту");
        System.out.println();
        
        System.out.println("# Прямые команды исследования");
        System.out.println("/crafto explore Explorer 128");
        System.out.println("/crafto find Explorer diamond_ore");
        System.out.println("/crafto waypoint create Explorer home BASE");
        System.out.println("/crafto waypoint navigate Explorer home");
        System.out.println("/crafto map create Explorer 200");
        System.out.println("/crafto stats Explorer");
        System.out.println();
        
        System.out.println("# Типы ресурсов для поиска");
        System.out.println("diamond_ore, iron_ore, gold_ore, coal_ore, copper_ore");
        System.out.println("redstone_ore, lapis_ore, emerald_ore, ancient_debris");
        System.out.println();
        
        System.out.println("# Типы путевых точек");
        System.out.println("BASE, MINE, FARM, LANDMARK, TRADING_POST, DANGER_ZONE");
        System.out.println();
    }
}