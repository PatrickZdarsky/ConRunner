package at.conrunner;

import org.springframework.boot.SpringApplication;

public class TestShiftcontrolApplication {

	public static void main(String[] args) {
		SpringApplication.from(ConRunnerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
