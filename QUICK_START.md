# 🚀 Быстрый старт Crafto AI

## ⚡ Установка за 5 минут

### 1. Установите Ollama
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows - скачайте с https://ollama.ai/download/windows
```

### 2. Установите модель
```bash
ollama serve
ollama pull qwen2.5:7b
```

### 3. Скомпилируйте мод
```bash
git clone https://github.com/iwizard7/minecraft_bot_Crafto.git
cd Crafto
./gradlew build
```

### 4. Установите в Minecraft
- Скопируйте `build/libs/crafto-ai-mod-1.0.0.jar` в папку `mods/`
- Запустите Minecraft с Forge 1.20.1

## 🎮 Первые команды

```
@Crafto hello                    # Приветствие
@Crafto build house             # Построить дом
@Crafto follow me               # Следовать за игроком
@Crafto kill 3 zombies         # Убить 3 зомби
@Crafto status                  # Проверить статус
```

## ⚙️ Минимальная конфигурация

Создайте `config/crafto-common.toml`:
```toml
[ai]
    ollama_url = "http://localhost:11434"
    model_name = "qwen2.5:7b"

[performance]
    max_active_craftos = 3
```

## 🔧 Проверка работы

1. Запустите Ollama: `ollama serve`
2. Проверьте API: `curl http://localhost:11434/api/tags`
3. В игре: `@Crafto hello`

**Готово!** 🎉