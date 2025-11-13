package com.crafto.ai.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CraftoConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> OLLAMA_BASE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> OLLAMA_MODEL;
    public static final ForgeConfigSpec.IntValue OLLAMA_MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue OLLAMA_TEMPERATURE;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> OPENAI_MODEL;
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;
    public static final ForgeConfigSpec.IntValue ACTION_TICK_DELAY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHAT_RESPONSES;
    public static final ForgeConfigSpec.IntValue MAX_ACTIVE_CRAFTOS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI API Configuration").push("ai");
        
        AI_PROVIDER = builder
            .comment("AI provider to use: 'groq' (FASTEST, FREE), 'openai', 'gemini', or 'local' (Ollama)")
            .define("provider", "local");

        builder.pop();

        builder.comment("Ollama Local LLM Configuration").push("ollama");

        OLLAMA_BASE_URL = builder
            .comment("Ollama API base URL")
            .define("baseUrl", "http://localhost:11434");

        OLLAMA_MODEL = builder
            .comment("Ollama model to use - optimized for M2: 'qwen2.5:7b' (fastest), 'mistral:7b', 'llama3.2:3b' (smallest)")
            .define("model", "qwen2.5:7b");

        OLLAMA_MAX_TOKENS = builder
            .comment("Maximum tokens for Ollama response (lower = faster on M2)")
            .defineInRange("maxTokens", 400, 100, 4096);

        OLLAMA_TEMPERATURE = builder
            .comment("Temperature for Ollama responses (0.0-2.0)")
            .defineInRange("temperature", 0.7, 0.0, 2.0);

        builder.pop();

        builder.comment("OpenAI/Gemini API Configuration (same key field used for both)").push("openai");
        
        OPENAI_API_KEY = builder
            .comment("Your OpenAI API key (required)")
            .define("apiKey", "");
        
        OPENAI_MODEL = builder
            .comment("OpenAI model to use (gpt-4, gpt-4-turbo-preview, gpt-3.5-turbo)")
            .define("model", "gpt-4-turbo-preview");
        
        MAX_TOKENS = builder
            .comment("Maximum tokens per API request")
            .defineInRange("maxTokens", 8000, 100, 65536);
        
        TEMPERATURE = builder
            .comment("Temperature for AI responses (0.0-2.0, lower is more deterministic)")
            .defineInRange("temperature", 0.7, 0.0, 2.0);
        
        builder.pop();

        builder.comment("Crafto Behavior Configuration").push("behavior");
        
        ACTION_TICK_DELAY = builder
            .comment("Ticks between action checks (20 ticks = 1 second, higher = better performance)")
            .defineInRange("actionTickDelay", 10, 1, 100);
        
        ENABLE_CHAT_RESPONSES = builder
            .comment("Allow Craftos to respond in chat")
            .define("enableChatResponses", true);
        
        MAX_ACTIVE_CRAFTOS = builder
            .comment("Maximum number of Craftos that can be active simultaneously (lower = better performance)")
            .defineInRange("maxActiveCraftos", 5, 1, 50);
        
        builder.pop();

        SPEC = builder.build();
    }
}
