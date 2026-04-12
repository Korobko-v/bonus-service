package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.router.RouteParameters;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.dto.request.CreateCardRequest;
import ru.korobko.bonusservice.dto.request.TransactionRequest;
import ru.korobko.bonusservice.service.BonusCardService;
import ru.korobko.bonusservice.service.BonusService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/cards", layout = MainLayout.class)
@PageTitle("Управление картами")
@RolesAllowed("ADMIN")
public class AdminCardsView extends VerticalLayout {

    private final BonusCardService bonusCardService;
    private final BonusService bonusService;
    private final Grid<BonusCardDto> grid = new Grid<>(BonusCardDto.class);
    private final TextField filter = new TextField("Фильтр по номеру");

    @Autowired
    public AdminCardsView(BonusCardService bonusCardService, BonusService bonusService) {
        this.bonusCardService = bonusCardService;
        this.bonusService = bonusService;

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

        // Кнопка "История транзакций"
        grid.addComponentColumn(card -> {
            Button historyBtn = new Button("История");
            historyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            historyBtn.addClickListener(e -> {
                UI.getCurrent().navigate(TransactionHistoryForCardView.class,
                        new RouteParameters("cardNumber", card.getCardNumber()));
            });
            return historyBtn;
        }).setHeader("История");

        grid.addComponentColumn(card -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button accrueBtn = new Button("+");
            accrueBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            accrueBtn.addClickListener(e -> showTransactionDialog(card, "accrue"));

            Button writeOffBtn = new Button("-");
            writeOffBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            writeOffBtn.addClickListener(e -> showTransactionDialog(card, "writeoff"));

            actions.add(accrueBtn, writeOffBtn);
            return actions;
        }).setHeader("Операции");

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

    private void showTransactionDialog(BonusCardDto card, String type) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(type.equals("accrue") ? "Начисление бонусов" : "Списание бонусов");

        BigDecimalField amountField = new BigDecimalField("Сумма");
        amountField.setValue(BigDecimal.valueOf(100));
        amountField.setRequired(true);

        TextField descriptionField = new TextField("Описание");
        descriptionField.setPlaceholder("Необязательно");

        TextField orderIdField = new TextField("ID заказа");
        orderIdField.setValue(UUID.randomUUID().toString());
        orderIdField.setRequired(true);

        Button submitBtn = new Button("Выполнить", e -> {
            try {
                TransactionRequest request = new TransactionRequest();
                request.setCardNumber(card.getCardNumber());
                request.setAmount(amountField.getValue().doubleValue());
                request.setDescription(descriptionField.getValue());
                request.setOrderId(orderIdField.getValue());

                if (type.equals("accrue")) {
                    bonusService.accrueBonus(request);
                    Notification.show("Начислено " + amountField.getValue() + " бонусов");
                } else {
                    bonusService.writeOffBonus(request);
                    Notification.show("Списано " + amountField.getValue() + " бонусов");
                }
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show("Ошибка: " + ex.getMessage());
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        dialog.add(amountField, descriptionField, orderIdField, new HorizontalLayout(submitBtn, cancelBtn));
        dialog.open();
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