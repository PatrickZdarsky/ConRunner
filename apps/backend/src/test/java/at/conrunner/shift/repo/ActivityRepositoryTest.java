package at.conrunner.shift.repo;

import at.conrunner.TestcontainersConfiguration;
import at.conrunner.shift.entity.Activity;
import at.conrunner.shift.util.TestEntityFactory;
import at.conrunner.shift.config.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({TestConfig.class, TestcontainersConfiguration.class})
public class ActivityRepositoryTest {
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private TestEntityFactory testEntityFactory;

    @Test
    void testSaveActivity() {
        Activity factoryPersistedActivity = testEntityFactory.createPersistedActivity();
        Assertions.assertNotNull(factoryPersistedActivity);
    }
}
