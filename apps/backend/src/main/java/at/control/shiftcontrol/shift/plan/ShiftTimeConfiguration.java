package at.control.shiftcontrol.shift.plan;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ShiftTimeConfiguration {
    @Bean
    Clock shiftClock() {
        return Clock.systemUTC();
    }
}
