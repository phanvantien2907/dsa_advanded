import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Đang khởi tạo hệ thống đánh giá tuyến đường...");

        // 1. Tạo một danh sách các chuyến xe (giả lập dữ liệu sau khi chạy thuật toán chèn)
        List<Route> mockRoutes = new ArrayList<>();

        // Giả sử Xe 1 chạy hết 150 km, tốn 4500 giây
        Route route1 = new Route(150.0, 4500.0);
        // Giả sử Xe 2 chạy hết 120 km, tốn 4200 giây
        Route route2 = new Route(120.0, 4200.0);

        mockRoutes.add(route1);
        mockRoutes.add(route2);

        // 2. Gom các tuyến đường này thành một Giải pháp (Solution) tổng thể
        Solution currentSolution = new Solution(mockRoutes);

        // 3. Gọi "Người chấm thi" ra để tính điểm
        SolutionEvaluator evaluator = new SolutionEvaluator();
        double finalScore = evaluator.calculateFitnessScore(currentSolution);

        // 4. In kết quả
        System.out.println("--- KẾT QUẢ ĐÁNH GIÁ (FITNESS SCORE) ---");
        System.out.println("Số lượng xe sử dụng: " + mockRoutes.size());
        System.out.println("Tổng điểm Fitness Score: " + finalScore);
    }
}

class SolutionEvaluator {

    // Trọng số ưu tiên theo bài báo khoa học
    private static final double WEIGHT_COMPACTNESS = 100000.0;
    private static final double WEIGHT_VEHICLES = 10000.0;
    private static final double WEIGHT_DISTANCE = 10.0;
    private static final double WEIGHT_WORKLOAD_BALANCE = 1.0;

    public double calculateFitnessScore(Solution solution) {
        double score = 0.0;

        // 1. Đánh giá độ gọn (Sm) và chồng chéo (Nh)
        double compactnessPenalty = calculateCompactnessPenalty(solution.getRoutes());
        score += compactnessPenalty * WEIGHT_COMPACTNESS;

        // 2. Đánh giá số lượng xe (Vn)
        int numVehicles = solution.getRoutes().size();
        score += numVehicles * WEIGHT_VEHICLES;

        // 3. Đánh giá tổng quãng đường / thời gian (TD)
        double totalDistance = calculateTotalDistance(solution.getRoutes());
        score += totalDistance * WEIGHT_DISTANCE;

        // 4. Đánh giá cân bằng khối lượng công việc (RTD)
        double workloadDeviation = calculateWorkloadDeviation(solution.getRoutes());
        score += workloadDeviation * WEIGHT_WORKLOAD_BALANCE;

        return score;
    }

    private double calculateCompactnessPenalty(List<Route> routes) {
        // TODO: Viết thuật toán Bao lồi (Convex Hull) ở đây để đếm số điểm giao nhau (Nh)
        // TODO: Tính tổng khoảng cách từ các điểm dừng đến tâm của Route đó (Sm)
        // Tạm thời trả về 0 để test
        return 0.0;
    }

    private double calculateTotalDistance(List<Route> routes) {
        double totalDistance = 0.0;
        for (Route route : routes) {
            totalDistance += route.getTotalDistance();
        }
        return totalDistance;
    }

    private double calculateWorkloadDeviation(List<Route> routes) {
        if (routes.isEmpty()) return 0.0;

        double maxTime = Double.MIN_VALUE;
        double minTime = Double.MAX_VALUE;

        for (Route route : routes) {
            double routeTime = route.getTotalRouteTime();
            if (routeTime > maxTime) maxTime = routeTime;
            if (routeTime < minTime) minTime = routeTime;
        }
        return maxTime - minTime;
    }
}

class Solution {
    private List<Route> routes;

    // Constructor
    public Solution(List<Route> routes) {
        this.routes = routes;
    }

    public List<Route> getRoutes() {
        return routes;
    }
}

class Route {
    private double totalDistance;
    private double totalRouteTime;

    public Route(double totalDistance, double totalRouteTime) {
        this.totalDistance = totalDistance;
        this.totalRouteTime = totalRouteTime;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public double getTotalRouteTime() {
        return totalRouteTime;
    }
}