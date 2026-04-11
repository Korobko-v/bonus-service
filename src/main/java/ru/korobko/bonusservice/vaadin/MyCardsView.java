package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import ru.korobko.bonusservice.dto.BonusCardDto;
import ru.korobko.bonusservice.repository.UserRepository;
import ru.korobko.bonusservice.service.BonusCardService;

import java.util.List;

@Route(value = "my-cards", layout = MainLayout.class)
@PageTitle("Мои карты")
@RolesAllowed("USER")
public class MyCardsView extends VerticalLayout {

    private final BonusCardService bonusCardService;
    private final UserRepository userRepository;
    private final Grid<BonusCardDto> grid = new Grid<>(BonusCardDto.class);

    @Autowired
    public MyCardsView(BonusCardService bonusCardService, UserRepository userRepository) {
        this.bonusCardService = bonusCardService;
        this.userRepository = userRepository;

        setSizeFull();

        configureGrid();
        add(grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(BonusCardDto::getCardNumber).setHeader("Номер карты");
        grid.addColumn(BonusCardDto::getClientName).setHeader("Клиент");
        grid.addColumn(BonusCardDto::getBalance).setHeader("Баланс");
        grid.addColumn(card -> card.isActive() ? "Активна" : "Деактивирована").setHeader("Статус");
    }

    private void refreshGrid() {
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        List<BonusCardDto> cards = bonusCardService
                .getAllCardsByClientId(userRepository.findByUsername(username).get().getId());
        grid.setItems(cards);
    }
}