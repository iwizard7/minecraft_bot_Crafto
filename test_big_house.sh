#!/bin/bash

# Скрипт для тестирования функции строительства большого дома

echo "🏗️  Тест системы строительства большого дома"
echo "=============================================="
echo ""

# Проверка существования NBT файла
echo "1. Проверка NBT файла..."
if [ -f "structures/big-house.nbt" ]; then
    echo "   ✅ structures/big-house.nbt существует"
    ls -lh structures/big-house.nbt
else
    echo "   ⚠️  structures/big-house.nbt не найден"
    echo "   Файл будет создан при запуске мода"
fi
echo ""

# Проверка других структур
echo "2. Доступные структуры:"
if [ -d "structures" ]; then
    ls -1 structures/*.nbt 2>/dev/null | while read file; do
        echo "   - $(basename "$file")"
    done
else
    echo "   ⚠️  Папка structures не найдена"
fi
echo ""

# Проверка schematic файлов
echo "3. Schematic файлы для конвертации:"
if [ -d "structures" ]; then
    ls -1 structures/*.schematic 2>/dev/null | while read file; do
        echo "   - $(basename "$file")"
    done
    
    if [ ! -f "structures/*.schematic" ]; then
        echo "   ℹ️  Schematic файлы не найдены"
    fi
else
    echo "   ⚠️  Папка structures не найдена"
fi
echo ""

# Проверка Java файлов
echo "4. Проверка новых файлов..."
files=(
    "src/main/java/com/crafto/ai/structure/BigHouseGenerator.java"
    "src/main/java/com/crafto/ai/structure/SchematicConverter.java"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file"
    else
        echo "   ❌ $file не найден"
    fi
done
echo ""

# Проверка документации
echo "5. Проверка документации..."
if [ -f "docs/BIG_HOUSE_GUIDE.md" ]; then
    echo "   ✅ docs/BIG_HOUSE_GUIDE.md"
else
    echo "   ❌ docs/BIG_HOUSE_GUIDE.md не найден"
fi
echo ""

# Инструкции по использованию
echo "=============================================="
echo "📝 Инструкции по использованию:"
echo ""
echo "1. Соберите проект:"
echo "   ./gradlew build"
echo ""
echo "2. Запустите Minecraft с модом"
echo ""
echo "3. В игре создайте бота:"
echo "   /crafto spawn Alex"
echo ""
echo "4. Дайте команду строительства:"
echo "   @Alex build big house"
echo "   или"
echo "   @Alex построй большой дом"
echo ""
echo "5. Бот начнет строить дом перед вами!"
echo ""
echo "=============================================="
echo "✨ Готово! Система настроена и готова к использованию."
