import java.io.*;
import java.util.*;
import java.util.stream.*;


/**
 * ═══════════════════════════════════════════════════════════════
 *  VRPTW THU GOM RÁC — SPRINT 3
 *  Ch4: Extended Insertion Algorithm  (Solomon mở rộng)
 *  Ch5: Clustering-based VRPTW        (K-means + Insertion + SA)
 *  Ch6: So sánh kết quả thực nghiệm  (Sm, Nh, RTD, TD)
 * ═══════════════════════════════════════════════════════════════
 */
public class Main {

    // ── Thông số hệ thống ───────────────────────────────────────
    static final double SPEED     = 30.0;   // km/h
    static final int    DUMP_SVC  = 1800;   // 30 phút xả rác (giây)
    static final double MAX_VOL   = 8.0;    // m³/xe
    static final double MAX_WGT   = 1000.0; // kg/xe
    static final int    DEPOT_OPEN  = toS(6, 0);
    static final int    DEPOT_CLOSE = toS(18, 0);
    static final int    LUNCH_S   = toS(11, 0);
    static final int    LUNCH_E   = toS(13, 0);
    static final int    LUNCH_D   = 3600;   // 1 tiếng

    public static void main(String[] args) {
        String[] files = {
                "102_stop.txt", "277_stop.txt", "335_stop.txt",
                "444_stop.txt", "804_stop.txt", "1051_stop.txt",
                "1351_stop.txt", "1599_stop.txt", "1932_stop.txt", "2100_stop.txt"
        };

        banner("BẮT ĐẦU CHẠY BENCHMARK ĐA LUỒNG (BATCH PROCESSING)");
        System.out.println("Đang xử lý " + files.length + " files song song. Vui lòng đợi...\n");

        long totalStart = System.currentTimeMillis();

        // Chạy đa luồng — mỗi file 1 luồng riêng
        List<BenchmarkResult> results = Arrays.stream(files)
                .parallel()
                .map(Main::runSingleBenchmark)
                .collect(Collectors.toList());

        long totalEnd = System.currentTimeMillis();

        printFinalReportTable(results, totalEnd - totalStart);
    }

    /**
     * Benchmark cho 1 file:
     *   Ch4 — ExtendedInsertion trực tiếp trên toàn bộ stops
     *   Ch5 — ClusteringVRPTW: phân cụm → lưu file .txt → load lại → ExtendedInsertion
     */
    private static BenchmarkResult runSingleBenchmark(String fileName) {
        List<Stop> stops = DataLoader.loadStops(fileName);
        BenchmarkResult result = new BenchmarkResult(fileName, stops.size());
        if (stops.isEmpty()) return result;

        Depot depot = new Depot(0, 0);
        List<DumpSite> dumps = Arrays.asList(
                new DumpSite("DS1", 10, 15),
                new DumpSite("DS2", -12, -18));

        try {
            // ── Ch4: Extended Insertion trực tiếp ──────────────────
            long t1 = System.currentTimeMillis();
            Solution sol4 = new ExtendedInsertion(depot, dumps).solve(deepCopy(stops));
            long t2 = System.currentTimeMillis();

            result.v4    = sol4.routes.size();
            result.td4   = sol4.routes.stream().mapToDouble(r -> r.distKm).sum();
            result.sm4   = Metrics.sm(sol4.routes);
            result.nh4   = Metrics.nh(sol4.routes);
            result.rtd4  = Metrics.rtd(sol4.routes);
            result.time4Ms = t2 - t1;

            // ── Ch5: Clustering → lưu file → load → ExtendedInsertion
            long t3 = System.currentTimeMillis();
            Solution sol5 = new ClusteringVRPTW(depot, dumps).solve(fileName);
            long t4 = System.currentTimeMillis();

            result.v5    = sol5.routes.size();
            result.td5   = sol5.routes.stream().mapToDouble(r -> r.distKm).sum();
            result.sm5   = Metrics.sm(sol5.routes);
            result.nh5   = Metrics.nh(sol5.routes);
            result.rtd5  = Metrics.rtd(sol5.routes);
            result.time5Ms = t4 - t3;

        } catch (Exception e) {
            System.err.println("❌ Lỗi tại file " + fileName + ": " + e.getMessage());
        }

        System.out.println("✔ Xong: " + fileName);
        return result;
    }

    /** Bảng so sánh kết quả thực nghiệm (dùng cho bài báo). */
    private static void printFinalReportTable(List<BenchmarkResult> results, long totalTime) {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                 BẢNG KẾT QUẢ THỰC NGHIỆM (COMPUTATIONAL RESULTS)                               ║");
        System.out.println("╠════════════════╦══════╦══════╦══════════════╦══════╦══════════════╦════════════╦═══════════════╣");
        System.out.println("║ Problem set    ║ Algo ║  Vn  ║   Sm (km)    ║  Nh  ║   TD (km)    ║  RTD (sec) ║   CT (ms)     ║");
        System.out.println("╠════════════════╬══════╬══════╬══════════════╬══════╬══════════════╬════════════╬═══════════════╣");

        results.sort(Comparator.comparingInt(a -> a.numStops));

        for (BenchmarkResult r : results) {
            if (r.numStops == 0) continue;
            System.out.printf("║ %-14s ║  Ch4 ║ %4d ║ %12.2f ║ %4d ║ %12.1f ║ %10d ║ %13d ║%n",
                    r.fileName, r.v4, r.sm4, r.nh4, r.td4, (int)r.rtd4, r.time4Ms);
            System.out.printf("║ %-14s ║  Ch5 ║ %4d ║ %12.2f ║ %4d ║ %12.1f ║ %10d ║ %13d ║%n",
                    "", r.v5, r.sm5, r.nh5, r.td5, (int)r.rtd5, r.time5Ms);
            System.out.println("╟────────────────╫──────╫──────╫──────────────╫──────╫──────────────╫────────────╫───────────────╢");
        }
        System.out.printf("║ TỔNG THỜI GIAN CHẠY TOÀN BỘ BENCHMARK (ĐA LUỒNG): %-54d ms ║%n", totalTime);
        System.out.println("╚════════════════╩══════╩══════╩══════════════╩══════╩══════════════╩════════════╩═══════════════╝");
    }

    static int toS(int h, int m) { return h * 3600 + m * 60; }
    static String fmt(int s) { return String.format("%02d:%02d", s/3600, (s%3600)/60); }
    static double dist(double x1, double y1, double x2, double y2) { return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)); }
    static int travel(double x1, double y1, double x2, double y2) { return (int)(dist(x1,y1,x2,y2) / SPEED * 3600); }
    static List<Stop> deepCopy(List<Stop> src) { return src.stream().map(Stop::copy).collect(Collectors.toList()); }
    static void banner(String t) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.printf( "║  %-56s║%n", t);
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }

}

class BenchmarkResult {
    String fileName; int numStops;
    int v4; double td4, sm4; int nh4; double rtd4; long time4Ms;
    int v5; double td5, sm5; int nh5; double rtd5; long time5Ms;
    BenchmarkResult(String f, int n) { fileName=f; numStops=n; }
}

class DataLoader {
    public static List<Stop> loadStops(String fileName) {
        List<Stop> stops = new ArrayList<>();
        File file = new File("data/" + fileName);
        if (!file.exists()) {
            System.err.println("❌ Không tìm thấy file: " + file.getAbsolutePath());
            return stops;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("//")) continue;

                String[] p = line.trim().split("\\s+");
                if (p.length >= 8) {
                    stops.add(new Stop(p[0],
                            Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                            Integer.parseInt(p[5]),
                            Double.parseDouble(p[6]), Double.parseDouble(p[7])));
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc file " + fileName + ": " + e.getMessage());
        }
        return stops;
    }

    /** Load stops từ đường dẫn tuyệt đối/tương đối (dùng cho cluster files). */
    public static List<Stop> loadStopsFromPath(String path) {
        List<Stop> stops = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("❌ Không tìm thấy cluster file: " + file.getAbsolutePath());
            return stops;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("//")) continue;
                String[] p = line.trim().split("\\s+");
                if (p.length >= 8) {
                    stops.add(new Stop(p[0],
                            Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]),
                            Integer.parseInt(p[5]),
                            Double.parseDouble(p[6]), Double.parseDouble(p[7])));
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc cluster file " + path + ": " + e.getMessage());
        }
        return stops;
    }
}

class Stop {
    String id; double x, y;
    int e, l, svc;
    double vol, wgt;
    boolean routed = false;

    Stop(String id, double x, double y, int e, int l, int svc, double vol, double wgt) {
        this.id=id; this.x=x; this.y=y; this.e=e; this.l=l; this.svc=svc;
        this.vol=vol; this.wgt=wgt;
    }
    Stop copy() { return new Stop(id,x,y,e,l,svc,vol,wgt); }
}

class Depot { double x, y; Depot(double x, double y){this.x=x;this.y=y;} }

class DumpSite { String id; double x, y;
    DumpSite(String id, double x, double y){this.id=id;this.x=x;this.y=y;} }

class Route {
    String id;
    List<Stop> stops    = new ArrayList<>();
    double distKm       = 0;
    int totalTimeSec    = 0;
    int dumpCount       = 0;
    boolean lunchDone   = false;
    Route(String id) { this.id = id; }
}

class Cluster {
    int id; double cx, cy; boolean finalized = false;
    List<Stop> stops = new ArrayList<>();
    Cluster(int id) { this.id = id; }
    void recenter() {
        if (stops.isEmpty()) return;
        cx = stops.stream().mapToDouble(s->s.x).average().orElse(0);
        cy = stops.stream().mapToDouble(s->s.y).average().orElse(0);
    }
    double totalVol() { return stops.stream().mapToDouble(s->s.vol).sum(); }
}

class Solution { List<Route> routes; Solution(List<Route> r) { routes=r; } }
class InsertResult { int pos; double cost; InsertResult(int p, double c){pos=p;cost=c;} }


class ExtendedInsertion {

    Depot depot; List<DumpSite> dumps;
    int vehicleCount = 0;
    // Tham số Solomon: α1, α2 trọng số c1; λ cho c2
    static final double A1=0.5, A2=0.5, MU=1.0, LAMBDA=1.0;

    ExtendedInsertion(Depot d, List<DumpSite> ds) { depot=d; dumps=ds; }

    Solution solve(List<Stop> stops) {
        System.out.println("\n  [Ch4] ── Bắt đầu Extended Insertion Algorithm ──");

        // Bước 0: Đánh dấu tất cả unrouted
        stops.forEach(s -> s.routed = false);
        List<Route> routes = new ArrayList<>();

        // Bước 1: Lặp chừng nào còn unrouted
        while (hasUnrouted(stops)) {

            // Bước 2: Tạo tuyến T mới cho một xe
            Route T = new Route("V" + (++vehicleCount));
            boolean lunchCheck = false;  // trạng thái nghỉ trưa = chưa thực hiện

            System.out.printf("  [Ch4] Tạo tuyến %s...%n", T.id);

            // Bước 3: Chọn điểm hạt nhân (seed stop)
            Stop seed = selectSeed(stops, depot.x, depot.y, Main.DEPOT_OPEN);
            if (seed == null) { System.out.println("  [Ch4] Không tìm được seed → kết thúc."); break; }

            seed.routed = true;
            List<Stop> CR = new ArrayList<>();
            CR.add(seed);
            System.out.printf("  [Ch4]   Seed: %s | TW=[%s,%s] vol=%.1f%n",
                    seed.id, Main.fmt(seed.e),
                    Main.fmt(seed.l), seed.vol);

            // Bước 4: Chèn Solomon
            insertSolomon(CR, stops, depot, lunchCheck);

            // Bước 5: Kiểm tra capacity → tách nếu cần
            double volSum = CR.stream().mapToDouble(s->s.vol).sum();
            double wgtSum = CR.stream().mapToDouble(s->s.wgt).sum();

            if (volSum <= Main.MAX_VOL &&
                    wgtSum <= Main.MAX_WGT) {
                // Hợp lệ: đưa toàn bộ CR vào T
                T.stops.addAll(CR);
                System.out.printf("  [Ch4]   CR hợp lệ: %d stops, vol=%.1f%n", CR.size(), volSum);
            } else {
                // Tách CR → SR1 + SR2
                List<Stop> SR1 = new ArrayList<>(), SR2 = new ArrayList<>();
                double cumVol = 0;
                for (Stop s : CR) {
                    if (cumVol + s.vol <= Main.MAX_VOL) {
                        SR1.add(s); cumVol += s.vol;
                    } else {
                        s.routed = false; SR2.add(s);   // đẩy lại unrouted
                    }
                }
                T.stops.addAll(SR1);
                lunchCheck = T.lunchDone;
                System.out.printf("  [Ch4]   Tách: SR1=%d stops, SR2=%d → unrouted%n",
                        SR1.size(), SR2.size());
            }

            calcMetrics(T);
            routes.add(T);
            System.out.printf("  [Ch4]   %s: %d stops | %.1fkm | %d dump | lunch=%b%n",
                    T.id, T.stops.size(), T.distKm, T.dumpCount, T.lunchDone);
        }

        // Bước 6: Tối ưu vị trí xả rác
        System.out.println("  [Ch4] Bước 6: Tính lại metrics...");
        routes.forEach(this::calcMetrics);

        // Bước 7: Kiểm tra lunch break
        System.out.println("  [Ch4] Bước 7: Kiểm tra lunch break...");
        for (Route r : routes) {
            if (r.totalTimeSec + Main.DEPOT_OPEN < Main.LUNCH_S) {
                r.lunchDone = false;
            } else {
                r.lunchDone = true;
            }
        }

        // Bước 8: Hoàn tất
        System.out.printf("  [Ch4] Bước 8: Hoàn tất! → %d tuyến đường%n", routes.size());
        return new Solution(routes);
    }

    boolean hasUnrouted(List<Stop> stops) {
        return stops.stream().anyMatch(s -> !s.routed);
    }

    /**
     * Solomon seed: điểm xa depot nhất (hoặc xa điểm tham chiếu nhất)
     * có time window hợp lệ tại thời điểm hiện tại.
     */
    Stop selectSeed(List<Stop> stops, double refX, double refY, int clock) {
        return stops.stream()
                .filter(s -> !s.routed)
                .filter(s -> clock + Main.travel(refX,refY,s.x,s.y) <= s.l)
                .max(Comparator.comparingDouble(s ->
                        Main.dist(s.x, s.y, refX, refY)))
                .orElse(
                        // fallback: lấy stop có TW sớm nhất nếu không tìm được
                        stops.stream().filter(s -> !s.routed)
                                .min(Comparator.comparingInt(s -> s.e)).orElse(null));
    }

    /**
     * Bước 4: Chèn tuần tự Solomon vào sub-route CR.
     * Chọn u có c2 lớn nhất, chèn vào vị trí c1 nhỏ nhất.
     */
    void insertSolomon(List<Stop> CR, List<Stop> allStops,
                       Depot depot, boolean lunchDone) {
        boolean inserted = true;
        while (inserted) {
            inserted = false;
            double currentVol = CR.stream().mapToDouble(s->s.vol).sum();
            double currentWgt = CR.stream().mapToDouble(s->s.wgt).sum();
            Stop bestU = null; int bestPos = -1;
            double bestC2 = Double.NEGATIVE_INFINITY;

            for (Stop u : allStops) {
                if (u.routed) continue;
                if (currentVol + u.vol > Main.MAX_VOL || currentWgt + u.wgt > Main.MAX_WGT) {
                    continue;
                }
                InsertResult res = findBestInsert(u, CR);
                if (res == null) continue;

                // c2 = λ*dist(depot,u) - c1_best
                double c2 = LAMBDA * Main.dist(depot.x, depot.y, u.x, u.y)
                        - res.cost;
                if (c2 > bestC2) { bestC2=c2; bestU=u; bestPos=res.pos; }
            }

            if (bestU != null) {
                CR.add(bestPos, bestU);
                bestU.routed = true;
                inserted = true;
            }
        }
    }

    /**
     * Tìm vị trí chèn tốt nhất cho u vào CR: tính cost c1 tại mỗi vị trí.
     */
    InsertResult findBestInsert(Stop u, List<Stop> CR) {
        // Tính mảng arrival time hiện tại của CR
        int[] arrTime = new int[CR.size()];
        double px = depot.x, py = depot.y;
        int clock = Main.DEPOT_OPEN;
        for (int i = 0; i < CR.size(); i++) {
            Stop s = CR.get(i);
            clock += Main.travel(px, py, s.x, s.y);
            clock = Math.max(clock, s.e);
            arrTime[i] = clock;
            clock += s.svc;
            px = s.x; py = s.y;
        }

        double bestC1 = Double.MAX_VALUE; int bestPos = -1;

        for (int i = 0; i <= CR.size(); i++) {
            double prevX = (i==0) ? depot.x : CR.get(i-1).x;
            double prevY = (i==0) ? depot.y : CR.get(i-1).y;
            int    prevT = (i==0) ? Main.DEPOT_OPEN
                    : arrTime[i-1] + CR.get(i-1).svc;

            double nextX = (i<CR.size()) ? CR.get(i).x : depot.x;
            double nextY = (i<CR.size()) ? CR.get(i).y : depot.y;

            int arrU = prevT + Main.travel(prevX, prevY, u.x, u.y);
            if (arrU < u.e) arrU = u.e;    // chờ mở TW
            if (arrU > u.l) continue;       // vi phạm TW → bỏ qua

            int depU    = arrU + u.svc;
            int arrNext = depU + Main.travel(u.x, u.y, nextX, nextY);
            int origNext = (i<CR.size()) ? arrTime[i] : 0;

            // Kiểm tra TW của stop kế tiếp không bị vi phạm
            if (i < CR.size() && arrNext > CR.get(i).l) continue;

            double d_pu = Main.dist(prevX, prevY, u.x, u.y);
            double d_un = Main.dist(u.x, u.y, nextX, nextY);
            double d_pn = Main.dist(prevX, prevY, nextX, nextY);

            double c1 = A1*(d_pu+d_un - MU*d_pn) + A2*(arrNext - origNext);

            if (c1 < bestC1) { bestC1=c1; bestPos=i; }
        }
        return bestPos == -1 ? null : new InsertResult(bestPos, bestC1);
    }

    /** Tính lại toàn bộ metrics của tuyến: dist, time, dump, lunch. */
    void calcMetrics(Route route) {
        if (route.stops.isEmpty()) return;
        double dist=0, cumVol=0;
        double curX=depot.x, curY=depot.y;
        int clock = Main.DEPOT_OPEN;
        int dumps = 0; boolean lunchDone = false;

        for (Stop s : route.stops) {
            // Kiểm tra nghỉ trưa
            if (!lunchDone) {
                int eta = clock + Main.travel(curX, curY, s.x, s.y);
                if (eta >= Main.LUNCH_S) {
                    clock = Main.LUNCH_S + Main.LUNCH_D;
                    lunchDone = true;
                }
            }
            // Kiểm tra xả rác trước khi thu
            if (cumVol + s.vol > Main.MAX_VOL) {
                DumpSite ds = nearestDump(curX, curY);
                dist  += Main.dist(curX,curY,ds.x,ds.y)
                        + Main.dist(ds.x,ds.y,s.x,s.y);
                clock += Main.travel(curX,curY,ds.x,ds.y)
                        + Main.DUMP_SVC
                        + Main.travel(ds.x,ds.y,s.x,s.y);
                curX=s.x; curY=s.y; cumVol=0; dumps++;
            } else {
                dist  += Main.dist(curX,curY,s.x,s.y);
                clock += Main.travel(curX,curY,s.x,s.y);
                curX=s.x; curY=s.y;
            }
            clock = Math.max(clock, s.e);
            clock += s.svc;
            cumVol += s.vol;
        }
        // Xả rác cuối + về depot
        if (cumVol > 0) {
            DumpSite ds = nearestDump(curX, curY);
            dist += Main.dist(curX,curY,ds.x,ds.y)
                    + Main.dist(ds.x,ds.y,depot.x,depot.y);
            dumps++;
        } else {
            dist += Main.dist(curX,curY,depot.x,depot.y);
        }
        route.distKm      = Math.round(dist * 100.0) / 100.0;
        route.totalTimeSec= clock - Main.DEPOT_OPEN;
        route.dumpCount   = dumps;
        route.lunchDone   = lunchDone;
    }

    DumpSite nearestDump(double x, double y) {
        return dumps.stream()
                .min(Comparator.comparingDouble(d -> Main.dist(x,y,d.x,d.y)))
                .orElseThrow();
    }
}

class ClusteringVRPTW {

    Depot depot; List<DumpSite> dumps;
    static final double MAX_VOL_DAY = Main.MAX_VOL * 2; // 2 chuyến/ngày
    static final String CLUSTER_DIR = "data/clusters/";

    ClusteringVRPTW(Depot d, List<DumpSite> ds) { depot=d; dumps=ds; }

    /**
     * Pipeline:
     * 1. Load stops từ fileName
     * 2. Phân cụm K-means
     * 3. Ghi mỗi cluster ra file .txt tạm (data/clusters/)
     * 4. Load lại từng file cluster → feed vào ExtendedInsertion
     * 5. Tổng hợp kết quả và render ra console
     */
    Solution solve(String fileName) throws IOException {
        System.out.println("\n  [Ch5] ── Bắt đầu Clustering-based VRPTW ──");

        // ── BƯỚC 0: Load dữ liệu đầu vào ──
        System.out.println("  [Ch5] Bước 0: Load dữ liệu từ " + fileName);
        List<Stop> stops = DataLoader.loadStops(fileName);
        if (stops.isEmpty()) throw new IOException("Không tải được dữ liệu: " + fileName);

        double totalVol = stops.stream().mapToDouble(s->s.vol).sum();
        int N = Math.max(2, (int)Math.ceil(totalVol / MAX_VOL_DAY));
        System.out.printf("  [Ch5]   Tổng stops=%d | TotalVol=%.1fm³ → N=%d xe ước tính%n",
                stops.size(), totalVol, N);

        // Tạo thư mục cluster nếu chưa có
        new File(CLUSTER_DIR).mkdirs();

        List<Route> finalRoutes = null;

        while (true) {
            // ── BƯỚC 1: Phân cụm K-means có ràng buộc capacity ──
            System.out.printf("%n  [Ch5] Bước 1: Phân cụm Capacitated K-means (N=%d)...%n", N);
            List<Cluster> clusters = capacitatedKMeans(stops, N);

            System.out.println("  [Ch5]   Cải thiện compactness (swap points)...");
            improveCompactness(clusters);

            clusters.sort((a,b) -> b.stops.size() - a.stops.size());
            for (int i=0; i<clusters.size(); i++) clusters.get(i).id = i+1;

            // ── BƯỚC 2: Ghi mỗi cluster ra file .txt ──
            System.out.println("  [Ch5] Bước 2: Ghi cluster files...");
            String baseFile = fileName.replace(".txt", "");
            List<String> clusterFiles = new ArrayList<>();
            for (Cluster cluster : clusters) {
                String cfName = baseFile + "_cluster" + cluster.id + ".txt";
                String cfPath = CLUSTER_DIR + cfName;
                writeClusterFile(cfPath, cluster.stops);
                clusterFiles.add(cfPath);
                System.out.printf("  [Ch5]   Đã ghi Cụm %d (%d stops) → %s%n",
                        cluster.id, cluster.stops.size(), cfPath);
            }

            // ── BƯỚC 3: Load lại từng cluster file → ExtendedInsertion ──
            System.out.println("  [Ch5] Bước 3: Chạy ExtendedInsertion trên từng cluster...");
            List<Route> routes = new ArrayList<>();
            List<Stop> unassigned = new ArrayList<>();
            int vId = 0;

            for (int ci = 0; ci < clusters.size(); ci++) {
                Cluster cluster = clusters.get(ci);
                String cfPath = clusterFiles.get(ci);

                // Load lại stops từ cluster file
                List<Stop> clusterStops = DataLoader.loadStopsFromPath(cfPath);
                clusterStops.forEach(s -> s.routed = false);

                System.out.printf("  [Ch5]   Cụm %d (%d stops, vol=%.1f):  load từ %s%n",
                        cluster.id, clusterStops.size(), cluster.totalVol(), cfPath);

                ExtendedInsertion ei = new ExtendedInsertion(depot, dumps);
                ei.vehicleCount = vId;
                Solution subSol = ei.solve(clusterStops);

                for (Route r : subSol.routes) {
                    routes.add(r);
                    System.out.printf("  [Ch5]     → %s: %d stops, %.1fkm%n",
                            r.id, r.stops.size(), r.distKm);
                }
                vId = ei.vehicleCount;

                for (Stop s : clusterStops) {
                    if (!s.routed) {
                        unassigned.add(s);
                        System.out.printf("  [Ch5]     ⚠ %s không chèn được → reassign%n", s.id);
                    }
                }

                cluster.finalized = cluster.totalVol() >= MAX_VOL_DAY * 0.9;
            }

            // Reassign unassigned → cụm chưa chốt gần nhất
            for (Stop s : unassigned) {
                Cluster best = clusters.stream()
                        .filter(c -> !c.finalized)
                        .min(Comparator.comparingDouble(c -> Main.dist(s.x,s.y,c.cx,c.cy)))
                        .orElse(clusters.get(clusters.size()-1));
                best.stops.add(s);
                System.out.printf("  [Ch5]   Reassign %s → Cụm %d%n", s.id, best.id);
            }

            // ── BƯỚC 4: Kiểm tra còn stop nào chưa lên tuyến? ──
            long stillUnrouted = routes.stream()
                    .flatMap(r -> r.stops.stream())
                    .count();
            long expectedTotal = stops.size() - unassigned.size();
            // Kiểm tra theo unassigned còn lại sau reassign
            if (unassigned.isEmpty()) {
                System.out.printf("  [Ch5] Bước 4: Tất cả stops đã lên tuyến ✓%n");
                finalRoutes = routes;
                break;
            } else {
                N++;
                System.out.printf("  [Ch5] Bước 4: Còn %d stops → Tăng N=%d, phân cụm lại!%n",
                        unassigned.size(), N);
                // Reset routed flag trên stops gốc
                stops.forEach(s -> s.routed = false);
            }
        }

        // ── BƯỚC 5: Hoàn tất ──
        System.out.printf("%n  [Ch5] Bước 5: Hoàn tất! → %d tuyến đường%n", finalRoutes.size());
        printRouteSummary(finalRoutes);
        return new Solution(finalRoutes);
    }

    /** Ghi danh sách stops ra file text (định dạng tương tự input). */
    private void writeClusterFile(String path, List<Stop> stops) throws IOException {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(path))) {
            for (Stop s : stops) {
                pw.printf("%s %.4f %.4f %d %d %d %.4f %.4f%n",
                        s.id, s.x, s.y, s.e, s.l, s.svc, s.vol, s.wgt);
            }
        }
    }

    /** In tóm tắt kết quả các tuyến ra console. */
    private void printRouteSummary(List<Route> routes) {
        System.out.println("\n  [Ch5] ── KẾT QUẢ TUYẾN ĐƯỜNG ──");
        System.out.println("  ┌────────┬────────┬──────────┬───────┬──────────────┐");
        System.out.println("  │ Tuyến  │ Stops  │ Dist(km) │ Dumps │ Lunch        │");
        System.out.println("  ├────────┼────────┼──────────┼───────┼──────────────┤");
        for (Route r : routes) {
            System.out.printf("  │ %-6s │ %6d │ %8.2f │ %5d │ %-12s │%n",
                    r.id, r.stops.size(), r.distKm, r.dumpCount,
                    r.lunchDone ? "Có nghỉ trưa" : "Không nghỉ");
        }
        System.out.println("  └────────┴────────┴──────────┴───────┴──────────────┘");
        double totalKm = routes.stream().mapToDouble(r->r.distKm).sum();
        System.out.printf("  Tổng: %d tuyến | %.2f km tổng%n", routes.size(), totalKm);
    }

    /**
     * Bước 1a: Capacitated K-means với Grand Centroid.
     * Grand Centroid = trung tâm của tất cả trọng tâm cụm,
     * giúp phân bổ xe đều quanh trung tâm vùng phục vụ.
     */
    List<Cluster> capacitatedKMeans(List<Stop> stops, int K) {
        List<Cluster> clusters = new ArrayList<>();
        for (int i=0; i<K; i++) clusters.add(new Cluster(i+1));

        // Tính Grand Centroid
        double gcX = stops.stream().mapToDouble(s->s.x).average().orElse(0);
        double gcY = stops.stream().mapToDouble(s->s.y).average().orElse(0);
        System.out.printf("  [Ch5]   Grand Centroid = (%.2f, %.2f)%n", gcX, gcY);

        // Khởi tạo tâm cụm: phân bổ đều trên vòng tròn quanh Grand Centroid
        double maxR = stops.stream()
                .mapToDouble(s -> Main.dist(s.x,s.y,gcX,gcY))
                .max().orElse(3.0) * 0.6;
        for (int i=0; i<K; i++) {
            double angle = 2 * Math.PI * i / K;
            clusters.get(i).cx = gcX + maxR * Math.cos(angle);
            clusters.get(i).cy = gcY + maxR * Math.sin(angle);
        }

        // Lặp K-means (tối đa 30 vòng) có ràng buộc capacity
        for (int iter=0; iter<30; iter++) {
            clusters.forEach(c -> c.stops.clear());

            // Sort theo TW sớm để ưu tiên gom stop cấp bách trước
            List<Stop> sorted = stops.stream()
                    .sorted((a, b) -> Double.compare(
                            Main.dist(b.x, b.y, gcX, gcY),
                            Main.dist(a.x, a.y, gcX, gcY)))
                    .collect(Collectors.toList());

            for (Stop s : sorted) {
                Cluster best = null; double bestD = Double.MAX_VALUE;
                for (Cluster c : clusters) {
                    if (c.totalVol() + s.vol > MAX_VOL_DAY) continue;
                    double d = Main.dist(s.x,s.y,c.cx,c.cy);
                    if (d < bestD) { bestD=d; best=c; }
                }
                if (best == null)  // fallback: cụm ít tải nhất
                    best = clusters.stream()
                            .min(Comparator.comparingDouble(Cluster::totalVol)).orElseThrow();
                best.stops.add(s);
            }
            clusters.forEach(Cluster::recenter);
        }

        for (Cluster c : clusters)
            System.out.printf("  [Ch5]   Cụm %d: %d stops | vol=%.1fm³ | c=(%.2f,%.2f)%n",
                    c.id, c.stops.size(), c.totalVol(), c.cx, c.cy);
        return clusters;
    }

    /**
     * Bước 1b: Cải thiện compactness = hoán đổi stop giữa cụm i↔j
     * nếu giảm được tổng Sm mà không vi phạm capacity.
     */
    void improveCompactness(List<Cluster> clusters) {
        int swaps = 0;
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i=0; i<clusters.size()-1; i++) {
                for (int j=i+1; j<clusters.size(); j++) {
                    Cluster ci=clusters.get(i), cj=clusters.get(j);
                    outer:
                    for (Stop si : new ArrayList<>(ci.stops)) {
                        for (Stop sj : new ArrayList<>(cj.stops)) {
                            double vi2 = ci.totalVol() - si.vol + sj.vol;
                            double vj2 = cj.totalVol() - sj.vol + si.vol;
                            if (vi2>MAX_VOL_DAY || vj2>MAX_VOL_DAY) continue;

                            double smBefore = smOf(ci) + smOf(cj);
                            ci.stops.remove(si); ci.stops.add(sj);
                            cj.stops.remove(sj); cj.stops.add(si);
                            ci.recenter(); cj.recenter();

                            if (smOf(ci)+smOf(cj) < smBefore) {
                                improved=true; swaps++; break outer;
                            } else { // hoàn tác
                                ci.stops.remove(sj); ci.stops.add(si);
                                cj.stops.remove(si); cj.stops.add(sj);
                                ci.recenter(); cj.recenter();
                            }
                        }
                    }
                }
            }
        }
        System.out.printf("  [Ch5]   Compactness improved: %d swaps%n", swaps);
    }

    double smOf(Cluster c) {
        if (c.stops.isEmpty()) return 0;
        return c.stops.stream()
                .mapToDouble(s -> Main.dist(s.x,s.y,c.cx,c.cy))
                .average().orElse(0);
    }
}


class Metrics {

    /** Sm: trung bình khoảng cách từ mỗi stop đến tâm của tuyến mình. */
    static double sm(List<Route> routes) {
        if (routes.isEmpty()) return 0;
        double sum = 0; int count = 0;
        for (Route r : routes) {
            if (r.stops.isEmpty()) continue;
            double cx = r.stops.stream().mapToDouble(s->s.x).average().orElse(0);
            double cy = r.stops.stream().mapToDouble(s->s.y).average().orElse(0);
            sum += r.stops.stream()
                    .mapToDouble(s -> Main.dist(s.x,s.y,cx,cy))
                    .average().orElse(0);
            count++;
        }
        return count == 0 ? 0 : sum / count;
    }

    /**
     * Nh (Hull Overlap): số điểm dừng của tuyến i nằm bên trong
     * Convex Hull của tuyến j (i≠j). Dùng thuật toán Graham's Scan.
     */
    static int nh(List<Route> routes) {
        if (routes.size() < 2) return 0;
        // Build hull cho mỗi tuyến
        List<List<Stop>> hulls = routes.stream()
                .map(r -> GrahamScan.compute(r.stops))
                .collect(Collectors.toList());
        int total = 0;
        for (int i=0; i<routes.size(); i++)
            for (int j=0; j<routes.size(); j++) {
                if (i==j) continue;
                for (Stop s : routes.get(i).stops)
                    if (GrahamScan.pointInside(s, hulls.get(j))) total++;
            }
        return total;
    }

    /** RTD: chênh lệch thời gian max-min giữa các tuyến (cân bằng khối lượng). */
    static double rtd(List<Route> routes) {
        if (routes.isEmpty()) return 0;
        double max = routes.stream().mapToDouble(r->r.totalTimeSec).max().orElse(0);
        double min = routes.stream().mapToDouble(r->r.totalTimeSec).min().orElse(0);
        return max - min;
    }
}

/** Graham's Scan — tính Convex Hull và kiểm tra point-in-polygon. */
class GrahamScan {

    static List<Stop> compute(List<Stop> pts) {
        if (pts.size() < 3) return new ArrayList<>(pts);

        // Pivot = điểm y nhỏ nhất (trái nhất nếu bằng)
        Stop pivot = pts.stream()
                .min(Comparator.comparingDouble((Stop s)->s.y)
                        .thenComparingDouble(s->s.x)).orElseThrow();

        List<Stop> sorted = pts.stream()
                .filter(s -> s != pivot)
                .sorted(Comparator.comparingDouble(s ->
                        Math.atan2(s.y-pivot.y, s.x-pivot.x)))
                .collect(Collectors.toList());

        Deque<Stop> stack = new ArrayDeque<>();
        stack.push(pivot);
        if (!sorted.isEmpty()) stack.push(sorted.get(0));

        for (int i=1; i<sorted.size(); i++) {
            Stop p = sorted.get(i);
            while (stack.size() >= 2) {
                Stop[] arr = stack.toArray(new Stop[0]);
                if (cross(arr[1], arr[0], p) <= 0) stack.pop();
                else break;
            }
            stack.push(p);
        }
        return new ArrayList<>(stack);
    }

    static double cross(Stop o, Stop a, Stop b) {
        return (a.x-o.x)*(b.y-o.y) - (a.y-o.y)*(b.x-o.x);
    }

    /** Ray-casting algorithm: kiểm tra point p nằm trong polygon hay không. */
    static boolean pointInside(Stop p, List<Stop> poly) {
        if (poly.size() < 3) return false;
        boolean inside = false; int n = poly.size();
        for (int i=0, j=n-1; i<n; j=i++) {
            Stop vi=poly.get(i), vj=poly.get(j);
            if (((vi.y>p.y) != (vj.y>p.y)) &&
                    (p.x < (vj.x-vi.x)*(p.y-vi.y)/(vj.y-vi.y)+vi.x))
                inside = !inside;
        }
        return inside;
    }
}
