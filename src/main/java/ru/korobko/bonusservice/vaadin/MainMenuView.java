package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

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

            Button logoutButton = new Button("Выйти", new Icon(VaadinIcon.SIGN_OUT));
            logoutButton.setVisible(isAuthenticated);
            logoutButton.addClickListener(e -> logout());

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

                add(title, welcome, adminButtons, logoutButton);
            } else {

                myCardsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                myTransactionsBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

                HorizontalLayout userButtons = new HorizontalLayout(myCardsBtn, myTransactionsBtn);
                userButtons.setJustifyContentMode(JustifyContentMode.CENTER);
                userButtons.getStyle().set("margin-top", "1em");

                add(title, welcome, userButtons, logoutButton);
            }
        }
    }

    private void logout() {
        try {
            HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
            HttpServletResponse response = VaadinServletResponse.getCurrent().getHttpServletResponse();

            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());

            SecurityContextHolder.clearContext();

            UI.getCurrent().getPage().executeJs(
                    "localStorage.removeItem('token'); window.location.href=''"
            );
        } catch (Exception e) {
            UI.getCurrent().getPage().executeJs("window.location.href=''");
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