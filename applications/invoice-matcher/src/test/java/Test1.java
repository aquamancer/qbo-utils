import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.aquamancer.invoicematcher.Headers.FRAGMENT;

public class Test1 {
    public static void main(String[] args) {
//        try {
//            FileReader testReader = new FileReader("/home/aqua/Downloads/asdf.txt");
//            System.out.println(testReader);
//        } catch (FileNotFoundException ex) {
//            System.out.println("File not found");
//        }
//        System.out.println(Double.parseDouble("$123.45"));
        try {
//            FileReader reader = new FileReader("applications/invoice-matcher/src/main/resources/csvtest.csv");
//            CSVParser sheet = CSVParser.parse(reader, CSVFormat.DEFAULT);
//            List<CSVRecord> records = sheet.getRecords();

            FileReader reader1 = new FileReader("applications/invoice-matcher/src/main/resources/Accounts Receivable/shortfragment.csv");
            CSVParser sheet1 = CSVParser.parse(reader1, CSVFormat.Builder.create().setHeader().build());
            Function<CSVRecord, List<String>> groupBy = record -> List.of(record.get(FRAGMENT.get("invoiceNumber")), record.get(FRAGMENT.get("eftTraceNumber")), record.get(FRAGMENT.get("paymentDate")), record.get(FRAGMENT.get("eftAmount")));
            printDuplicates(groupDuplicates1(sheet1.getRecords(), groupBy));
        } catch (FileNotFoundException ex) {
            System.out.println("File not found.");
        } catch (IOException ex) {

        }
    }
    private static Map<List<String>, List<CSVRecord>> groupDuplicates(List<CSVRecord> fragments) {
        Map<List<String>, List<CSVRecord>> duplicates = fragments
                .stream()
                .collect(Collectors.groupingBy(
                        record -> Arrays.asList(record.values())
                ));
        return duplicates;
    }
    private static Map<List<String>, List<CSVRecord>> groupDuplicates1(List<CSVRecord> fragments, Function<CSVRecord, List<String>> groupBy) {
        Map<List<String>, List<CSVRecord>> duplicates = fragments
                .stream()
                .collect(Collectors.groupingBy(
                        groupBy
                ));
        return duplicates;
    }
    private static void printDuplicates(Map<List<String>, List<CSVRecord>> map) {
        for (Map.Entry<List<String>, List<CSVRecord>> entry : map.entrySet()) {
            System.out.println("---------------------------");
            System.out.println(entry.getKey());
            for (CSVRecord fragment : entry.getValue()) {
                System.out.println(fragment);
            }
        }
    }
}
