package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import ru.korobko.bonusservice.dto.BonusTransactionDto;
import ru.korobko.bonusservice.service.BonusService;

import java.util.List;

@Route(value = "my-transactions", layout = MainLayout.class)
@PageTitle("История операций")
@RolesAllowed("USER")
public class TransactionHistoryView extends VerticalLayout {

    private final BonusService bonusService;
    private final Grid<BonusTransactionDto> grid = new Grid<>(BonusTransactionDto.class);

    @Autowired
    public TransactionHistoryView(BonusService bonusService) {
        this.bonusService = bonusService;

        setSizeFull();

        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(BonusTransactionDto::getTransactionId).setHeader("ID транзакции");
        grid.addColumn(BonusTransactionDto::getCardNumber).setHeader("Номер карты");
        grid.addColumn(BonusTransactionDto::getType).setHeader("Тип");
        grid.addColumn(BonusTransactionDto::getAmount).setHeader("Сумма");
        grid.addColumn(BonusTransactionDto::getDescription).setHeader("Описание");
        grid.addColumn(BonusTransactionDto::getCreatedAt).setHeader("Дата");
    }

    private void refreshGrid() {
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        List<BonusTransactionDto> transactions = bonusService.findAllByUsername(username);
        grid.setItems(transactions);
    }
}