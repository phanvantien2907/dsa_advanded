import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DataSeeder {

    // Danh sách số lượng điểm dừng cần sinh ra theo yêu cầu bài báo
    private static final int[] FILE_SIZES = {102, 277, 335, 444, 804, 1051, 1351, 1599, 1932, 2100};
    private static final String FOLDER_PATH = "data";

    // Các hằng số giới hạn để sinh dữ liệu sao cho thực tế
    private static final double MIN_COORD = -20.0;
    private static final double MAX_COORD = 20.0;
    private static final int DEPOT_OPEN = 6 * 3600;      // 06:00 (21600s)
    private static final int DEPOT_CLOSE = 18 * 3600;    // 18:00 (64800s)
    private static final int MIN_TW_DURATION = 2 * 3600; // Cửa sổ thời gian tối thiểu 2 tiếng
    private static final int SVC_TIME = 300;             // 5 phút phục vụ (300s)

    public static void main(String[] args) {
        System.out.println("🚀 Bắt đầu quá trình Seeding dữ liệu đa luồng...");

        // 1. Tạo thư mục /data nếu chưa tồn tại
        File directory = new File(FOLDER_PATH);
        if (!directory.exists()) {
            boolean created = directory.mkdir();
            System.out.println(created ? "📁 Đã tạo thư mục: /" + FOLDER_PATH : "❌ Không thể tạo thư mục!");
        }

        // 2. Khởi tạo Thread Pool (Dùng số luồng bằng số nhân CPU để tối ưu)
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(processors);
        System.out.println("⚙️ Đang chạy với " + processors + " luồng (threads)...");

        long startTime = System.currentTimeMillis();

        // 3. Giao việc (Task) cho các luồng để tạo file song song
        for (int size : FILE_SIZES) {
            executor.submit(() -> generateMockFile(size));
        }

        // 4. Đóng Pool và chờ các luồng hoàn thành
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("❌ Quá trình seeding bị gián đoạn!");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✅ Seeding hoàn tất trong " + (endTime - startTime) + " ms!");
        System.out.println("Tất cả các file đã nằm gọn trong folder /" + FOLDER_PATH);
    }

    /**
     * Hàm sinh ra 1 file dữ liệu cụ thể
     */
    private static void generateMockFile(int numStops) {
        String fileName = FOLDER_PATH + "/" + numStops + "_stop.txt";
        Random random = new Random();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Ghi Header
            writer.write(String.format("%-7s %-8s %-8s %-16s %-15s %-9s %-7s %-7s%n",
                        "//ID", "X", "Y", "EarlyStart(s)", "LateStart(s)", "Svc(s)", "Vol(m3)", "Wgt(kg)"));
            writer.write("//--------------------------------------------------------------------------------------\n");

            // Sinh từng dòng dữ liệu
            for (int i = 1; i <= numStops; i++) {
                String id = String.format("S%04d", i); // Format ID: S0001, S0002...

                // Sinh tọa độ X, Y
                double x = MIN_COORD + (MAX_COORD - MIN_COORD) * random.nextDouble();
                double y = MIN_COORD + (MAX_COORD - MIN_COORD) * random.nextDouble();

                // Sinh Time Window hợp lý (bảo đảm thời gian mở phải trước thời gian đóng kho khá dài)
                int earlyStart = DEPOT_OPEN + random.nextInt(DEPOT_CLOSE - DEPOT_OPEN - MIN_TW_DURATION);
                int lateStart = earlyStart + MIN_TW_DURATION + random.nextInt(4 * 3600); // Thêm 2-4 tiếng
                if (lateStart > DEPOT_CLOSE) lateStart = DEPOT_CLOSE;

                // Sinh Khối lượng và Thể tích
                double vol = 0.5 + 2.5 * random.nextDouble(); // Từ 0.5 đến 3.0 m3
                double wgt = 50.0 + 350.0 * random.nextDouble(); // Từ 50 đến 400 kg

                // Ghi vào file (căn lề cho đẹp)
                writer.write(String.format("%-7s %-8.1f %-8.1f %-16d %-15d %-9d %-7.1f %-7.1f%n",
                        id, x, y, earlyStart, lateStart, SVC_TIME, vol, wgt));
            }

            System.out.println("✔️ Đã tạo xong: " + fileName);

        } catch (IOException e) {
            System.err.println("❌ Lỗi khi ghi file " + fileName + ": " + e.getMessage());
        }
    }
}