// ShiftAssigner.java (または ShiftService.java)
package jp.kobe_u.cs.daikibo.MARUNAGE.app.service; // あなたのパッケージ宣言に合わせてください

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ShiftService { // ファイル名をShiftService.javaにしている場合、ここもShiftServiceにしてください

    // ShiftSlotクラスをShiftAssignerの内部クラスとして定義
    public static class ShiftSlot {
        private String date;
        private String timeZone;
        private String position;
        private String staffName;

        public ShiftSlot(String date, String timeZone, String position, String staffName) {
            this.date = date;
            this.timeZone = timeZone;
            this.position = position;
            this.staffName = staffName;
        }

        // 各フィールドのgetter
        public String getDate() { return date; }
        public String getTimeZone() { return timeZone; }
        public String getPosition() { return position; }
        public String getStaffName() { return staffName; }

        @Override
        public String toString() {
            return "{" +
                   " date: \"" + date + "\"," +
                   " timeZone: \"" + timeZone + "\"," +
                   " position: \"" + position + "\"," +
                   " staffName: \"" + staffName + "\"" +
                   " }";
        }
    }

    private static final String[] DATES = {"7/21 (月)", "7/22 (火)", "7/23 (水)", "7/24 (木)", "7/25 (金)", "7/26 (土)", "7/27 (日)"};
    private static final String[] TIME_ZONES = {"朝", "昼", "夜"};
    private static final String[] POSITIONS = {"キッチン", "ホール", "レジ"};

    // 目標シフト数: 63コマ / 21人 = 3コマ/人
    private static final int TARGET_SHIFTS_PER_STAFF = 3;

    public static void main(String[] args) {
        try {
            // 1. 希望シフトのCSVを読み込む
            Map<String, List<ShiftSlot>> availabilityMap = readAvailabilityFromCsv("shiftwish.csv");

            // 2. シフトを割り当てる
            List<ShiftSlot> assignedShifts = assignShifts(availabilityMap);

            // 3. 結果を出力
            System.out.println("--- 割り当てられたシフト一覧 (全63コマ) ---");
            for (ShiftSlot slot : assignedShifts) {
                System.out.println(slot);
            }

        } catch (IOException e) {
            System.err.println("CSVファイルの読み込み中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 希望シフトのCSVを読み込み、スタッフごとの勤務可能情報をMapで返す
     * @param filePath CSVファイルのパス
     * @return key: スタッフ名, value: 勤務可能なShiftSlotのリスト
     * @throws IOException ファイル読み込みエラー
     */
    private static Map<String, List<ShiftSlot>> readAvailabilityFromCsv(String filePath) throws IOException {
        Map<String, List<ShiftSlot>> availabilityMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] values = line.split(",");
                if (values.length >= 4) {
                    String staffName = values[0].trim();
                    String date = values[1].trim();
                    String timeZone = values[2].trim();
                    String position = values[3].trim();
                    
                    availabilityMap.computeIfAbsent(staffName, k -> new ArrayList<>())
                                   .add(new ShiftSlot(date, timeZone, position, staffName));
                }
            }
        }
        return availabilityMap;
    }

    /**
     * シフトの割り当てを行う
     * @param availabilityMap スタッフごとの勤務可能情報
     * @return 割り当て後のShiftSlotのリスト
     */
    private static List<ShiftSlot> assignShifts(Map<String, List<ShiftSlot>> availabilityMap) {
        List<ShiftSlot> assignedShifts = new ArrayList<>();
        Map<String, Integer> staffShiftCount = new HashMap<>();
        
        // 各日時の各時間帯で、どのスタッフが既に割り当てられたかを追跡するマップ
        // Key: "日付_時間帯" (例: "7/21 (月)_朝")
        // Value: その日時で既に割り当て済みのスタッフ名のセット
        Map<String, Set<String>> assignedStaffAtDateTime = new HashMap<>();

        // 全スタッフのシフト回数を0で初期化
        for (String staffName : availabilityMap.keySet()) {
            staffShiftCount.put(staffName, 0);
        }

        // 全ての63コマをループ
        for (String date : DATES) {
            for (String timeZone : TIME_ZONES) {
                // 新しい日時ブロックに入ったら、その日時の割り当て状況を初期化
                String dateTimeKey = date + "_" + timeZone;
                assignedStaffAtDateTime.put(dateTimeKey, new HashSet<>());

                for (String position : POSITIONS) {
                    String assignedStaff = null;
                    
                    // フェーズ1: 希望があり、掛け持ちがなく、目標シフト数に達していないスタッフの中から最もシフトが少ないスタッフを探す
                    assignedStaff = findBestStaffForSlot(date, timeZone, position, availabilityMap, 
                                                         staffShiftCount, assignedStaffAtDateTime.get(dateTimeKey));
                    
                    if (assignedStaff == null) {
                        // フェーズ2: 上記で見つからなかった場合、全スタッフの中から、掛け持ちがなく、
                        // 目標シフト数に達していないスタッフを優先して最もシフトが少ないスタッフを強制的に割り当てる
                        assignedStaff = findLeastBurdenedStaffOverall(staffShiftCount, assignedStaffAtDateTime.get(dateTimeKey));
                    }
                    
                    // 割り当てられたスタッフをShiftSlotに追加
                    ShiftSlot currentSlot = new ShiftSlot(date, timeZone, position, assignedStaff);
                    
                    // 割り当てが行われた場合のみシフト数をカウントし、割り当て状況を更新
                    if (assignedStaff != null) {
                        staffShiftCount.put(assignedStaff, staffShiftCount.get(assignedStaff) + 1);
                        assignedStaffAtDateTime.get(dateTimeKey).add(assignedStaff); // この日時で割り当て済みとして記録
                    }
                    
                    assignedShifts.add(currentSlot);
                }
            }
        }
        
        return assignedShifts;
    }

    /**
     * 特定のシフトコマに最適なスタッフを見つける（希望シフトを考慮し、掛け持ちと目標シフト数を回避）
     * @param date 日付
     * @param timeZone 時間帯
     * @param position ポジション
     * @param availabilityMap スタッフごとの勤務可能情報
     * @param staffShiftCount 各スタッフの割り当て済みシフト数
     * @param alreadyAssignedInCurrentTimeSlot 現在処理中の日時で既に割り当て済みのスタッフのセット
     * @return 割り当てるスタッフ名。見つからない場合は null。
     */
    private static String findBestStaffForSlot(String date, String timeZone, String position, 
                                                Map<String, List<ShiftSlot>> availabilityMap, 
                                                Map<String, Integer> staffShiftCount,
                                                Set<String> alreadyAssignedInCurrentTimeSlot) {
        String bestStaff = null;
        int minShifts = Integer.MAX_VALUE;

        List<String> staffNames = new ArrayList<>(availabilityMap.keySet());
        Collections.shuffle(staffNames); // ランダムな順序でスタッフをチェックすることで偏りを防ぐ

        // 目標シフト数に達していないスタッフを優先するためのリスト
        List<String> candidatesUnderTarget = new ArrayList<>();
        List<String> candidatesOverTarget = new ArrayList<>();

        for (String staffName : staffNames) {
            // 現在のタイムスロットで既に割り当てられているスタッフはスキップ
            if (alreadyAssignedInCurrentTimeSlot.contains(staffName)) {
                continue;
            }

            List<ShiftSlot> availableSlots = availabilityMap.get(staffName);
            boolean isAvailable = availableSlots.stream()
                .anyMatch(s -> s.getDate().equals(date) && 
                               s.getTimeZone().equals(timeZone) &&
                               s.getPosition().equals(position));
            
            if (isAvailable) {
                int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
                if (currentShifts < TARGET_SHIFTS_PER_STAFF) {
                    candidatesUnderTarget.add(staffName);
                } else {
                    candidatesOverTarget.add(staffName);
                }
            }
        }

        // まず目標シフト数以下のスタッフの中から最もシフトが少ない人を選ぶ
        for (String staffName : candidatesUnderTarget) {
            int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
            if (currentShifts < minShifts) {
                minShifts = currentShifts;
                bestStaff = staffName;
            }
        }

        // 目標シフト数以下のスタッフで割り当てられない場合のみ、目標シフト数以上のスタッフも考慮する
        if (bestStaff == null) {
            minShifts = Integer.MAX_VALUE; // リセット
            for (String staffName : candidatesOverTarget) {
                int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
                if (currentShifts < minShifts) {
                    minShifts = currentShifts;
                    bestStaff = staffName;
                }
            }
        }
        
        return bestStaff;
    }

    /**
     * 全スタッフの中から、最もシフトが少ないスタッフを見つける（強制割り当て用、掛け持ちと目標シフト数を回避）
     * @param staffShiftCount 各スタッフの割り当て済みシフト数
     * @param alreadyAssignedInCurrentTimeSlot 現在処理中の日時で既に割り当て済みのスタッフのセット
     * @return 最もシフトが少ないスタッフ名。スタッフが一人もいない場合は null。
     */
    private static String findLeastBurdenedStaffOverall(Map<String, Integer> staffShiftCount,
                                                         Set<String> alreadyAssignedInCurrentTimeSlot) {
        if (staffShiftCount.isEmpty()) {
            return null; // スタッフが一人もいない場合
        }

        String leastBurdenedStaff = null;
        int minShifts = Integer.MAX_VALUE;

        List<String> allStaffNames = new ArrayList<>(staffShiftCount.keySet());
        Collections.shuffle(allStaffNames); // 全スタッフの中から公平に選ぶためシャッフル

        // 目標シフト数に達していないスタッフを優先するためのリスト
        List<String> candidatesUnderTarget = new ArrayList<>();
        List<String> candidatesOverTarget = new ArrayList<>();

        for (String staffName : allStaffNames) {
            // 現在のタイムスロットで既に割り当てられているスタッフはスキップ
            if (alreadyAssignedInCurrentTimeSlot.contains(staffName)) {
                continue;
            }

            int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
            if (currentShifts < TARGET_SHIFTS_PER_STAFF) {
                candidatesUnderTarget.add(staffName);
            } else {
                candidatesOverTarget.add(staffName);
            }
        }
        
        // まず目標シフト数以下のスタッフの中から最もシフトが少ない人を選ぶ
        for (String staffName : candidatesUnderTarget) {
            int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
            if (currentShifts < minShifts) {
                minShifts = currentShifts;
                leastBurdenedStaff = staffName;
            }
        }

        // 目標シフト数以下のスタッフで割り当てられない場合のみ、目標シフト数以上のスタッフも考慮する
        if (leastBurdenedStaff == null) {
            minShifts = Integer.MAX_VALUE; // リセット
            for (String staffName : candidatesOverTarget) {
                int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
                if (currentShifts < minShifts) {
                    minShifts = currentShifts;
                    leastBurdenedStaff = staffName;
                }
            }
        }
        return leastBurdenedStaff;
    }
}