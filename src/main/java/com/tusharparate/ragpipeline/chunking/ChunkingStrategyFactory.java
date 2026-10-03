package com.tusharparate.ragpipeline.chunking;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves a {@link ChunkingStrategyType} to its Spring-managed implementation.
 * Spring auto-collects every {@link ChunkingStrategy} bean, so registering a new strategy
 * is just "add a @Component class implementing the interface" - nothing here needs editing.
 */
@Component
public class ChunkingStrategyFactory {

    private final Map<ChunkingStrategyType, ChunkingStrategy> strategies;

    public ChunkingStrategyFactory(List<ChunkingStrategy> strategyBeans) {
        this.strategies = strategyBeans.stream()
                .collect(Collectors.toMap(ChunkingStrategy::getType, Function.identity()));
    }

    public ChunkingStrategy resolve(ChunkingStrategyType type) {
        ChunkingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No chunking strategy registered for type: " + type);
        }
        return strategy;
    }
}
