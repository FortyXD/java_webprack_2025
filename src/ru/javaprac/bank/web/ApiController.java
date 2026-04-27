package ru.javaprac.bank.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping(value = "/api", produces = "application/json; charset=UTF-8")
public class ApiController {

    private final BankWebService service = new BankWebService();

    @GetMapping("/dashboard")
    public String dashboard() {
        return json(service::dashboard);
    }

    @GetMapping("/reference")
    public String reference() {
        return json(service::referenceData);
    }

    @GetMapping("/departments")
    public String departments(@RequestParam(required = false) String search) {
        return json(() -> service.departments(search));
    }

    @GetMapping("/departments/{id}")
    public String department(@PathVariable Long id) {
        return json(() -> service.department(id));
    }

    @PostMapping("/departments/save")
    public String saveDepartment(@RequestParam Map<String, String> params) {
        return json(() -> service.saveDepartment(params));
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id) {
        return json(() -> service.deleteDepartment(id));
    }

    @GetMapping("/clients")
    public String clients(@RequestParam(required = false) Long departmentId,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) String search) {
        return json(() -> service.clients(departmentId, type, search));
    }

    @GetMapping("/clients/{id}")
    public String client(@PathVariable Long id) {
        return json(() -> service.client(id));
    }

    @PostMapping("/clients/save")
    public String saveClient(@RequestParam Map<String, String> params) {
        return json(() -> service.saveClient(params));
    }

    @PostMapping("/clients/{id}/delete")
    public String deleteClient(@PathVariable Long id) {
        return json(() -> service.deleteClient(id));
    }

    @GetMapping("/accounts")
    public String accounts(@RequestParam(required = false) Long clientId,
                           @RequestParam(required = false) String accountType,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String createdFrom,
                           @RequestParam(required = false) String createdTo) {
        return json(() -> service.accounts(clientId, accountType, status, createdFrom, createdTo));
    }

    @GetMapping("/accounts/{id}")
    public String account(@PathVariable Long id) {
        return json(() -> service.account(id));
    }

    @PostMapping("/accounts/save")
    public String saveAccount(@RequestParam Map<String, String> params) {
        return json(() -> service.saveAccount(params));
    }

    @PostMapping("/accounts/{id}/operation")
    public String performOperation(@PathVariable Long id, @RequestParam Map<String, String> params) {
        return json(() -> service.performOperation(id, params));
    }

    @PostMapping("/accounts/{id}/close")
    public String closeAccount(@PathVariable Long id) {
        return json(() -> service.closeAccount(id));
    }

    private String json(Supplier<Map<String, Object>> action) {
        try {
            return JsonUtil.toJson(action.get());
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Ошибка обработки запроса: " + e.getMessage());
            return JsonUtil.toJson(error);
        }
    }
}
