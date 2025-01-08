import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class Test4 {
    public static void main(String[] args) {
        String date = "25-NOV-24";
        DateTimeFormatter format = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yy").toFormatter(Locale.ENGLISH);
        System.out.println(LocalDate.parse(date, format));
    }

}
