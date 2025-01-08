import com.aquamancer.invoicematcher.Headers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Test3 {
    private static final Logger LOGGER = LogManager.getLogger(Test3.class);
    public static void main(String[] args) {
        try {
            FileReader reader1 = new FileReader("applications/invoice-matcher/src/main/resources/Accounts Receivable/shortfragment.csv");
            CSVParser sheet1 = CSVParser.parse(reader1, CSVFormat.Builder.create().setHeader().build());
            List<CSVRecord> fragments = sheet1.getRecords();

            // Create a List of grouped fragments with the same invoice number.
            Function<CSVRecord, String> groupSameInvoiceNumber = record -> record.get(Headers.FRAGMENT.get("invoiceNumber"));
            List<List<CSVRecord>> invoiceGroups = fragmentsMapToList(groupFragments(fragments, groupSameInvoiceNumber));
            // Create a List of invoice groups that are divided up by duplicate.
            Function<List<CSVRecord>, String> groupByInvoiceNumber = invoiceGroup -> invoiceGroup.getFirst().get(Headers.FRAGMENT.get("invoiceNumber"));
            List<List<List<CSVRecord>>> invoiceAndDuplicateGroups = fragmentsMapToList(groupFragments(invoiceGroups, groupByInvoiceNumber));
            print3d(invoiceAndDuplicateGroups);
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        }
    }
    private static void print3d(List<List<List<CSVRecord>>> list) {
        for (List<List<CSVRecord>> invoiceGroup : list) {
            System.out.println("--------------- invoice " + invoiceGroup.getFirst().getFirst().get(Headers.FRAGMENT.get("invoiceNumber")));
            for (List<CSVRecord> duplicateGroup : invoiceGroup) {
                System.out.println("********** duplicate group ***********");
                for (CSVRecord fragment : duplicateGroup) {
                    System.out.println(fragment);
                }
            }
        }

    }
    private static double sumFragments(List<CSVRecord> fragments) {
        double sum = 0;
        for (CSVRecord fragment : fragments) {
            try {
                sum += Double.parseDouble(fragment.get(Headers.FRAGMENT.get("eftAmount")));
            } catch (NumberFormatException ex) {
                LOGGER.error("Could not parse Check EFT Amount from: {}", fragment.toString());
            }
        }
        LOGGER.info("Fragments sum result: {}", sum);
        return sum;
    }
    private static boolean areEqual(double a, double b) {
        double tolerance = 1e-6;
        return Math.abs(a - b) < tolerance;
    }

    private static <K, V> Map<K, List<V>> groupFragments(List<V> fragments, Function<V, K> groupBy) {
        return fragments.stream().collect(Collectors.groupingBy(groupBy));
    }


    private static <K, V> List<List<V>> fragmentsMapToList(Map<K, List<V>> groupedFragments) {
        return groupedFragments
                .values()
                .stream()
                .toList();
    }
}
