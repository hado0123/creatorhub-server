package com.creatorhub.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
@ConditionalOnProperty(
        name = "seed.enabled.creation-aggregate",
        havingValue = "true" )
@RequiredArgsConstructor
@Order(3)
public class CreationAggregateSeedRunner implements CommandLineRunner {

    private final CreationAggregateSeeder creationAggregateSeeder;

    @Override
    public void run(String... args) {
        creationAggregateSeeder.updateAggregates();
    }
}
