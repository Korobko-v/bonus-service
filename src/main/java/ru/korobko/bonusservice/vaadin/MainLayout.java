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

        // Клик по логотипу — возврат на главную
        logo.getStyle().set("cursor", "pointer");
        logo.addClickListener(e -> UI.getCurrent().navigate(""));

        String username = getCurrentUsername();
        Span userSpan = new Span("Пользователь: " + (username != null ? username : "Гость"));
        userSpan.addClassNames(LumoUtility.Margin.MEDIUM);

        Button logoutButton = new Button("Выйти", new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.addClickListener(e -> {
            // Очищаем контекст безопасности
            SecurityContextHolder.clearContext();
            // Очищаем localStorage и перенаправляем на главную
            UI.getCurrent().getPage().executeJs(
                    "localStorage.removeItem('token'); window.location.href=''"
            );
        });

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

    private void createDrawer() {
        // Получаем аутентификацию из SecurityContext
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