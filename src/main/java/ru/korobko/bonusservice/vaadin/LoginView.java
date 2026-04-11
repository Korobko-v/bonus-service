package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import ru.korobko.bonusservice.security.JwtUtil;

@Route("login")
@PageTitle("Вход в систему")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public LoginView(AuthenticationManager authenticationManager,
                     JwtUtil jwtUtil,
                     UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Вход в бонусную систему");
        title.getStyle().set("margin-bottom", "2em");

        TextField usernameField = new TextField("Логин");
        usernameField.setWidth("300px");
        usernameField.setRequired(true);

        PasswordField passwordField = new PasswordField("Пароль");
        passwordField.setWidth("300px");
        passwordField.setRequired(true);

        Button loginButton = new Button("Войти");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidth("300px");
        loginButton.addClickListener(e -> {
            if (usernameField.isEmpty() || passwordField.isEmpty()) {
                Notification.show("Заполните все поля");
                return;
            }

            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                usernameField.getValue(),
                                passwordField.getValue()
                        )
                );

                // Сохраняем аутентификацию в SecurityContext
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);

                // Сохраняем в HttpSession (для Vaadin)
                HttpServletRequest httpRequest = VaadinServletRequest.getCurrent().getHttpServletRequest();
                httpRequest.getSession().setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        securityContext
                );

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String jwt = jwtUtil.generateToken(userDetails);

                // Сохраняем токен в localStorage
                UI.getCurrent().getPage().executeJs("localStorage.setItem('token', $0)", jwt);

                Notification.show("Добро пожаловать, " + userDetails.getUsername() + "!");

                // Редирект на главную
                UI.getCurrent().navigate("");

            } catch (Exception ex) {
                Notification.show("Ошибка входа: неверный логин или пароль");
            }
        });

        RouterLink registerLink = new RouterLink("Нет аккаунта? Зарегистрироваться", RegisterView.class);
        registerLink.getStyle().set("margin-top", "1em");

        FormLayout formLayout = new FormLayout();
        formLayout.add(usernameField, passwordField, loginButton);
        formLayout.setWidth("300px");

        add(title, formLayout, registerLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() &&
                !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            event.forwardTo("");
        }
    }
}