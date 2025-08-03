package jp.kobe_u.cs.daikibo.MARUNAGE.app.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ShiftService {

    // (ShiftSlotクラスや他の既存メソッドはこのままなので、ここでは省略します)
    // ShiftSlotクラスをShiftAssignerの内部クラスとして定義
    public class ShiftSlot {
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

        public String getDate() { return date; }
        public String getTimeZone() { return timeZone; }
        public String getPosition() { return position; }
        public String getStaffName() { return staffName; }

        @Override
        public String toString() {
            return "{" + " date: \"" + date + "\"," + " timeZone: \"" + timeZone + "\"," + " position: \"" + position + "\"," + " staffName: \"" + staffName + "\"" + " }";
        }
    }

    private static final String[] DATES = { "7/21 (月)", "7/22 (火)", "7/23 (水)", "7/24 (木)", "7/25 (金)", "7/26 (土)", "7/27 (日)" };
    private static final String[] TIME_ZONES = { "朝", "昼", "夜" };
    private static final String[] POSITIONS = { "キッチン", "ホール", "レジ" };
    private static final int TARGET_SHIFTS_PER_STAFF = 3;

    public String[] getDATES() { return DATES; }
    public String[] getTIME_ZONES() { return TIME_ZONES; }
    public String[] getPOSITIONS() { return POSITIONS; }

    public Map<String, List<ShiftSlot>> readAvailabilityFromCsv(String filePath) throws IOException {
        Map<String, List<ShiftSlot>> availabilityMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                String[] values = line.split(",");
                if (values.length >= 4) {
                    String staffName = values[0].trim();
                    String date = values[1].trim();
                    String timeZone = values[2].trim();
                    String position = values[3].trim();
                    availabilityMap.computeIfAbsent(staffName, k -> new ArrayList<>()).add(new ShiftSlot(date, timeZone, position, staffName));
                }
            }
        }
        return availabilityMap;
    }

    public List<ShiftSlot> assignShifts(Map<String, List<ShiftSlot>> availabilityMap) {
        List<ShiftSlot> assignedShifts = new ArrayList<>();
        Map<String, Integer> staffShiftCount = new HashMap<>();
        Map<String, Set<String>> assignedStaffAtDateTime = new HashMap<>();
        for (String staffName : availabilityMap.keySet()) { staffShiftCount.put(staffName, 0); }
        for (String date : DATES) {
            for (String timeZone : TIME_ZONES) {
                String dateTimeKey = date + "_" + timeZone;
                assignedStaffAtDateTime.put(dateTimeKey, new HashSet<>());
                for (String position : POSITIONS) {
                    String assignedStaff = findBestStaffForSlot(date, timeZone, position, availabilityMap, staffShiftCount, assignedStaffAtDateTime.get(dateTimeKey));
                    if (assignedStaff == null) { assignedStaff = findLeastBurdenedStaffOverall(staffShiftCount, assignedStaffAtDateTime.get(dateTimeKey)); }
                    ShiftSlot currentSlot = new ShiftSlot(date, timeZone, position, assignedStaff);
                    if (assignedStaff != null) {
                        staffShiftCount.put(assignedStaff, staffShiftCount.get(assignedStaff) + 1);
                        assignedStaffAtDateTime.get(dateTimeKey).add(assignedStaff);
                    }
                    assignedShifts.add(currentSlot);
                }
            }
        }
        return assignedShifts;
    }

    private String findBestStaffForSlot(String date, String timeZone, String position, Map<String, List<ShiftSlot>> availabilityMap, Map<String, Integer> staffShiftCount, Set<String> alreadyAssignedInCurrentTimeSlot) {
        String bestStaff = null;
        int minShifts = Integer.MAX_VALUE;
        List<String> staffNames = new ArrayList<>(availabilityMap.keySet());
        Collections.shuffle(staffNames);
        List<String> candidatesUnderTarget = new ArrayList<>();
        List<String> candidatesOverTarget = new ArrayList<>();
        for (String staffName : staffNames) {
            if (alreadyAssignedInCurrentTimeSlot.contains(staffName)) continue;
            boolean isAvailable = availabilityMap.get(staffName).stream().anyMatch(s -> s.getDate().equals(date) && s.getTimeZone().equals(timeZone) && s.getPosition().equals(position));
            if (isAvailable) {
                if (staffShiftCount.getOrDefault(staffName, 0) < TARGET_SHIFTS_PER_STAFF) { candidatesUnderTarget.add(staffName); } else { candidatesOverTarget.add(staffName); }
            }
        }
        for (String staffName : candidatesUnderTarget) {
            int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
            if (currentShifts < minShifts) { minShifts = currentShifts; bestStaff = staffName; }
        }
        if (bestStaff == null) {
            minShifts = Integer.MAX_VALUE;
            for (String staffName : candidatesOverTarget) {
                int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
                if (currentShifts < minShifts) { minShifts = currentShifts; bestStaff = staffName; }
            }
        }
        return bestStaff;
    }

    private String findLeastBurdenedStaffOverall(Map<String, Integer> staffShiftCount, Set<String> alreadyAssignedInCurrentTimeSlot) {
        if (staffShiftCount.isEmpty()) return null;
        String leastBurdenedStaff = null;
        int minShifts = Integer.MAX_VALUE;
        List<String> allStaffNames = new ArrayList<>(staffShiftCount.keySet());
        Collections.shuffle(allStaffNames);
        List<String> candidatesUnderTarget = new ArrayList<>();
        List<String> candidatesOverTarget = new ArrayList<>();
        for (String staffName : allStaffNames) {
            if (alreadyAssignedInCurrentTimeSlot.contains(staffName)) continue;
            int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
            if (currentShifts < TARGET_SHIFTS_PER_STAFF) { candidatesUnderTarget.add(staffName); } else { candidatesOverTarget.add(staffName); }
        }
        for (String staffName : candidatesUnderTarget) {
            int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
            if (currentShifts < minShifts) { minShifts = currentShifts; leastBurdenedStaff = staffName; }
        }
        if (leastBurdenedStaff == null) {
            minShifts = Integer.MAX_VALUE;
            for (String staffName : candidatesOverTarget) {
                int currentShifts = staffShiftCount.getOrDefault(staffName, 0);
                if (currentShifts < minShifts) { minShifts = currentShifts; leastBurdenedStaff = staffName; }
            }
        }
        return leastBurdenedStaff;
    }

    // ★★★ ここからがデバッグコードを含むメソッドです ★★★

    private static final String SHIFT_WISH_CSV_PATH = "/shiftwish.csv";

    public Map<String, Object> getShiftWishData() {
        System.out.println("\n[デバッグ] getShiftWishData() メソッドが呼び出されました。");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Map<String, Map<String, List<String>>>> shiftData = new LinkedHashMap<>();
        Set<String> dateSet = new LinkedHashSet<>();

        try (InputStream is = getClass().getResourceAsStream(SHIFT_WISH_CSV_PATH)) {
            
            if (is == null) {
                System.err.println("[デバッグ] エラー: " + SHIFT_WISH_CSV_PATH + " が見つかりません。nullです。");
                System.err.println("[デバッグ] src/main/resources/ の直下に shiftwish.csv があるか確認してください。");
                return result; // ファイルが見つからないので空のデータを返す
            }
            
            System.out.println("[デバッグ] ファイルが見つかりました。読み込みを開始します。");
            
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            
            String line;
            br.readLine(); // ヘッダー行をスキップ
            
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                // (データ処理は変更なし)
                String[] values = line.split(",");
                if (values.length >= 4) {
                    String staffName = values[0].trim();
                    String date = values[1].trim();
                    String timeZone = values[2].trim();
                    String position = values[3].trim();

                    dateSet.add(date);
                    shiftData.computeIfAbsent(position, k -> new LinkedHashMap<>())
                            .computeIfAbsent(timeZone, k -> new LinkedHashMap<>())
                            .computeIfAbsent(date, k -> new ArrayList<>())
                            .add(staffName);
                }
            }
            System.out.println("[デバッグ] ファイルの読み込みが完了しました。読み込んだ行数: " + lineCount);

        } catch (IOException e) {
            System.err.println("[デバッグ] ファイル読み込み中にエラーが発生しました。");
            e.printStackTrace();
        }

        System.out.println("[デバッグ] 最終的なデータサイズ: shiftData のポジション数 = " + shiftData.size());
        System.out.println("[デバッグ] 収集した日付の数: " + dateSet.size());
        System.out.println("[デバッグ] メソッドの処理を終了します。\n");

        result.put("shiftData", shiftData);
        result.put("dates", new ArrayList<>(dateSet));
        result.put("positions", Arrays.asList(POSITIONS));
        result.put("timeZones", Arrays.asList(TIME_ZONES));

        return result;
    }
}
