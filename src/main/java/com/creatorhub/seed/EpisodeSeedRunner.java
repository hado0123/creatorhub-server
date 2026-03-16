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
        name = "seed.enabled.episode",
        havingValue = "true" )
@RequiredArgsConstructor
@Order(2)
public class EpisodeSeedRunner implements CommandLineRunner {

    private final EpisodeSeeder episodeSeeder;

    @Override
    public void run(String... args) {
        episodeSeeder.seedEpisodes();
    }
}
