package ru.korobko.bonusservice.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import ru.korobko.bonusservice.vaadin.LoginView;

@Configuration
@EnableWebSecurity
public class VaadinSecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Настройка для Vaadin
        super.configure(http);

        // Устанавливаем страницу логина
        setLoginView(http, LoginView.class);
    }
}