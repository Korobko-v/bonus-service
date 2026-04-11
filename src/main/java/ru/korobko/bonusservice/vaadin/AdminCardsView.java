package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.service.BonusCardService;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "admin/cards", layout = MainLayout.class)
@PageTitle("Управление картами")
@RolesAllowed("ADMIN")
public class AdminCardsView extends VerticalLayout {

    private final BonusCardService bonusCardService;
    private final Grid<BonusCardDto> grid = new Grid<>(BonusCardDto.class);
    private final TextField filter = new TextField("Фильтр по номеру");

    @Autowired
    public AdminCardsView(BonusCardService bonusCardService) {
        this.bonusCardService = bonusCardService;

        setSizeFull();

        configureFilter();
        configureGrid();

        Button addButton = new Button("Создать карту", e -> showCreateDialog());

        HorizontalLayout toolbar = new HorizontalLayout(filter, addButton);
        toolbar.setWidthFull();

        add(toolbar, grid);
        refreshGrid();
    }

    private void configureFilter() {
        filter.setPlaceholder("Поиск по номеру карты...");
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.addValueChangeListener(e -> refreshGrid());
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(BonusCardDto::getCardNumber).setHeader("Номер карты").setSortable(true);
        grid.addColumn(BonusCardDto::getClientName).setHeader("Клиент").setSortable(true);
        grid.addColumn(BonusCardDto::getBalance).setHeader("Баланс").setSortable(true);

        grid.addColumn(new ComponentRenderer<>(card -> {
            Span status = new Span(card.isActive() ? "Активна" : "Деактивирована");
            status.getStyle().set("color", card.isActive() ? "green" : "red");
            return status;
        })).setHeader("Статус");

        grid.addComponentColumn(card -> {
            Button actionBtn;
            if (card.isActive()) {
                actionBtn = new Button("Деактивировать");
                actionBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                actionBtn.addClickListener(e -> {
                    bonusCardService.deactivateCard(card.getId());
                    Notification.show("Карта " + card.getCardNumber() + " деактивирована");
                    refreshGrid();
                });
            } else {
                actionBtn = new Button("Активировать");
                actionBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
                actionBtn.addClickListener(e -> {
                    bonusCardService.activateCard(card.getId());
                    Notification.show("Карта " + card.getCardNumber() + " активирована");
                    refreshGrid();
                });
            }
            return actionBtn;
        }).setHeader("Действия");
    }

    private void showCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Создание новой карты");

        TextField cardNumber = new TextField("Номер карты");
        cardNumber.setRequired(true);

        TextField clientId = new TextField("ID клиента");
        clientId.setRequired(true);

        TextField clientName = new TextField("Имя клиента");

        BigDecimalField initialBalance = new BigDecimalField("Начальный баланс");
        initialBalance.setValue(BigDecimal.ZERO);

        Button saveBtn = new Button("Создать", e -> {
            CreateCardRequest request = new CreateCardRequest();
            request.setCardNumber(cardNumber.getValue());
            request.setClientId(Long.parseLong(clientId.getValue()));
            request.setClientName(clientName.getValue());
            request.setInitialBalance(initialBalance.getValue().doubleValue());

            bonusCardService.createCard(request);
            Notification.show("Карта создана");
            dialog.close();
            refreshGrid();
        });

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        dialog.add(cardNumber, clientId, clientName, initialBalance,
                new HorizontalLayout(saveBtn, cancelBtn));
        dialog.open();
    }

    private void refreshGrid() {
        List<BonusCardDto> cards = bonusCardService.getAllCards();

        if (filter.getValue() != null && !filter.getValue().isEmpty()) {
            cards = cards.stream()
                    .filter(c -> c.getCardNumber().contains(filter.getValue()))
                    .toList();
        }

        grid.setItems(cards);
    }
}