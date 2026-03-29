import java.util.ArrayList;
import java.util.List;

public class Main {
    static int N = 6; // 1 depot + 5 customers

    // Ma trận khoảng cách
    static int[][] distance = {
            {0, 10, 15, 20, 10, 25},
            {10, 0, 35, 25, 17, 30},
            {15, 35, 0, 30, 28, 40},
            {20, 25, 30, 0, 22, 18},
            {10, 17, 28, 22, 0, 16},
            {25, 30, 40, 18, 16, 0}
    };

    public static void main(String[] args) {

        boolean[] visited = new boolean[N];
        int current = 0; // bắt đầu từ depot
        int totalDistance = 0;

        List<Integer> route = new ArrayList<>();
        route.add(0);
        visited[0] = true;

        for (int i = 1; i < N; i++) {

            int nextCity = -1;
            int minDist = Integer.MAX_VALUE;

            for (int j = 0; j < N; j++) {
                if (!visited[j] && distance[current][j] < minDist) {
                    minDist = distance[current][j];
                    nextCity = j;
                }
            }

            route.add(nextCity);
            visited[nextCity] = true;
            totalDistance += minDist;
            current = nextCity;
        }

        // quay lại depot
        totalDistance += distance[current][0];
        route.add(0);

        System.out.println("Route: " + route);
        System.out.println("Total Distance: " + totalDistance);
    }
}