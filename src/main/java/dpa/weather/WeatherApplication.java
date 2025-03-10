package dpa.weather;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
//import org.h2.tools.Server;

import java.sql.SQLException;

@SpringBootApplication
public class WeatherApplication {

    public static void main(String[] args) {
        System.setProperty("vaadin.productionMode", "true");
        SpringApplication.run(WeatherApplication.class, args);
    }
}