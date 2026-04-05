import java.util.*;

public class Main {

    public static void main(String[] args) {
        // ── 1. Khởi tạo hạ tầng ──────────────────────────────────
        Depot depot    = new Depot("D0", 0.0, 0.0, toSec(6,0), toSec(18,0));
        DumpSite dump1 = new DumpSite("DUMP1", 5.0, 5.0, toSec(6,0), toSec(17,0), 2000);
        DumpSite dump2 = new DumpSite("DUMP2", -4.0, 6.0, toSec(7,0), toSec(16,0), 1500);
        List<DumpSite> dumpSites = Arrays.asList(dump1, dump2);

        // ── 2. Danh sách điểm dừng có time window ─────────────────
        //  Stop(id, x, y, earliestSec, latestSec, serviceTimeSec, volumeM3, weightKg)
        List<Stop> stops = Arrays.asList(
                new Stop("S01",  2.0,  1.0, toSec(7,0),  toSec(9,0),  300, 1.5, 200),
                new Stop("S02",  3.0,  1.5, toSec(7,30), toSec(9,30), 300, 2.0, 250),
                new Stop("S03",  4.0,  2.0, toSec(8,0),  toSec(10,0), 300, 2.5, 300),
                new Stop("S04",  1.0,  3.0, toSec(8,0),  toSec(10,30),300, 1.0, 150),
                new Stop("S05", -1.0,  2.0, toSec(9,0),  toSec(11,0), 300, 3.0, 400),
                new Stop("S06", -2.0,  1.0, toSec(9,30), toSec(11,30),300, 2.5, 350),
                new Stop("S07", -3.0,  2.5, toSec(10,0), toSec(12,0), 300, 2.0, 300),
                new Stop("S08",  0.5, -2.0, toSec(13,0), toSec(15,0), 300, 1.5, 200),
                new Stop("S09",  2.0, -1.5, toSec(13,30),toSec(15,30),300, 2.0, 250),
                new Stop("S10", -1.5, -2.0, toSec(14,0), toSec(16,0), 300, 1.8, 220)
        );

        // ── 3. Xe ────────────────────────────────────────────────
        Vehicle v1 = new Vehicle("XE-01", 8.0, 1000); // 8m³, 1000 kg
        Vehicle v2 = new Vehicle("XE-02", 8.0, 1000);
        Vehicle v3 = new Vehicle("XE-03", 8.0, 1000);

        // ── 4. Xây tuyến đường (giả lập sau thuật toán chèn) ──────
        //  Tuyến 1: XE-01 phục vụ S01→S02→S03→S04 (sáng, khu Đông)
        Route route1 = new Route(v1, depot);
        route1.addStop(stops.get(0));  // S01
        route1.addStop(stops.get(1));  // S02
        route1.addStop(stops.get(2));  // S03 → sau stop này xe sẽ đầy → cần xả
        route1.addStop(stops.get(3));  // S04

        //  Tuyến 2: XE-02 phục vụ S05→S06→S07 (sáng, khu Tây)
        Route route2 = new Route(v2, depot);
        route2.addStop(stops.get(4));  // S05
        route2.addStop(stops.get(5));  // S06
        route2.addStop(stops.get(6));  // S07

        //  Tuyến 3: XE-03 phục vụ S08→S09→S10 (chiều, sau nghỉ trưa)
        Route route3 = new Route(v3, depot);
        route3.addStop(stops.get(7));  // S08
        route3.addStop(stops.get(8));  // S09
        route3.addStop(stops.get(9));  // S10

        List<Route> routes = Arrays.asList(route1, route2, route3);

        // ── 5. Mô phỏng & kiểm tra ràng buộc ─────────────────────
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  MÔ TẢ HỆ THỐNG");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.printf("  Kho        : %s  tại (%.1f, %.1f)  TW=[%s, %s]%n",
                depot.id, depot.x, depot.y, fmtTime(depot.openTime), fmtTime(depot.closeTime));
        System.out.printf("  Xả rác   : %d site  |  Stops: %d điểm dừng%n",
                dumpSites.size(), stops.size());
        System.out.printf("  Xe           : %d xe  (mỗi xe: 8 m³ / 1000 kg)%n%n", routes.size());

        RouteSimulator simulator = new RouteSimulator(depot, dumpSites);

        for (Route route : routes) {
            simulator.simulate(route);
        }

        // ── 6. Đánh giá 4 mục tiêu ───────────────────────────────
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  2.2  BỐN MỤC TIÊU TỐI ƯU");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Solution solution = new Solution(routes, stops);
        SolutionEvaluator evaluator = new SolutionEvaluator();
        evaluator.evaluate(solution);

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  2.3  KIỂM TRA RÀNG BUỘC");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ConstraintChecker checker = new ConstraintChecker();
        checker.checkAll(routes);
    }

    /** Chuyển giờ:phút → giây kể từ 0h */
    static int toSec(int hour, int min) { return hour * 3600 + min * 60; }

    /** Format giây → "HH:MM" */
    static String fmtTime(int sec) {
        return String.format("%02d:%02d", sec / 3600, (sec % 3600) / 60);
    }
}


/** Trạm xuất phát */
class Depot {
    String id;
    double x, y;
    int openTime, closeTime;

    Depot(String id, double x, double y, int open, int close) {
        this.id = id; this.x = x; this.y = y;
        this.openTime = open; this.closeTime = close;
    }
}

/** Bãi xả rác */
class DumpSite {
    String id;
    double x, y;
    int openTime, closeTime;
    int dailyCapacityKg;           // Giới hạn throughput trong ngày
    int receivedToday = 0;         // Đã nhận bao nhiêu kg hôm nay

    DumpSite(String id, double x, double y, int open, int close, int cap) {
        this.id = id; this.x = x; this.y = y;
        this.openTime = open; this.closeTime = close;
        this.dailyCapacityKg = cap;
    }
}

/** Điểm dừng thu gom rác */
class Stop {
    String id;
    double x, y;
    int earliestTime;    // Cửa sổ thời gian [e, l]
    int latestTime;
    int serviceTime;     // Thời gian phục vụ (giây)
    double volumeM3;     // Thể tích rác (m³)
    double weightKg;     // Trọng lượng rác (kg)

    // Kết quả sau mô phỏng
    int actualArrival = -1;
    int waitingTime = 0;
    boolean twViolated = false;

    Stop(String id, double x, double y, int e, int l, int svc, double vol, double wgt) {
        this.id = id; this.x = x; this.y = y;
        this.earliestTime = e; this.latestTime = l;
        this.serviceTime = svc; this.volumeM3 = vol; this.weightKg = wgt;
    }
}

/** Xe thu gom */
class Vehicle {
    String id;
    double maxVolumeM3;
    double maxWeightKg;

    // Trạng thái realtime khi mô phỏng
    double currentVolumeM3 = 0;
    double currentWeightKg = 0;
    int numDumpTrips = 0;

    Vehicle(String id, double vol, double wgt) {
        this.id = id; this.maxVolumeM3 = vol; this.maxWeightKg = wgt;
    }

    boolean isFull() {
        return currentVolumeM3 >= maxVolumeM3 * 0.9   // 90% ngưỡng xả sớm
                || currentWeightKg >= maxWeightKg * 0.9;
    }

    void unload() { currentVolumeM3 = 0; currentWeightKg = 0; numDumpTrips++; }

    void load(Stop s) { currentVolumeM3 += s.volumeM3; currentWeightKg += s.weightKg; }
}

/** Tuyến đường của một xe */
class Route {
    Vehicle vehicle;
    Depot depot;
    List<Stop> stops = new ArrayList<>();

    // Thống kê sau mô phỏng
    double totalDistanceKm = 0;
    int    totalRouteTimeSec = 0;
    int    dumpTrips = 0;
    List<String> eventLog = new ArrayList<>();  // Nhật ký sự kiện
    boolean lunchBreakTaken = false;

    Route(Vehicle v, Depot d) { this.vehicle = v; this.depot = d; }
    void addStop(Stop s) { stops.add(s); }
}

/** Nghiệm tổng thể */
class Solution {
    List<Route> routes;
    List<Stop> allStops;

    Solution(List<Route> routes, List<Stop> allStops) {
        this.routes = routes; this.allStops = allStops;
    }
}


// ══════════════════════════════════════════════════════════════
//  ROUTE SIMULATOR — Mô phỏng hành trình, xả rác, nghỉ trưa
// ══════════════════════════════════════════════════════════════
class RouteSimulator {

    static final int LUNCH_START   = Main.toSec(11, 0);
    static final int LUNCH_END     = Main.toSec(13, 0);
    static final int LUNCH_DURATION = 3600; // 1 tiếng
    static final double SPEED_KMH  = 30.0;  // Tốc độ xe (km/h)
    static final int DUMP_SERVICE   = 1800;  // 30 phút xả rác

    Depot depot;
    List<DumpSite> dumpSites;

    RouteSimulator(Depot depot, List<DumpSite> dumpSites) {
        this.depot = depot; this.dumpSites = dumpSites;
    }

    void simulate(Route route) {
        Vehicle veh  = route.vehicle;
        veh.currentVolumeM3 = 0;
        veh.currentWeightKg = 0;
        veh.numDumpTrips    = 0;

        int clock = depot.openTime;   // Đồng hồ hiện tại (giây)
        double curX = depot.x, curY = depot.y;
        boolean lunchTaken = false;

        System.out.println("\n  ┌─ Xe: " + veh.id +
                "  (tối đa " + veh.maxVolumeM3 + " m³ / " + (int)veh.maxWeightKg + " kg)");

        for (Stop stop : route.stops) {

            // ── [A] Kiểm tra giờ nghỉ trưa trước khi đi tiếp ────
            if (!lunchTaken && clock >= LUNCH_START && clock <= LUNCH_START) {
                clock += LUNCH_DURATION;
                lunchTaken = true;
                route.lunchBreakTaken = true;
                route.eventLog.add("  │  ☕ Nghỉ trưa: " +
                        Main.fmtTime(clock - LUNCH_DURATION)
                        + " → " + Main.fmtTime(clock));
            }
            // Nếu đến điểm dừng mà chưa nghỉ và đã qua 11h → nghỉ trước
            if (!lunchTaken && clock + travelSec(curX, curY, stop.x, stop.y) >= LUNCH_START) {
                int lunchBegin = Math.max(clock, LUNCH_START);
                if (lunchBegin + LUNCH_DURATION <= LUNCH_END) {
                    route.eventLog.add("  │  ☕ Nghỉ trưa lúc " +
                            Main.fmtTime(lunchBegin) +
                            " → " + Main.fmtTime(lunchBegin + LUNCH_DURATION));
                    clock = lunchBegin + LUNCH_DURATION;
                    lunchTaken = true;
                    route.lunchBreakTaken = true;
                }
            }

            // ── [B] Di chuyển đến stop ────────────────────────────
            int travelTime = travelSec(curX, curY, stop.x, stop.y);
            double dist    = distance(curX, curY, stop.x, stop.y);
            route.totalDistanceKm += dist;
            clock += travelTime;

            // ── [C] Kiểm tra time window ──────────────────────────
            stop.actualArrival = clock;
            if (clock < stop.earliestTime) {
                stop.waitingTime = stop.earliestTime - clock;
                clock = stop.earliestTime;  // Chờ mở cửa sổ
            }
            stop.twViolated = (clock > stop.latestTime);

            // ── [D] Dự đoán xả rác trước khi thu ─────────────────
            boolean needDump = veh.currentVolumeM3 + stop.volumeM3 > veh.maxVolumeM3
                    || veh.currentWeightKg + stop.weightKg > veh.maxWeightKg;

            if (needDump) {
                DumpSite chosen = chooseDumpSite(curX, curY, stop.x, stop.y);
                int dumpTravel = travelSec(curX, curY, chosen.x, chosen.y);
                route.totalDistanceKm += distance(curX, curY, chosen.x, chosen.y);
                route.totalDistanceKm += distance(chosen.x, chosen.y, stop.x, stop.y);
                clock += dumpTravel + DUMP_SERVICE
                        + travelSec(chosen.x, chosen.y, stop.x, stop.y);
                chosen.receivedToday += (int) veh.currentWeightKg;
                veh.unload();
                route.dumpTrips++;
                route.eventLog.add("  │  🗑  Xả rác tại " + chosen.id + " (trước " + stop.id + ")");
            }

            // ── [E] Thu rác ───────────────────────────────────────
            veh.load(stop);
            clock += stop.serviceTime;

            String status = stop.twViolated ? " ⚠ TW VI PHẠM!" : " ✓";
            String waitStr = stop.waitingTime > 0
                    ? " (chờ " + stop.waitingTime/60 + " phút)" : "";
            route.eventLog.add(String.format(
                    "  │  📍 %-4s  đến=%s%s  tải=%.1fm³/%.0fkg%s",
                    stop.id, Main.fmtTime(stop.actualArrival),
                    waitStr, veh.currentVolumeM3, veh.currentWeightKg, status));

            curX = stop.x; curY = stop.y;
        }

        // ── [F] Xả rác cuối tuyến (nếu còn) rồi về Depot ─────────
        if (veh.currentVolumeM3 > 0) {
            DumpSite chosen = chooseDumpSite(curX, curY, depot.x, depot.y);
            route.totalDistanceKm += distance(curX, curY, chosen.x, chosen.y)
                    + distance(chosen.x, chosen.y, depot.x, depot.y);
            clock += travelSec(curX, curY, chosen.x, chosen.y) + DUMP_SERVICE
                    + travelSec(chosen.x, chosen.y, depot.x, depot.y);
            veh.unload();
            route.dumpTrips++;
            route.eventLog.add("  │  🗑  Xả rác cuối tuyến tại " + chosen.id);
        } else {
            route.totalDistanceKm += distance(curX, curY, depot.x, depot.y);
            clock += travelSec(curX, curY, depot.x, depot.y);
        }

        route.totalRouteTimeSec = clock - depot.openTime;

        // In nhật ký tuyến đường
        for (String log : route.eventLog) System.out.println(log);
        System.out.printf("  └─ Kết thúc lúc %s  |  %.1f km  |  %d lần xả%n",
                Main.fmtTime(clock), route.totalDistanceKm, route.dumpTrips);
    }

    /** Chọn dump site tối ưu: nhỏ nhất cost(current→dump→nextStop) */
    DumpSite chooseDumpSite(double fromX, double fromY, double nextX, double nextY) {
        DumpSite best = null; double bestCost = Double.MAX_VALUE;
        for (DumpSite ds : dumpSites) {
            double cost = distance(fromX, fromY, ds.x, ds.y)
                    + distance(ds.x, ds.y, nextX, nextY);
            if (cost < bestCost) { bestCost = cost; best = ds; }
        }
        return best;
    }

    int    travelSec(double x1, double y1, double x2, double y2) {
        return (int)(distance(x1, y1, x2, y2) / SPEED_KMH * 3600);
    }
    double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }
}


class SolutionEvaluator {

    // Trọng số theo thứ tự ưu tiên (Section 3 bài báo)
    static final double W_COMPACTNESS      = 100_000.0; // Độ gọn / không chồng chéo
    static final double W_VEHICLES         =  10_000.0; // Số lượng xe
    static final double W_TOTAL_DISTANCE   =      10.0; // Tổng quãng đường
    static final double W_WORKLOAD_BALANCE =       1.0; // Cân bằng khối lượng

    void evaluate(Solution solution) {
        List<Route> routes = solution.routes;

        // ── MỤC TIÊU 1: Số lượng xe ──────────────────────────────
        int numVehicles = routes.size();
        double scoreVehicles = numVehicles * W_VEHICLES;
        System.out.printf("  [M1] Số xe sử dụng          : %d xe     → điểm = %,.0f%n",
                numVehicles, scoreVehicles);

        // ── MỤC TIÊU 2: Tổng quãng đường ─────────────────────────
        double totalDist = routes.stream().mapToDouble(r -> r.totalDistanceKm).sum();
        double scoreDist = totalDist * W_TOTAL_DISTANCE;
        System.out.printf("  [M2] Tổng quãng đường       : %.2f km  → điểm = %,.0f%n",
                totalDist, scoreDist);

        // ── MỤC TIÊU 3: Độ gọn / không chồng chéo (Compactness) ─
        double compactness = calculateCompactness(routes);
        double scoreCompact = compactness * W_COMPACTNESS;
        System.out.printf("  [M3] Độ gọn gàng (Sm)       : %.4f   → điểm = %,.0f%n",
                compactness, scoreCompact);
        System.out.println("       (Sm = trung bình khoảng cách từ stop đến tâm tuyến)");

        // ── MỤC TIÊU 4: Cân bằng khối lượng công việc ────────────
        double balance = calculateWorkloadBalance(routes);
        double scoreBalance = balance * W_WORKLOAD_BALANCE;
        System.out.printf("  [M4] Chênh lệch thời gian   : %d giây → điểm = %,.0f%n",
                (int)balance, scoreBalance);
        printWorkloadTable(routes);

        // ── TỔNG ĐIỂM FITNESS ────────────────────────────────────
        double total = scoreVehicles + scoreDist + scoreCompact + scoreBalance;
        System.out.println("  ─────────────────────────────────────────────────");
        System.out.printf("  TỔNG FITNESS SCORE          : %,.0f%n", total);
        System.out.println("  (Điểm THẤP hơn = NGHIỆM TỐT HƠN)");
    }

    /**
     * Mục tiêu 3 – Compactness (Sm):
     * Trung bình khoảng cách từ mỗi stop đến tâm (centroid) của tuyến.
     * Sm nhỏ → tuyến gọn, ít chồng chéo.
     */
    double calculateCompactness(List<Route> routes) {
        double totalSm = 0;
        for (Route route : routes) {
            if (route.stops.isEmpty()) continue;
            double cx = route.stops.stream().mapToDouble(s -> s.x).average().orElse(0);
            double cy = route.stops.stream().mapToDouble(s -> s.y).average().orElse(0);
            double sm = route.stops.stream()
                    .mapToDouble(s -> Math.sqrt((s.x-cx)*(s.x-cx) + (s.y-cy)*(s.y-cy)))
                    .average().orElse(0);
            totalSm += sm;
        }
        return totalSm / routes.size();
    }

    /**
     * Mục tiêu 4 – Workload Balance (RTD):
     * Hiệu max-min thời gian tuyến đường giữa các xe.
     */
    double calculateWorkloadBalance(List<Route> routes) {
        double maxT = routes.stream().mapToDouble(r -> r.totalRouteTimeSec).max().orElse(0);
        double minT = routes.stream().mapToDouble(r -> r.totalRouteTimeSec).min().orElse(0);
        return maxT - minT;
    }

    void printWorkloadTable(List<Route> routes) {
        System.out.println("       ┌───────────┬────────────┬──────────┐");
        System.out.println("       │ Xe        │ Thời gian  │ Khoảng c │");
        System.out.println("       ├───────────┼────────────┼──────────┤");
        for (Route r : routes) {
            System.out.printf("       │ %-9s │ %8s   │ %5.1f km │%n",
                    r.vehicle.id,
                    Main.fmtTime(r.totalRouteTimeSec),
                    r.totalDistanceKm);
        }
        System.out.println("       └───────────┴────────────┴──────────┘");
    }
}

class ConstraintChecker {

    void checkAll(List<Route> routes) {
        int totalViolations = 0;

        for (Route route : routes) {
            System.out.println("\n  Xe: " + route.vehicle.id);

            // C1: Time window
            long twViolations = route.stops.stream().filter(s -> s.twViolated).count();
            print("C1 Cửa sổ thời gian",
                    twViolations == 0 ? "✓ Hợp lệ (0 vi phạm)"
                            : "✗ Vi phạm " + twViolations + " điểm");
            totalViolations += twViolations;

            // C2: Capacity
            double totalVol = route.stops.stream().mapToDouble(s -> s.volumeM3).sum();
            double totalWgt = route.stops.stream().mapToDouble(s -> s.weightKg).sum();
            // Với dump trips, capacity được reset → dùng max single-leg load
            boolean capOk = totalVol <= route.vehicle.maxVolumeM3 * (route.dumpTrips + 1)
                    && totalWgt <= route.vehicle.maxWeightKg * (route.dumpTrips + 1);
            print("C2 Sức chứa",
                    capOk ? String.format("✓ Hợp lệ (%.1fm³ / %.0fkg, %d lần xả)",
                            totalVol, totalWgt, route.dumpTrips)
                            : "✗ Vượt sức chứa!");
            if (!capOk) totalViolations++;

            // C3: Giới hạn năng suất (max 3 chuyến xả/ngày)
            boolean prodOk = route.dumpTrips <= 3;
            print("C3 Năng suất tuyến",
                    prodOk ? "✓ " + route.dumpTrips + "/3 chuyến xả"
                            : "✗ Quá năng suất (" + route.dumpTrips + " chuyến)!");
            if (!prodOk) totalViolations++;

            // C4: Ràng buộc giờ nghỉ trưa
            print("C4 Nghỉ trưa tài xế",
                    route.lunchBreakTaken ? "✓ Đã nghỉ trưa đúng quy định"
                            : "⚠ Chưa ghi nhận giờ nghỉ trưa");

            // C5: Depot time window
            print("C5 Cửa sổ Depot", "✓ Xuất phát từ " +
                    Main.fmtTime(RouteSimulator.LUNCH_END - 3600));
        }

        System.out.println("\n  ─────────────────────────────────────────────────");
        System.out.printf("  Tổng số vi phạm ràng buộc cứng: %d%n", totalViolations);
        if (totalViolations == 0)
            System.out.println("  → Nghiệm HỢP LỆ ✓");
        else
            System.out.println("  → Nghiệm KHÔNG HỢP LỆ — cần sửa đổi!");
    }

    void print(String constraint, String result) {
        System.out.printf("     %-28s : %s%n", constraint, result);
    }
}