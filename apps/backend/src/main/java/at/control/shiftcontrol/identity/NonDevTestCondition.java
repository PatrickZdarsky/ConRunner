package at.control.shiftcontrol.identity;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;
@Deprecated
public class NonDevTestCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        // Check if EITHER profile is active
        boolean isDevActive = env.acceptsProfiles(Profiles.of("development"));
        boolean isTestActive = env.acceptsProfiles(Profiles.of("test"));

        return !(isDevActive || isTestActive);
    }
}

