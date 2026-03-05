package com.creatorhub.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
@ConditionalOnProperty(
        name = "seed.enabled",
        havingValue = "true")
@RequiredArgsConstructor
@Order(0)
public class MemberCreatorSeedRunner implements CommandLineRunner {

    private final MemberCreatorSeeder memberCreatorSeeder;

    @Override
    public void run(String... args) {
        memberCreatorSeeder.seedMembersAndCreators(100);
    }
}
