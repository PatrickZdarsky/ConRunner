package at.control.shiftcontrol;

import org.springframework.boot.SpringApplication;

public class TestShiftcontrolApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShiftcontrolApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
