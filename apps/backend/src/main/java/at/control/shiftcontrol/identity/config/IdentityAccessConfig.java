package at.control.shiftcontrol.identity.config;

import at.control.shiftcontrol.identity.auth.MachineClientAuthorizer;
import at.control.shiftcontrol.identity.config.props.MachineClientRegistration;
import at.control.shiftcontrol.identity.config.props.PlatformAdminBootstrap;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration
public class IdentityAccessConfig {
    @Bean
    PlatformAdminBootstrap platformAdminBootstrap(Environment environment) {
        List<PlatformAdminBootstrap.ExternalIdentity> configuredAdministrators = Binder.get(environment)
            .bind("identity.bootstrap-platform-admins", Bindable.listOf(PlatformAdminBootstrap.ExternalIdentity.class))
            .orElse(List.of());
        return new PlatformAdminBootstrap(configuredAdministrators);
    }

    @Bean
    MachineClientAuthorizer machineClientAuthorizer(Environment environment) {
        List<MachineClientRegistration> machineClients = Binder.get(environment)
            .bind("identity.machine-clients", Bindable.listOf(MachineClientRegistration.class))
            .orElse(List.of());
        return new MachineClientAuthorizer(machineClients);
    }
}
