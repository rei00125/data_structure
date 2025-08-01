package jp.kobe_u.cs.daikibo.MARUNAGE.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ShiftController {

    @GetMapping("/")
    public String showIndex() {
        return "index";
    }

    @GetMapping("/employee/menu")
    public String showEmployeeMenu() {
        return "employee_menu";
    }

    @GetMapping("/admin/menu")
    public String showAdminMenu() {
        return "admin_menu";
    }

    @GetMapping("/shifts/new")
    public String showShiftNew() {
        return "shift_new";
    }

    @GetMapping("/shifts/confirm")
    public String showShiftConfirm() {
        return "shift_confirm";
    }

    @GetMapping("/admin/register")
    public String showAdminRegister() {
        return "admin_register";
    }

    @GetMapping("/admin/generate")
    public String showAdminGenerate() {
        return "admin_generate";
    }
}
