package ru.korobko.bonusservice.vaadin;

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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.korobko.bonusservice.model.User;
import ru.korobko.bonusservice.repository.UserRepository;

@Route("register")
@PageTitle("Регистрация")
@AnonymousAllowed
public class RegisterView extends VerticalLayout implements BeforeEnterObserver {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegisterView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Регистрация нового пользователя");
        title.getStyle().set("margin-bottom", "2em");

        TextField usernameField = new TextField("Логин");
        usernameField.setWidth("300px");
        usernameField.setRequired(true);

        PasswordField passwordField = new PasswordField("Пароль");
        passwordField.setWidth("300px");
        passwordField.setRequired(true);

        Button registerButton = new Button("Зарегистрироваться");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidth("300px");
        registerButton.addClickListener(e -> {
            if (usernameField.isEmpty() || passwordField.isEmpty()) {
                Notification.show("Заполните все поля");
                return;
            }

            if (userRepository.existsByUsername(usernameField.getValue())) {
                Notification.show("Пользователь с таким логином уже существует");
                return;
            }

            User user = User.builder()
                    .username(usernameField.getValue())
                    .password(passwordEncoder.encode(passwordField.getValue()))
                    .role("USER")
                    .build();

            userRepository.save(user);
            Notification.show("Регистрация успешна! Теперь вы можете войти.");
            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        RouterLink loginLink = new RouterLink("Уже есть аккаунт? Войти", LoginView.class);
        loginLink.getStyle().set("margin-top", "1em");

        RouterLink mainMenuLink = new RouterLink("Вернуться в главное меню", MainMenuView.class);
        mainMenuLink.getStyle().set("margin-top", "0.5em");

        FormLayout formLayout = new FormLayout();
        formLayout.add(usernameField, passwordField, registerButton);
        formLayout.setWidth("300px");

        add(title, formLayout, loginLink, mainMenuLink);
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