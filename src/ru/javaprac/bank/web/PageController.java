package ru.javaprac.bank.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @GetMapping("/")
    public String dashboard(Model model) {
        return page(model, "dashboard", null, null);
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        return page(model, "reports", null, null);
    }

    @GetMapping("/departments")
    public String departments(Model model) {
        return page(model, "departments", null, null);
    }

    @GetMapping("/departments/new")
    public String newDepartment(Model model) {
        return page(model, "department-form", null, null);
    }

    @GetMapping("/departments/{id}")
    public String department(@PathVariable Long id, Model model) {
        return page(model, "department-detail", id, null);
    }

    @GetMapping("/departments/{id}/edit")
    public String editDepartment(@PathVariable Long id, Model model) {
        return page(model, "department-form", id, null);
    }

    @GetMapping("/clients")
    public String clients(Model model) {
        return page(model, "clients", null, null);
    }

    @GetMapping("/clients/new")
    public String newClient(Model model) {
        return page(model, "client-form", null, null);
    }

    @GetMapping("/clients/{id}")
    public String client(@PathVariable Long id, Model model) {
        return page(model, "client-detail", id, null);
    }

    @GetMapping("/clients/{id}/edit")
    public String editClient(@PathVariable Long id, Model model) {
        return page(model, "client-form", id, null);
    }

    @GetMapping("/accounts")
    public String accounts(Model model) {
        return page(model, "accounts", null, null);
    }

    @GetMapping("/accounts/new")
    public String newAccount(@RequestParam(required = false) Long clientId, Model model) {
        model.addAttribute("clientId", clientId == null ? "" : clientId);
        return page(model, "account-form", null, null);
    }

    @GetMapping("/accounts/{id}")
    public String account(@PathVariable Long id, Model model) {
        return page(model, "account-detail", id, null);
    }

    @GetMapping("/accounts/{id}/operation")
    public String operation(@PathVariable Long id,
                            @RequestParam(defaultValue = "credit") String kind,
                            Model model) {
        return page(model, "operation-form", id, kind);
    }

    private String page(Model model, String page, Long id, String kind) {
        model.addAttribute("page", page);
        model.addAttribute("entityId", id == null ? "" : id);
        model.addAttribute("kind", kind == null ? "" : kind);
        if (!model.containsAttribute("clientId")) {
            model.addAttribute("clientId", "");
        }
        return page;
    }
}
