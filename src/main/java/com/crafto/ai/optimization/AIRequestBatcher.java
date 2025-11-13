package com.crafto.ai.optimization;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.crafto.ai.CraftoMod;
import com.crafto.ai.ai.OllamaClient;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AIRequestBatcher {
    private static final Gson GSON = new Gson();
    private static final int BATCH_SIZE = 5;
    private static final long BATCH_TIMEOUT_MS = 2000; // 2 секунды
    private static final int MAX_CONCURRENT_REQUESTS = 3;
    
    private final Queue<BatchRequest> pendingRequests = new ConcurrentLinkedQueue<>();
    private final Map<String, String> responseCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> activeRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Semaphore requestSemaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    
    private volatile ScheduledFuture<?> batchProcessor;
    
    public static class BatchRequest {
        public final String id;
        public final String agentName;
        public final String command;
        public final String context;
        public final CompletableFuture<String> future;
        public final long timestamp;
        
        public BatchRequest(String agentName, String command, String context) {
            this.id = "req_" + System.currentTimeMillis() + "_" + Math.random();
            this.agentName = agentName;
            this.command = command;
            this.context = context;
            this.future = new CompletableFuture<>();
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public CompletableFuture<String> submitRequest(String agentName, String command, String context) {
        // Проверяем кэш
        String cacheKey = generateCacheKey(command, context);
        String cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse != null) {
            CraftoMod.LOGGER.info("Using cached response for: " + command);
            return CompletableFuture.completedFuture(cachedResponse);
        }
        
        // Проверяем активные запросы
        CompletableFuture<String> activeRequest = activeRequests.get(cacheKey);
        if (activeRequest != null) {
            CraftoMod.LOGGER.info("Joining active request for: " + command);
            return activeRequest;
        }
        
        BatchRequest request = new BatchRequest(agentName, command, context);
        pendingRequests.offer(request);
        
        // Запускаем обработчик батчей если он не запущен
        startBatchProcessor();
        
        // Если батч заполнен, обрабатываем немедленно
        if (pendingRequests.size() >= BATCH_SIZE) {
            processBatch();
        }
        
        return request.future;
    }
    
    private synchronized void startBatchProcessor() {
        if (batchProcessor == null || batchProcessor.isDone()) {
            batchProcessor = scheduler.scheduleAtFixedRate(
                this::processBatch, 
                BATCH_TIMEOUT_MS, 
                BATCH_TIMEOUT_MS, 
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    private void processBatch() {
        if (pendingRequests.isEmpty()) {
            return;
        }
        
        List<BatchRequest> batch = new ArrayList<>();
        BatchRequest request;
        
        // Собираем батч
        while (batch.size() < BATCH_SIZE && (request = pendingRequests.poll()) != null) {
            batch.add(request);
        }
        
        if (batch.isEmpty()) {
            return;
        }
        
        CraftoMod.LOGGER.info("Processing batch of {} requests", batch.size());
        
        // Обрабатываем батч асинхронно
        CompletableFuture.runAsync(() -> processBatchAsync(batch), scheduler);
    }
    
    private void processBatchAsync(List<BatchRequest> batch) {
        try {
            requestSemaphore.acquire();
            
            if (batch.size() == 1) {
                // Одиночный запрос
                processSingleRequest(batch.get(0));
            } else {
                // Батчевый запрос
                processBatchedRequests(batch);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            batch.forEach(req -> req.future.completeExceptionally(e));
        } finally {
            requestSemaphore.release();
        }
    }
    
    private void processSingleRequest(BatchRequest request) {
        String cacheKey = generateCacheKey(request.command, request.context);
        activeRequests.put(cacheKey, request.future);
        
        try {
            String response = sendSingleRequestToAI(request);
            
            // Кэшируем ответ
            responseCache.put(cacheKey, response);
            
            request.future.complete(response);
            CraftoMod.LOGGER.info("Completed single request for agent: " + request.agentName);
            
        } catch (Exception e) {
            CraftoMod.LOGGER.error("Failed to process single request: " + e.getMessage());
            request.future.completeExceptionally(e);
        } finally {
            activeRequests.remove(cacheKey);
        }
    }
    
    private void processBatchedRequests(List<BatchRequest> batch) {
        try {
            Map<String, String> responses = sendBatchRequestToAI(batch);
            
            for (BatchRequest request : batch) {
                String response = responses.get(request.id);
                if (response != null) {
                    String cacheKey = generateCacheKey(request.command, request.context);
                    responseCache.put(cacheKey, response);
                    request.future.complete(response);
                } else {
                    request.future.completeExceptionally(
                        new RuntimeException("No response received for request: " + request.id));
                }
            }
            
            CraftoMod.LOGGER.info("Completed batch of {} requests", batch.size());
            
        } catch (Exception e) {
            CraftoMod.LOGGER.error("Failed to process batch: " + e.getMessage());
            batch.forEach(req -> req.future.completeExceptionally(e));
        }
    }
    
    private String sendSingleRequestToAI(BatchRequest request) throws Exception {
        // Используем OllamaClient напрямую для одиночных запросов
        OllamaClient ollamaClient = new OllamaClient();
        String systemPrompt = "You are a helpful AI assistant for Minecraft. Respond with JSON containing reasoning, plan, and tasks.";
        String userPrompt = buildPrompt(request.command, request.context);
        return ollamaClient.sendRequest(systemPrompt, userPrompt);
    }
    
    private Map<String, String> sendBatchRequestToAI(List<BatchRequest> batch) throws Exception {
        // Создаем батчевый промпт
        JsonObject batchPrompt = new JsonObject();
        batchPrompt.addProperty("model", "llama3.2");
        batchPrompt.addProperty("stream", false);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Process multiple agent commands in batch. Return JSON with responses for each request ID.\n\n");
        
        for (BatchRequest request : batch) {
            promptBuilder.append("Request ID: ").append(request.id).append("\n");
            promptBuilder.append("Agent: ").append(request.agentName).append("\n");
            promptBuilder.append("Command: ").append(request.command).append("\n");
            promptBuilder.append("Context: ").append(request.context).append("\n");
            promptBuilder.append("---\n");
        }
        
        promptBuilder.append("\nReturn format: {\"").append(batch.get(0).id).append("\": \"response1\", \"")
                     .append(batch.size() > 1 ? batch.get(1).id : "example").append("\": \"response2\", ...}");
        
        batchPrompt.addProperty("prompt", promptBuilder.toString());
        
        String response = sendHttpRequest(GSON.toJson(batchPrompt));
        
        // Парсим батчевый ответ
        return parseBatchResponse(response, batch);
    }
    
    private Map<String, String> parseBatchResponse(String response, List<BatchRequest> batch) {
        Map<String, String> responses = new HashMap<>();
        
        try {
            JsonObject jsonResponse = GSON.fromJson(response, JsonObject.class);
            
            for (BatchRequest request : batch) {
                if (jsonResponse.has(request.id)) {
                    responses.put(request.id, jsonResponse.get(request.id).getAsString());
                } else {
                    // Fallback: используем весь ответ для первого запроса
                    if (responses.isEmpty()) {
                        responses.put(request.id, response);
                    }
                }
            }
            
        } catch (Exception e) {
            CraftoMod.LOGGER.warn("Failed to parse batch response, using fallback");
            // Fallback: отдаем весь ответ первому запросу
            if (!batch.isEmpty()) {
                responses.put(batch.get(0).id, response);
            }
        }
        
        return responses;
    }
    
    private String buildPrompt(String command, String context) {
        return "Agent command: " + command + "\nContext: " + context + 
               "\nProvide a JSON response with reasoning, plan, and tasks.";
    }
    
    private String sendHttpRequest(String payload) throws Exception {
        // Используем OllamaClient для отправки запроса
        OllamaClient ollamaClient = new OllamaClient();
        return ollamaClient.sendRequest("You are a helpful AI assistant for Minecraft.", payload);
    }
    
    private String generateCacheKey(String command, String context) {
        return (command + "_" + context).hashCode() + "";
    }
    
    // Предзагрузка популярных команд
    public void preloadPopularCommands() {
        String[] popularCommands = {
            "follow me", "kill 10 mobs", "kill 100 mobs", "stop", "come here"
        };
        
        for (String command : popularCommands) {
            String cacheKey = generateCacheKey(command, "");
            if (!responseCache.containsKey(cacheKey)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        String response = sendSingleRequestToAI(
                            new BatchRequest("preload", command, ""));
                        responseCache.put(cacheKey, response);
                        CraftoMod.LOGGER.info("Preloaded command: " + command);
                    } catch (Exception e) {
                        CraftoMod.LOGGER.warn("Failed to preload command: " + command);
                    }
                });
            }
        }
    }
    
    // Очистка кэша
    public void clearCache() {
        responseCache.clear();
        CraftoMod.LOGGER.info("AI request cache cleared");
    }
    
    // Статистика
    public void logStatistics() {
        CraftoMod.LOGGER.info("AI Request Batcher Statistics:");
        CraftoMod.LOGGER.info("- Cached responses: " + responseCache.size());
        CraftoMod.LOGGER.info("- Pending requests: " + pendingRequests.size());
        CraftoMod.LOGGER.info("- Active requests: " + activeRequests.size());
        CraftoMod.LOGGER.info("- Total requests processed: " + requestCounter.get());
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}