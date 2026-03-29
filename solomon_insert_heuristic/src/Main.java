import java.util.ArrayList;
import java.util.List;


public class Main {
    static double distance(Customer a, Customer b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }
    public static void main(String[] args) {
        Customer depot = new Customer(0, 0, 0);

        List<Customer> customers = new ArrayList<>();
        customers.add(new Customer(1, 2, 2));
        customers.add(new Customer(2, 6, 1));
        customers.add(new Customer(3, 4, 3));

        // STEP 1: chọn seed (xa depot nhất)
        Customer seed = customers.get(0);
        double maxDist = distance(depot, seed);

        for (Customer c : customers) {
            double d = distance(depot, c);
            if (d > maxDist) {
                maxDist = d;
                seed = c;
            }
        }

        List<Customer> route = new ArrayList<>();
        route.add(depot);
        route.add(seed);
        route.add(depot);

        customers.remove(seed);

        // STEP 2: insertion heuristic
        while (!customers.isEmpty()) {

            double bestCost = Double.MAX_VALUE;
            Customer bestCustomer = null;
            int bestPosition = -1;

            for (Customer c : customers) {

                for (int i = 0; i < route.size() - 1; i++) {

                    Customer a = route.get(i);
                    Customer b = route.get(i + 1);

                    double cost = distance(a, c) + distance(c, b) - distance(a, b);

                    if (cost < bestCost) {
                        bestCost = cost;
                        bestCustomer = c;
                        bestPosition = i + 1;
                    }
                }
            }

            route.add(bestPosition, bestCustomer);
            customers.remove(bestCustomer);
        }


        System.out.println("Final Route:");

        for (Customer c : route) {
            System.out.print(c.id + " -> ");
        }

        System.out.println("END");
    }
}