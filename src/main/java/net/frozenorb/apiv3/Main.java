package net.frozenorb.apiv3;

import net.frozenorb.apiv3.util.SpringUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main implements ApplicationContextAware {

	public static void main(String[] args) {
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		SpringApplication.run(Main.class, args);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		SpringUtils.setBeanFactory(applicationContext);
	}

}