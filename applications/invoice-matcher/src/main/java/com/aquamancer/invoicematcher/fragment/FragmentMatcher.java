package com.aquamancer.invoicematcher.fragment;

import com.aquamancer.invoicematcher.BankDepositParser;
import com.aquamancer.invoicematcher.Headers;
import com.aquamancer.invoicematcher.SumCombinations;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.AbstractMap.SimpleEntry;


public class FragmentMatcher {
    private static final Logger LOGGER = LogManager.getLogger(FragmentMatcher.class);
    public static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH);
    public static final Function<CSVRecord, List<String>> GROUP_DUPLICATES = record -> List.of(record.get(Headers.FRAGMENT.get("invoiceNumber")), record.get(Headers.FRAGMENT.get("eftTraceNumber")), record.get(Headers.FRAGMENT.get("paymentDate")), record.get(Headers.FRAGMENT.get("eftAmount")));
    public static Match calculateFragmentMatches(CSVRecord bankDeposit, List<CSVRecord> fragmentList) {
        double target = BankDepositParser.parseAmount(bankDeposit, Headers.BANK.get("receivedAmount"));
        List<Map.Entry<MatchMethod, Supplier<List<CSVRecord>>>> matchMethods = new ArrayList<>();

        // Get list of fragments with the target date
        LocalDate bankDepositDate = LocalDate.parse(bankDeposit.get(Headers.BANK.get("date")), BankDepositParser.DATE_FORMAT);
        List<CSVRecord> sameDate = getFragmentsSameDate(fragmentList, bankDepositDate, DATE_FORMAT);
        // Get list of fragment groups with same eft trace number from sameDate
        Function<CSVRecord, String> groupByEftTrace = record -> record.get(Headers.FRAGMENT.get("eftTraceNumber"));
        Map<String, List<CSVRecord>> groupedByEft = groupFragments(sameDate, groupByEftTrace);
        List<List<CSVRecord>> sameDateEftTrace = fragmentsMapToList(groupedByEft);

        // Gather match methods
        matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_ALL, () -> Methods.entireGroup(sameDate, target)));
        matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_HALF, () -> Methods.halfDuplicates(sameDate, GROUP_DUPLICATES, target)));
        matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_NONE, () -> Methods.noDuplicates(sameDate, GROUP_DUPLICATES, target)));
        for (List<CSVRecord> eftTraceGroup : sameDateEftTrace) {
            matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_EFT_ALL, () -> Methods.entireGroup(eftTraceGroup, target)));
            matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_EFT_HALF, () -> Methods.halfDuplicates(eftTraceGroup, GROUP_DUPLICATES, target)));
            matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_EFT_NONE, () -> Methods.noDuplicates(eftTraceGroup, GROUP_DUPLICATES, target)));
        }
        // Sum combinations methods
        matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_SUM_ALL, () -> Methods.sumCombinations(sameDate, GROUP_DUPLICATES, target)));
        matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_SUM_HALF, () -> Methods.sumCombinations(getHalfDuplicates(sameDate, GROUP_DUPLICATES), GROUP_DUPLICATES, target)));
        matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_SUM_NONE, () -> Methods.sumCombinations(getNoDuplicates(sameDate, GROUP_DUPLICATES), GROUP_DUPLICATES, target)));
        for (List<CSVRecord> eftTraceGroup : sameDateEftTrace) {
            matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_EFT_SUM_ALL, () -> Methods.sumCombinations(eftTraceGroup, GROUP_DUPLICATES, target)));
            matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_EFT_SUM_HALF, () -> Methods.sumCombinations(getHalfDuplicates(eftTraceGroup, GROUP_DUPLICATES), GROUP_DUPLICATES, target)));
            matchMethods.add(new SimpleEntry<>(MatchMethod.DATE_EFT_SUM_NONE, () -> Methods.sumCombinations(getNoDuplicates(eftTraceGroup, GROUP_DUPLICATES), GROUP_DUPLICATES, target)));
        }

        // Iterate through each operation done and check if there is a match.
        for (Map.Entry<MatchMethod, Supplier<List<CSVRecord>>> matchAttempt : matchMethods) {
            MatchMethod method = matchAttempt.getKey();
            Supplier<List<CSVRecord>> operation = matchAttempt.getValue();
            List<CSVRecord> potentialMatch = operation.get();
            if (!potentialMatch.isEmpty()) {
                return new Match(potentialMatch, bankDeposit, method);
            }
        }
        // There was no match
        return new Match(bankDeposit, MatchMethod.NO_MATCH);
    }

    public static class Methods {
        public static List<CSVRecord> entireGroup(List<?> fragments, double target) {
            List<CSVRecord> allFragments = extractAll(fragments);
            return areEqual(sum(allFragments), target) ? allFragments : List.of();
        }

        public static List<CSVRecord> halfDuplicates(List<CSVRecord> fragments, Function<CSVRecord, List<String>> isDuplicate, double target) {
            List<CSVRecord> halfDuplicates = getHalfDuplicates(fragments, isDuplicate);
            return areEqual(sum(halfDuplicates), target) ? halfDuplicates : List.of();
        }

        public static List<CSVRecord> noDuplicates(List<CSVRecord> fragments, Function<CSVRecord, List<String>> isDuplicate, double target) {
            List<CSVRecord> noDuplicates = getNoDuplicates(fragments, isDuplicate);
            return areEqual(sum(noDuplicates), target) ? noDuplicates : List.of();
        }

        public static List<CSVRecord> sumCombinations(List<CSVRecord> fragments, Function<CSVRecord, List<String>> isDuplicate, double target) {
            if (fragments.size() > 16) {
                return List.of();
            }
            // Map<sum amount, list of combinations that add up to sum amount>
            // combinations = list of CSVRecords
            Map<Double, List<List<CSVRecord>>> sumCombinations = SumCombinations.sumCombinationsFragments(fragments);
            // If there are combinations that add up to target, and the list of combinations that add up to
            // target is not empty~
            if (sumCombinations.containsKey(target) && !sumCombinations.get(target).isEmpty()) {
                List<List<CSVRecord>> combinationMatches = sumCombinations.get(target);
                // If there is only one combination that adds up to the target, return it.
                if (combinationMatches.size() == 1) return combinationMatches.getFirst();

                // If there are multiple combinations that add up to the target, return the combination
                // that includes the least amount of duplicates.

                // Combinations that contain the least amount of duplicates
                List<List<CSVRecord>> leastDuplicates = new ArrayList<>();
                int currentNumLeastDuplicates = Integer.MAX_VALUE;
                for (List<CSVRecord> combination : combinationMatches) {
                    Collection<List<CSVRecord>> duplicateGroups = groupFragments(combination, isDuplicate).values();
                    // Count the amount of duplicates per group
                    int numDuplicates = 0;
                    for (List<CSVRecord> duplicateGroup : duplicateGroups) {
                        if (duplicateGroup.size() > 1) {
                            numDuplicates += duplicateGroup.size() - 1;
                        }
                    }
                    // If leastDuplicates is empty or this combination has the same amount of duplicates, add to list
                    if (leastDuplicates.isEmpty() || numDuplicates == currentNumLeastDuplicates) {
                        leastDuplicates.add(combination);
                        currentNumLeastDuplicates = numDuplicates; // In case leastDuplicates.isEmpty()
                    } else if (numDuplicates < currentNumLeastDuplicates) {
                        // Clear leastDuplicates, and add combination.
                        leastDuplicates = new ArrayList<>();
                        leastDuplicates.add(combination);
                        currentNumLeastDuplicates = numDuplicates;
                    }
                }
                // Just return the first combination with the least duplicates
                // Could handle it differently if there are multiple combinations with the same amount of duplicates.
                return leastDuplicates.getFirst();
            } else {
                return List.of();
            }
        }
    }
    public static List<CSVRecord> getHalfDuplicates(List<CSVRecord> fragments, Function<CSVRecord, List<String>> isDuplicate) {
        Map<List<String>, List<CSVRecord>> duplicateGroups = groupFragments(fragments, isDuplicate);
        List<CSVRecord> result = new ArrayList<>();
        for (List<CSVRecord> duplicateGroup : duplicateGroups.values()) {
            for (int i = 0; i < duplicateGroup.size() / 2; i++) {
                result.add(duplicateGroup.get(i));
            }
        }
        return result;
    }
    public static List<CSVRecord> getNoDuplicates(List<CSVRecord> fragments, Function<CSVRecord, List<String>> isDuplicate) {
        Map<List<String>, List<CSVRecord>> duplicateGroups = groupFragments(fragments, isDuplicate);
        List<CSVRecord> result = new ArrayList<>();
        for (List<CSVRecord> duplicateGroup : duplicateGroups.values()) {
            result.add(duplicateGroup.getFirst());
        }
        return result;
    }
    private static double sum(List<CSVRecord> fragments) {
        double sum = 0;
        for (CSVRecord fragment : fragments) {
            try {
                sum += Double.parseDouble(fragment.get(Headers.FRAGMENT.get("eftAmount")));
            } catch (NumberFormatException ex) {
                LOGGER.error("Could not parse Check EFT Amount from: {}", fragment.toString());
            }
        }
        return sum;
    }
    private static boolean areEqual(double a, double b) {
        double tolerance = 1e-6;
        return Math.abs(a - b) < tolerance;
    }

    private static <K, V> Map<K, List<V>> groupFragments(List<V> fragments, Function<V, K> groupBy) {
        return fragments.stream().collect(Collectors.groupingBy(groupBy));
    }

    private static List<CSVRecord> getFragmentsSameDate(List<CSVRecord> fragments, LocalDate targetDate, DateTimeFormatter fragmentDateFormat) {
        return fragments.stream()
                .filter(record -> {
                    boolean dontFilter;
                    try {
                        dontFilter = !record.get(Headers.FRAGMENT.get("eftAmount")).isBlank()
                                && !record.get(Headers.FRAGMENT.get("paymentDate")).isBlank()
                                && targetDate.isEqual(LocalDate.parse(record.get(Headers.FRAGMENT.get("paymentDate")), fragmentDateFormat));
                    } catch (DateTimeParseException ex) {
                        LOGGER.warn("Error parsing date for fragment{}, ", record);
                        return false;
                    }
                    return dontFilter;
                })
                .toList();

    }

    private static <K, V> List<List<V>> fragmentsMapToList(Map<K, List<V>> groupedFragments) {
        return groupedFragments
                .values()
                .stream()
                .toList();
    }


    /**
     * Extracts all elements from the lowest nested List.
     * @requires All elements of the same type.
     * @return
     * @param <T>
     */
    public static <T> List<T> extractAll(Collection<?> collection) {
        List<T> result = new ArrayList<>();

        for (Object element : collection) {
            if (element instanceof Collection) {
                // Recursive call for nested collections
                result.addAll(extractAll((Collection<?>) element));
            } else {
                // Safe cast because we assume all elements are of type T
                result.add((T) element);
            }
        }

        return result;
    }
}
