package at.conrunner.shift.config;

import at.conrunner.shift.service.rewardpoints.RewardPointsCalculator;
import at.conrunner.shift.service.rewardpoints.impl.RewardPointsCalculatorImpl;
import at.conrunner.shift.util.TestEntityFactory;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
public class TestConfig {
    @Bean
    public TestEntityFactory testEntityFactory() {
        return new TestEntityFactory();
    }
    @Bean
    public RewardPointsCalculator rewardPointsCalculator() {
        return new RewardPointsCalculatorImpl();
    }
}
