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
        name = "seed.enabled",
        havingValue = "true" )
@RequiredArgsConstructor
@Order(1)
public class CreationSeedRunner implements CommandLineRunner {

    private final CreationSeeder creationSeeder;

    @Override
    public void run(String... args) {
        creationSeeder.seedCreations(7000, 1000);
    }
}
