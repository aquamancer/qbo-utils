package com.aquamancer.invoicematcher;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SumCombinations {
    private static final Logger LOGGER = LogManager.getLogger(SumCombinations.class);

    public static Map<Double, List<List<Integer>>> sumCombinations(List<Double> numbers) {
        Map<Double, List<List<Integer>>> result = new HashMap<>();

        // Get all combinations of indices
        int n = numbers.size();
        int totalCombinations = (1 << n); // 2^n combinations
        LOGGER.info("Generating all combinations of sums, n = {}", totalCombinations);

        for (int i = 1; i < totalCombinations; i++) { // Skip the empty combination
            List<Integer> indices = new ArrayList<>();
            double sum = 0;

            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) { // If the jth bit is set
                    indices.add(j);
                    sum += numbers.get(j);
                }
            }

            // Store the sum and indices in the map
            result.computeIfAbsent(Math.round(sum*1e7) / 1e7, k -> new ArrayList<>()).add(indices);
        }

        return result;
    }
    public static Map<Double, List<List<CSVRecord>>> sumCombinationsFragments(List<CSVRecord> fragments) {
        Map<Double, List<List<CSVRecord>>> result = new HashMap<>();

        // Get all combinations of indices
        int n = fragments.size();
        int totalCombinations = (1 << n); // 2^n combinations
        LOGGER.info("Generating all combinations of sums, n = {}. i = {}", n, totalCombinations);

        for (int i = 1; i < totalCombinations; i++) { // Skip the empty combination
            List<CSVRecord> indices = new ArrayList<>();
            double sum = 0;

            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) { // If the jth bit is set
                    indices.add(fragments.get(j));
                    sum += Double.parseDouble(fragments.get(j).get(Headers.FRAGMENT.get("eftAmount")));
                }
            }

            // Store the sum and indices in the map
            result.computeIfAbsent(Math.round(sum*1e7) / 1e7, k -> new ArrayList<>()).add(indices);
        }

        return result;
    }

    public static Map<Double, List<List<Integer>>> sumCombinationsFragmentsIndices(List<CSVRecord> fragments) {
        Map<Double, List<List<Integer>>> result = new HashMap<>();

        // Get all combinations of indices
        int n = fragments.size();
        int totalCombinations = (1 << n); // 2^n combinations
        LOGGER.info("Generating all combinations of sums, n = {}", totalCombinations);

        for (int i = 1; i < totalCombinations; i++) { // Skip the empty combination
            List<Integer> indices = new ArrayList<>();
            double sum = 0;

            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) { // If the jth bit is set
                    indices.add(j);
                    sum += Double.parseDouble(fragments.get(j).get(Headers.FRAGMENT.get("eftAmount")));
                }
            }

            // Store the sum and indices in the map
            result.computeIfAbsent(Math.round(sum*1e7) / 1e7, k -> new ArrayList<>()).add(indices);
        }

        return result;
    }
//    public static void main(String[] args) {
//        List<Double> numbers = Arrays.asList(-2.0, 1.0, 2.0, 3.0, 100.0, 24.4, 13.7, 88.88, 123049.23, 2224.12, -123.21);
//        Map<Double, List<List<Integer>>> combinations = sumCombinations(numbers);
//
//        for (Map.Entry<Double, List<List<Integer>>> entry : combinations.entrySet()) {
//            System.out.println("Sum: " + entry.getKey() + ", Indices: " + entry.getValue());
//        }
//    }
}
