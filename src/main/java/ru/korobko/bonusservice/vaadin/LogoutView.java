package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("logout")
@PageTitle("Выход")
public class LogoutView extends VerticalLayout implements BeforeEnterObserver {

    public LogoutView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 message = new H1("Вы вышли из системы");
        add(message);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        SecurityContextHolder.clearContext();

        UI.getCurrent().getPage().executeJs(
                "localStorage.removeItem('token'); window.location.href='login'"
        );
    }
}