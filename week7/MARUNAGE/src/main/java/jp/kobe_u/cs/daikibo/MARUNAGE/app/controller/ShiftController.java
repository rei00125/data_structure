package jp.kobe_u.cs.daikibo.MARUNAGE.app.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String showAdminGenerate(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month, Model model) {
        LocalDate today = LocalDate.now();
        if (year == null) {
            year = today.getYear();
        }
        if (month == null) {
            month = today.getMonthValue();
        }

        // 月の境界を越える処理
        if (month == 0) {
            month = 12;
            year--;
        } else if (month == 13) {
            month = 1;
            year++;
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        List<LocalDate> daysInMonth = IntStream.rangeClosed(1, yearMonth.lengthOfMonth())
                .mapToObj(yearMonth::atDay)
                .collect(Collectors.toList());

        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("daysInMonth", daysInMonth);

        // --- ダミーデータ ---
        List<String> positions = List.of("ホール", "キッチン", "洗い場");
        List<String> timeZones = List.of("9:00-12:00", "12:00-15:00", "15:00-18:00", "18:00-21:00");
        
        Map<String, Map<String, Map<Integer, String>>> shifts = new HashMap<>();
        for (String pos : positions) {
            Map<String, Map<Integer, String>> tzMap = new HashMap<>();
            for (String tz : timeZones) {
                Map<Integer, String> dayMap = new HashMap<>();
                for (LocalDate day : daysInMonth) {
                    // 簡単なダミーロジック
                    if (Math.random() > 0.7) {
                        dayMap.put(day.getDayOfMonth(), "田中");
                    }
                }
                tzMap.put(tz, dayMap);
            }
            shifts.put(pos, tzMap);
        }
        
        model.addAttribute("positions", positions);
        model.addAttribute("timeZones", timeZones);
        model.addAttribute("shifts", shifts);
        // --- ダミーデータここまで ---

        return "admin_generate";
    }
}
