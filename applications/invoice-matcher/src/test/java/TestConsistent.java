import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class TestConsistent {
    public static void main(String[] args) {
        try {
            List<CSVRecord> records = CSVParser.parse(new FileReader("applications/invoice-matcher/src/main/resources/csvtest.csv"), CSVFormat.Builder.create().setHeader().build()).getRecords();
            for (CSVRecord record : records) {
                System.out.println(record);
                System.out.println(record.isConsistent());
            }
        } catch (IOException ex) {

        }
    }
}
