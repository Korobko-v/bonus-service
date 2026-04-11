package ru.korobko.bonusservice.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.korobko.bonusservice.model.User;
import ru.korobko.bonusservice.repository.UserRepository;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("Пользователи")
@RolesAllowed("ADMIN")
public class AdminUsersView extends VerticalLayout {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Grid<User> grid = new Grid<>(User.class);

    @Autowired
    public AdminUsersView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();

        configureGrid();

        Button addButton = new Button("Создать пользователя", e -> showCreateDialog());

        add(addButton, grid);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(User::getUsername).setHeader("Логин");
        grid.addColumn(User::getRole).setHeader("Роль");

        grid.addComponentColumn(user -> {
            Button deleteBtn = new Button("Удалить");
            deleteBtn.addClickListener(e -> {
                userRepository.delete(user);
                refreshGrid();
            });
            return deleteBtn;
        }).setHeader("Действия");
    }

    private void showCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Создание пользователя");

        TextField username = new TextField("Логин");
        username.setRequired(true);

        PasswordField password = new PasswordField("Пароль");
        password.setRequired(true);

        TextField role = new TextField("Роль (USER/ADMIN)");
        role.setValue("USER");

        Button saveBtn = new Button("Создать", e -> {
            User user = User.builder()
                    .username(username.getValue())
                    .password(passwordEncoder.encode(password.getValue()))
                    .role(role.getValue())
                    .build();
            userRepository.save(user);
            dialog.close();
            refreshGrid();
        });

        dialog.add(username, password, role, saveBtn);
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(userRepository.findAll());
    }
}