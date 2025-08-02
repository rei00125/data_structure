package jp.kobe_u.cs.daikibo.MARUNAGE.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.kobe_u.cs.daikibo.MARUNAGE.app.service.ShiftService;
import jp.kobe_u.cs.daikibo.MARUNAGE.app.service.ShiftService.ShiftSlot;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap; // 順序を保持するため
import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
public class ShiftController {

    @Autowired
    private ShiftService shiftService;

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
    public String showAdminGenerate(Model model) {
        try {
            // ShiftServiceのmainメソッドはstaticで、CSV読み込みパスが固定されているため、
            // SpringのDIで利用しやすいようにShiftServiceに新しいメソッドを追加するか、
            // mainメソッドのロジックをサービスメソッドとして公開する必要があります。
            // ここでは、ShiftServiceのmainメソッドのロジックを直接呼び出す形を想定します。
            // 実際には、ShiftServiceにassignAndGetShifts()のようなメソッドを追加し、
            // CSVパスを引数で渡すか、application.propertiesで設定するようにすべきです。

            // 仮の実装として、ShiftServiceのmainメソッドのロジックを模倣します。
            // 実際には、ShiftServiceに適切なメソッドを追加し、それを呼び出すべきです。
            Map<String, List<ShiftSlot>> availabilityMap = shiftService
                    .readAvailabilityFromCsv("src/main/java/jp/kobe_u/cs/daikibo/MARUNAGE/app/service/shiftwish.csv"); // パスを修正
            List<ShiftSlot> assignedShifts = shiftService.assignShifts(availabilityMap);

            // HTMLテンプレートに渡すためのデータ構造に変換
            // Map<ポジション名, Map<時間帯, Map<日付文字列, スタッフ名>>>
            Map<String, Map<String, Map<String, String>>> shiftsByPositionTimeDate = new LinkedHashMap<>();

            // 初期化
            for (String pos : shiftService.getPOSITIONS()) {
                shiftsByPositionTimeDate.put(pos, new LinkedHashMap<>());
                for (String tz : shiftService.getTIME_ZONES()) {
                    shiftsByPositionTimeDate.get(pos).put(tz, new LinkedHashMap<>());
                    for (String date : shiftService.getDATES()) {
                        shiftsByPositionTimeDate.get(pos).get(tz).put(date, ""); // 初期値は空文字列
                    }
                }
            }

            // 割り当てられたシフトをマップに格納
            for (ShiftSlot slot : assignedShifts) {
                shiftsByPositionTimeDate
                        .get(slot.getPosition())
                        .get(slot.getTimeZone())
                        .put(slot.getDate(), slot.getStaffName());
            }

            model.addAttribute("shifts", shiftsByPositionTimeDate);
            model.addAttribute("dayHeaders", shiftService.getDATES());
            model.addAttribute("positions", shiftService.getPOSITIONS());
            model.addAttribute("timeZones", shiftService.getTIME_ZONES());
            model.addAttribute("month", "7"); // 仮で7月を設定。実際には日付から動的に取得すべき

        } catch (IOException e) {
            model.addAttribute("errorMessage", "シフトの生成中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace(); // デバッグ用にスタックトレースを出力
        }
        return "admin_generate";
    }
}
