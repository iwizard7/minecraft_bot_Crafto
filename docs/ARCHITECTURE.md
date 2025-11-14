# 🏗️ Архитектура системы строительства по NBT шаблонам

## Обзор системы

```
┌─────────────────────────────────────────────────────────────┐
│                    Minecraft + Forge                         │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │              Crafto AI Mod                          │    │
│  │                                                      │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │         Command Processing                    │  │    │
│  │  │                                               │  │    │
│  │  │  Player: "@Alex build big house"            │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  PromptBuilder → AI (Ollama)                │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Task: {"action": "build",                  │  │    │
│  │  │         "parameters": {"structure":          │  │    │
│  │  │                       "big-house"}}          │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                      ↓                              │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │         Action Execution                      │  │    │
│  │  │                                               │  │    │
│  │  │  ActionExecutor                              │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  BuildStructureAction                        │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  tryLoadFromTemplate("big-house")           │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                      ↓                              │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │         Template Loading                      │  │    │
│  │  │                                               │  │    │
│  │  │  StructureTemplateLoader                     │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  loadFromNBT(level, "big-house")            │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Read: structures/big-house.nbt             │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Parse NBT: palette + blocks                │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Return: List<TemplateBlock>                │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                      ↓                              │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │         Collaborative Building                │  │    │
│  │  │                                               │  │    │
│  │  │  CollaborativeBuildManager                   │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Register build or join existing            │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Distribute blocks among bots               │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Each bot places blocks (1/tick)            │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Track progress (blocks placed/total)       │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                      ↓                              │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │         Inventory Management                  │  │    │
│  │  │                                               │  │    │
│  │  │  InventoryManager                            │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Auto-refill materials                       │  │    │
│  │  │           ↓                                   │  │    │
│  │  │  Consume blocks on placement                │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │                      ↓                              │    │
│  │              🏠 Built House!                        │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Компоненты системы

### 1. Генерация NBT (Startup)

```
Mod Initialization
        ↓
CraftoMod.commonSetup()
        ↓
    ┌───────────────────────────────┐
    │  BigHouseGenerator.initialize() │
    └───────────────────────────────┘
        ↓
    Check: big-house.nbt exists?
        ↓
    ┌─────────────┬─────────────┐
    │    Yes      │     No      │
    │   Skip      │  Generate   │
    └─────────────┴─────────────┘
                  ↓
        generateBigHouseNBT()
                  ↓
        Create structure data:
        - Floor (15×12)
        - Walls (4 sides, 2 floors)
        - Windows (20+)
        - Doors (entrance + internal)
        - Stairs (between floors)
        - Roof (multi-level)
        - Furniture (all rooms)
                  ↓
        Build palette:
        - oak_planks
        - stone_bricks
        - glass_pane
        - oak_door
        - etc.
                  ↓
        Save to NBT format
                  ↓
    structures/big-house.nbt
```

### 2. Конвертация Schematic (Startup)

```
Mod Initialization
        ↓
SchematicConverter.convertAllSchematics()
        ↓
    Scan structures/ folder
        ↓
    Find *.schematic files
        ↓
    For each schematic:
        ↓
    ┌─────────────────────────────┐
    │  convertSchematicToNBT()    │
    └─────────────────────────────┘
        ↓
    Read schematic data:
    - Width, Height, Length
    - Block IDs (legacy)
    - Block data
        ↓
    Convert legacy IDs:
    - 1 → stone
    - 5 → oak_planks
    - 20 → glass
    - etc.
        ↓
    Create NBT structure:
    - size: [w, h, l]
    - palette: [blocks]
    - blocks: [{pos, state}]
        ↓
    Save as .nbt file
```

### 3. Загрузка шаблона (Runtime)

```
BuildStructureAction.onStart()
        ↓
tryLoadFromTemplate("big-house", startPos)
        ↓
    ┌─────────────────────────────────┐
    │  StructureTemplateLoader        │
    │  .loadFromNBT(level, name)      │
    └─────────────────────────────────┘
        ↓
    Search for NBT file:
    - structures/big-house.nbt
    - structures/big house.nbt
    - Fuzzy match
        ↓
    ┌──────────┬──────────┐
    │  Found   │ Not Found│
    │          │  Return  │
    │          │   null   │
    └──────────┴──────────┘
        ↓
    Read compressed NBT
        ↓
    Parse structure:
    - size: [15, 8, 12]
    - palette: 10-15 blocks
    - blocks: 800-1000 entries
        ↓
    Create LoadedTemplate:
    - name: "big-house"
    - blocks: List<TemplateBlock>
    - dimensions: w×h×d
        ↓
    Return to BuildStructureAction
```

### 4. Строительство (Runtime)

```
BuildStructureAction.onTick()
        ↓
    ┌─────────────────────────────────┐
    │  CollaborativeBuildManager      │
    └─────────────────────────────────┘
        ↓
    Check: existing build?
        ↓
    ┌──────────┬──────────┐
    │   Yes    │    No    │
    │   Join   │  Create  │
    └──────────┴──────────┘
        ↓
    Register bot as participant
        ↓
    Enable flying mode
        ↓
    ┌─────────────────────────────────┐
    │  Building Loop (every tick)     │
    └─────────────────────────────────┘
        ↓
    For 1 block per tick:
        ↓
    getNextBlock(build, botName)
        ↓
    ┌──────────┬──────────┐
    │  Block   │   None   │
    │Available │  (idle)  │
    └──────────┴──────────┘
        ↓
    Check inventory
        ↓
    ┌──────────┬──────────┐
    │   Has    │  Needs   │
    │  Block   │  Refill  │
    └──────────┴──────────┘
        ↓
    Teleport if far (>5 blocks)
        ↓
    Look at block position
        ↓
    Swing hand (animation)
        ↓
    Consume block from inventory
        ↓
    Place block in world
        ↓
    Spawn particles + sound
        ↓
    Mark block as placed
        ↓
    Update progress
        ↓
    Check: build complete?
        ↓
    ┌──────────┬──────────┐
    │   Yes    │    No    │
    │ Finish   │ Continue │
    └──────────┴──────────┘
        ↓
    Disable flying
        ↓
    Return success
```

## Структура данных

### NBT Format

```nbt
{
  "size": [15, 8, 12],           // Width, Height, Depth
  
  "palette": [                    // Block types used
    {"Name": "minecraft:oak_planks"},
    {"Name": "minecraft:stone_bricks"},
    {"Name": "minecraft:glass_pane"},
    {"Name": "minecraft:oak_door"},
    ...
  ],
  
  "blocks": [                     // Block placements
    {
      "state": 0,                 // Index in palette
      "pos": [0, 0, 0]           // Relative position
    },
    {
      "state": 1,
      "pos": [1, 0, 0]
    },
    ...
  ]
}
```

### LoadedTemplate

```java
class LoadedTemplate {
    String name;                    // "big-house"
    List<TemplateBlock> blocks;     // ~800-1000 blocks
    int width;                      // 15
    int height;                     // 8
    int depth;                      // 12
}

class TemplateBlock {
    BlockPos relativePos;           // Position in template
    BlockState blockState;          // Block type + properties
}
```

### CollaborativeBuild

```java
class CollaborativeBuild {
    String structureId;                      // "big-house-12345"
    BlockPos startPos;                       // World position
    List<BlockPlacement> allBlocks;          // All blocks to place
    Set<String> participatingCraftos;        // Bot names
    Map<String, BlockPlacement> assignments; // Bot → current block
    Set<BlockPos> placedBlocks;             // Completed blocks
    
    int getTotalBlocks();                    // Total count
    int getBlocksPlaced();                   // Completed count
    int getProgressPercentage();             // % complete
    boolean isComplete();                    // All done?
}
```

## Поток данных

### 1. Команда → Задача

```
Player Input: "@Alex build big house"
        ↓
PromptBuilder.buildSystemPrompt()
    + buildUserPrompt(crafto, command, worldKnowledge)
        ↓
Send to Ollama AI
        ↓
AI Response (JSON):
{
  "reasoning": "Building a large house from template",
  "plan": "Build big house from NBT template",
  "tasks": [
    {
      "action": "build",
      "parameters": {
        "structure": "big-house"
      }
    }
  ]
}
        ↓
ResponseParser.parseAIResponse()
        ↓
Task object:
    action = "build"
    parameters = {"structure": "big-house"}
```

### 2. Задача → Действие

```
Task
        ↓
ActionExecutor.executeTask(task)
        ↓
Switch on task.action:
    case "build":
        ↓
    new BuildStructureAction(crafto, task)
        ↓
    action.start()
```

### 3. Действие → Строительство

```
BuildStructureAction.onStart()
        ↓
structureType = "big-house"
        ↓
tryLoadFromTemplate("big-house", groundPos)
        ↓
LoadedTemplate template
        ↓
Convert to List<BlockPlacement>
        ↓
CollaborativeBuildManager.registerBuild()
        ↓
BuildStructureAction.onTick() (every tick)
        ↓
Place blocks (1 per tick)
        ↓
Update progress
        ↓
Complete when all blocks placed
```

## Производительность

### Оптимизации

1. **Сжатие NBT**
   - Использование NbtIo.writeCompressed()
   - Размер файла: ~2-5 KB вместо ~10-20 KB

2. **Ленивая загрузка**
   - NBT загружается только при первом использовании
   - Кэширование LoadedTemplate не требуется (быстрая загрузка)

3. **Батчинг блоков**
   - 1 блок за тик = 20 блоков/секунду
   - Можно увеличить до 2-3 блоков/тик

4. **Коллаборация**
   - N ботов = N × скорость
   - Автоматическое распределение работы

### Метрики

```
Структура: big-house
Размер: 15×8×12 = 1440 блоков объем
Блоков в структуре: ~800-1000
Размер NBT: ~3 KB

1 бот:
- Скорость: 20 блоков/сек
- Время: ~50 секунд

3 бота:
- Скорость: 60 блоков/сек
- Время: ~17 секунд

Память:
- NBT в памяти: ~10 KB
- LoadedTemplate: ~50 KB
- CollaborativeBuild: ~100 KB
Итого: ~160 KB на структуру
```

## Расширяемость

### Добавление новой структуры

```
1. Создать NBT файл:
   - Вручную (Structure Block)
   - Программно (Generator)
   - Конвертировать (Schematic)

2. Поместить в structures/
   structures/my-structure.nbt

3. Обновить PromptBuilder:
   - my-structure: Description

4. Добавить пример:
   "build my structure" → {"structure": "my-structure"}

5. Использовать:
   @Alex build my structure
```

### Создание генератора

```java
public class MyStructureGenerator {
    public static void generateMyStructure() {
        List<BlockEntry> blocks = new ArrayList<>();
        
        // Добавить блоки
        blocks.add(new BlockEntry("minecraft:stone", 0, 0, 0));
        blocks.add(new BlockEntry("minecraft:stone", 1, 0, 0));
        // ...
        
        // Сохранить в NBT
        saveToNBT(blocks, width, height, depth, 
                 "structures/my-structure.nbt");
    }
}
```

## Отладка

### Логирование

```java
// Генерация
CraftoMod.LOGGER.info("Generated big house NBT: {} blocks", count);

// Загрузка
CraftoMod.LOGGER.info("Loaded '{}' from NBT template with {} blocks", 
                      name, blocks.size());

// Строительство
CraftoMod.LOGGER.info("Crafto '{}' PLACED BLOCK at {} - Total: {}/{}", 
                      name, pos, placed, total);

// Прогресс
CraftoMod.LOGGER.info("{} build progress: {}/{} ({}%)", 
                      type, placed, total, percent);
```

### Проверки

```bash
# NBT файл существует
ls -la structures/big-house.nbt

# Логи генерации
grep "Generated big house" logs/latest.log

# Логи загрузки
grep "Loaded.*from NBT" logs/latest.log

# Логи строительства
grep "PLACED BLOCK" logs/latest.log

# Прогресс
grep "build progress" logs/latest.log
```

---

**Архитектура готова к расширению и масштабированию!** 🚀
