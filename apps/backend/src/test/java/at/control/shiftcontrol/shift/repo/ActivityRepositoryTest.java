package at.control.shiftcontrol.shift.repo;

import at.control.shiftcontrol.shift.entity.Activity;
import at.control.shiftcontrol.shift.util.TestEntityFactory;
import at.control.shiftcontrol.shift.config.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

@DataJpaTest(properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({TestConfig.class, at.control.shiftcontrol.TestcontainersConfiguration.class})
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
