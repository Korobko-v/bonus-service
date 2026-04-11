package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("")
@PageTitle("Бонусная система - Главное меню")
@PermitAll
public class MainMenuView extends VerticalLayout {

    public MainMenuView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"));

        H1 title = new H1("Бонусная система");
        title.getStyle().set("margin-bottom", "1em");

        if (!isAuthenticated) {
            Paragraph welcome = new Paragraph("Добро пожаловать! Пожалуйста, войдите или зарегистрируйтесь.");
            Button loginBtn = new Button("Войти", e -> UI.getCurrent().navigate("login"));
            Button registerBtn = new Button("Регистрация", e -> UI.getCurrent().navigate("register"));
            loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            HorizontalLayout buttons = new HorizontalLayout(loginBtn, registerBtn);
            buttons.setJustifyContentMode(JustifyContentMode.CENTER);

            add(title, welcome, buttons);
        } else {
            String username = getCurrentUsername();
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            Paragraph welcome = new Paragraph("Добро пожаловать, " + username + "!");
            welcome.getStyle().set("margin-bottom", "1em");

            Button logoutBtn = new Button("Выйти");
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            logoutBtn.addClickListener(e -> {
                SecurityContextHolder.clearContext();
                UI.getCurrent().getPage().executeJs(
                        "localStorage.removeItem('token'); window.location.href=''"
                );
            });

            Button myCardsBtn = new Button("Мои карты", e -> UI.getCurrent().navigate("my-cards"));
            Button myTransactionsBtn = new Button("История операций", e -> UI.getCurrent().navigate("my-transactions"));
            if (isAdmin) {
                Button cardsBtn = new Button("Управление картами", e -> UI.getCurrent().navigate("admin/cards"));
                Button usersBtn = new Button("Пользователи", e -> UI.getCurrent().navigate("admin/users"));

                cardsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                usersBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

                HorizontalLayout adminButtons = new HorizontalLayout(cardsBtn, usersBtn);
                adminButtons.setJustifyContentMode(JustifyContentMode.CENTER);
                adminButtons.getStyle().set("margin-top", "1em");

                add(title, welcome, adminButtons, logoutBtn);
            } else {

                myCardsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                myTransactionsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

                HorizontalLayout userButtons = new HorizontalLayout(myCardsBtn, myTransactionsBtn);
                userButtons.setJustifyContentMode(JustifyContentMode.CENTER);
                userButtons.getStyle().set("margin-top", "1em");

                add(title, welcome, userButtons, logoutBtn);
            }
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }
}