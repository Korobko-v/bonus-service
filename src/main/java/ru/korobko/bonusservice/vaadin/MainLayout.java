package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Бонусная система");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.MEDIUM
        );

        logo.getStyle().set("cursor", "pointer");
        logo.addClickListener(e -> UI.getCurrent().navigate(""));

        String username = getCurrentUsername();
        boolean isAuthenticated = username != null && !username.equals("anonymousUser");

        Span userSpan = new Span("Пользователь: " + (isAuthenticated ? username : "Гость"));
        userSpan.addClassNames(LumoUtility.Margin.MEDIUM);

        Button logoutButton = new Button("Выйти", new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.setVisible(isAuthenticated);
        logoutButton.addClickListener(e -> logout());

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(),
                logo,
                userSpan,
                logoutButton
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(
                LumoUtility.Padding.Vertical.NONE,
                LumoUtility.Padding.Horizontal.MEDIUM
        );

        addToNavbar(header);
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

    private void createDrawer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"));

        if (!isAuthenticated) {
            return;
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> {
                    String role = grantedAuthority.getAuthority();
                    return role.equals("ROLE_ADMIN") || role.equals("ADMIN");
                });

        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Главное меню", MainMenuView.class, VaadinIcon.HOME.create()));

        if (isAdmin) {
            nav.addItem(new SideNavItem("Управление картами", AdminCardsView.class, VaadinIcon.CREDIT_CARD.create()));
            nav.addItem(new SideNavItem("Пользователи", AdminUsersView.class, VaadinIcon.USERS.create()));
        } else {
            nav.addItem(new SideNavItem("Мои карты", MyCardsView.class, VaadinIcon.CREDIT_CARD.create()));
            nav.addItem(new SideNavItem("История операций", TransactionHistoryView.class, VaadinIcon.ARCHIVE.create()));
        }

        addToDrawer(nav);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "Гость";

        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            String username = (String) principal;
            return username.equals("anonymousUser") ? "Гость" : username;
        }
        return "Гость";
    }
}