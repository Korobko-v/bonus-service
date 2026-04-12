package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.dto.request.RefundRequest;
import ru.korobko.bonusservice.model.BonusTransaction;
import ru.korobko.bonusservice.service.BonusService;

import java.util.List;

@Route(value = "card-transactions/:cardNumber", layout = MainLayout.class)
@PageTitle("История транзакций карты")
@PermitAll
public class TransactionHistoryForCardView extends VerticalLayout implements BeforeEnterObserver {

    private final BonusService bonusService;
    private final Grid<BonusTransactionDto> grid = new Grid<>(BonusTransactionDto.class);
    private String cardNumber;
    private boolean isAdmin;

    @Autowired
    public TransactionHistoryForCardView(BonusService bonusService) {
        this.bonusService = bonusService;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> {
                    String role = grantedAuthority.getAuthority();
                    return role.equals("ROLE_ADMIN") || role.equals("ADMIN");
                });

        setSizeFull();

        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(BonusTransactionDto::getTransactionId).setHeader("ID транзакции");
        grid.addColumn(BonusTransactionDto::getType).setHeader("Тип");
        grid.addColumn(BonusTransactionDto::getAmount).setHeader("Сумма");
        grid.addColumn(BonusTransactionDto::getStatus).setHeader("Статус");
        grid.addColumn(BonusTransactionDto::getDescription).setHeader("Описание");
        grid.addColumn(BonusTransactionDto::getCreatedAt).setHeader("Дата");

        if (isAdmin) {
            grid.addColumn(new ComponentRenderer<>(transaction -> {
                if (transaction.getStatus() == BonusTransaction.TransactionStatus.REFUND) {
                    return null;
                }

                Button refundBtn = new Button("Возврат");
                refundBtn.addThemeVariants(ButtonVariant.LUMO_WARNING, ButtonVariant.LUMO_TERTIARY);
                refundBtn.addClickListener(e -> showRefundDialog(transaction));
                return refundBtn;
            })).setHeader("Действия");
        }
    }

    private void showRefundDialog(BonusTransactionDto transaction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Возврат транзакции");

        TextField infoField = new TextField("Информация");
        infoField.setValue("Транзакция: " + transaction.getTransactionId() +
                ", Сумма: " + transaction.getAmount() +
                ", Тип: " + transaction.getType());
        infoField.setReadOnly(true);
        infoField.setWidthFull();

        TextField orderIdField = new TextField("ID заказа для возврата");
        orderIdField.setRequired(true);
        orderIdField.setWidthFull();

        Button submitBtn = new Button("Подтвердить возврат", e -> {
            try {
                RefundRequest request = new RefundRequest();
                request.setCardNumber(cardNumber);
                request.setOriginalTransactionId(transaction.getTransactionId());
                request.setOrderId(orderIdField.getValue());
                request.setDescription("Возврат через админ-панель");

                bonusService.refundBonus(request);
                Notification.show("Возврат выполнен успешно");
                dialog.close();
                refreshGrid(); // обновляем таблицу
            } catch (Exception ex) {
                Notification.show("Ошибка возврата: " + ex.getMessage());
            }
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Отмена", e -> dialog.close());

        dialog.add(infoField, orderIdField, new HorizontalLayout(submitBtn, cancelBtn));
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        cardNumber = event.getRouteParameters().get("cardNumber").orElse(null);

        if (cardNumber != null) {
            H2 title = new H2("История транзакций карты: " + cardNumber);
            addComponentAsFirst(title);
            refreshGrid();
        }
    }

    private void refreshGrid() {
        try {
            List<BonusTransactionDto> transactions = bonusService.getTransactionHistoryForAdmin(cardNumber);
            grid.setItems(transactions);
        } catch (Exception e) {
            Notification.show("Ошибка загрузки истории: " + e.getMessage());
        }
    }
}