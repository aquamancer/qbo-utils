package com.aquamancer.payrollscript;

import com.aquamancer.csv.CsvToMaps;
import com.aquamancer.payrollscript.payperiod.PayPeriod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.AbstractMap;
import java.util.stream.Stream;

import static com.aquamancer.qbo.QboActions.pressControlA;
import static org.apache.logging.log4j.Level.*;

public class Qbo {
    private static final String OPERATING_SYSTEM = System.getProperty("os.name");
    private static final Logger LOGGER = LogManager.getLogger(Qbo.class);
    
    public static WebDriver driver = new ChromeDriver();
    
    static String PREFERENCES_FILENAME = "payroll-script-preferences.json";
    static String ELEMENT_CONFIG_FILENAME = "element-config.json";
    static String COLUMN_REFERENCES_FILENAME = "column-references.json";
    static JsonObject preferences, elementConfig, columnReferences;
    static String jarPath, spreadsheetPath;
    static boolean usingManualFilePath; // updated via getSpreadsheetPath
    static PayPeriod payPeriod;

    static String HEADER_TABLE_IDENTIFIER = "Name (";
    static String MAIN_TABLE_IDENTIFIER = "Hourly";
    
    static List<String> COLUMNS_TO_CLEAR = List.of("regularPay", "fringeHW", "overtime", "sickPay", "vacationPay", "holidayPay");

    /**
     * Main method - code execution point.
     * @param args Command line args - none.
     */
    public static void main(String[] args) {
        // Determine the path of the .jar and load .json files.
        try {
            LOGGER.debug("Initializing .jar path and .json files.");
            jarPath = new File(Qbo.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
            preferences = JsonParser.parseReader(new FileReader(jarPath + "/" + PREFERENCES_FILENAME)).getAsJsonObject();
            elementConfig = JsonParser.parseReader(new FileReader(jarPath + "/" + ELEMENT_CONFIG_FILENAME)).getAsJsonObject();
            columnReferences = JsonParser.parseReader(new FileReader(jarPath + "/" + COLUMN_REFERENCES_FILENAME)).getAsJsonObject();
        } catch (URISyntaxException | FileNotFoundException e) {
            LOGGER.log(FATAL, "Cannot locate a file from the following: {}, {}, or {}", PREFERENCES_FILENAME, ELEMENT_CONFIG_FILENAME, COLUMN_REFERENCES_FILENAME);
            throw new RuntimeException(String.format("Cannot locate a file from the following: %s, %s, or %s", PREFERENCES_FILENAME, ELEMENT_CONFIG_FILENAME, COLUMN_REFERENCES_FILENAME));
        }

        // Calculate the path of the spreadsheet
        // todo fix this. should only instantiate PayPeriod if using automatic file name
        payPeriod = new PayPeriod();
        updateSpreadsheetPath(preferences);
        // Band-aid solution to make sure that payPeriod does not get used in case manual file path is being used.
        if (usingManualFilePath)
            payPeriod = null;
        // Maximize the window
        driver.manage().window().maximize();
        // Navigate browser to payroll url
        LOGGER.log(DEBUG, "Navigating to: " + elementConfig.get("payroll").getAsJsonObject().get("url").getAsString());
        driver.navigate().to(elementConfig.get("payroll").getAsJsonObject().get("url").getAsString());
        // Wait for the payroll screen to fully loaded with the correct heading elements.
        waitForPayrollScreen(new WebDriverWait(driver, Duration.ofSeconds(600)));
        // Verify that the correct pay period is selected in qbo.
        if (!usingManualFilePath)
            verifyCorrectPayPeriodInQbo(driver);
        // Fill payroll table.
        fillTable(spreadsheetPath);
    }
    
    /**
     * Looks up the value for the column name represented in quickbooks or the spreadsheet.<br>
     * The format of each column header is jsonKey.(spreadsheet/quickbooks).name = "value".
     * @param jsonKey the root key for a column header in columnReferences. e.g. "employeeName".
     * @param quickbooksOrSpreadsheet "quickbooks" or "spreadsheet", depending on if you want to look up the quickbooks or spreadsheet column name for jsonKey.
     * @param columnReferences JsonObject of column-references.json.
     * @return The name of what quickbooks or the spreadsheet calls the column.
     */
    private static String getJsonColumnName(String jsonKey, String quickbooksOrSpreadsheet, JsonObject columnReferences) {
        return columnReferences.get(jsonKey).getAsJsonObject().get(quickbooksOrSpreadsheet).getAsJsonObject().get("name").getAsString();
    }

    /**
     * Reads preferences(.json) to get the absolute path to spreadsheet. If "absoluteManualPathToSpreadsheet" is an empty string, it will use the folder path provided in "absolutePathToSpreadsheetFolder" concatenated
     * with the auto-generated spreadsheet name based on time of execution.
     * @param preferences preferences.json.
     * @return The absolute path of the spreadsheet.
     */
    private static void updateSpreadsheetPath(JsonObject preferences) {
        if (!preferences.get("absoluteManualPathToSpreadsheet").getAsString().isEmpty()) {
            // manual file path
            spreadsheetPath = preferences.get("absoluteManualPathToSpreadsheet").getAsString();
            LOGGER.info("Manual spreadsheet override detected. Using file: {}", spreadsheetPath);
            usingManualFilePath = true;
        } else {
            // absolute path to folder + auto-generated file name
            spreadsheetPath = preferences.get("absolutePathToSpreadsheetFolder").getAsString() + "/" + payPeriod.getPayrollSpreadsheetName();
            LOGGER.info("Manual spreadsheet override is blank. Using provided folder path and auto-generated spreadsheet file name: {}", spreadsheetPath);
            usingManualFilePath = false;
        }
    }
    
    /**
     * Waits for the payroll screen to "isDisplayed()" two &lt;tbody&gt;'s. When these two &lt;tbody&gt;'s
     * are isDisplayed(), the wait ends.<br>
     * 
     * As of 11/23/2024, there are two &lt;tbody&gt; elements. The first one is the row of table headers, e.g.
     * "Name (x of n)", "Salary". <br>
     * The second &lt;tbody&gt; element is the main table below the headers.<br>
     * 
     * This method waits for the two &lt;tbody&gt; elements, and verifies that 
     * <ul>
     *  <li>one &lt;tbody&gt; element contains the string: "Name (".</li>
     *  <li>another &lt;tbody&gt; element contains the string: "Hourly"</li>
     * <br>
     * @param wait
     */
    private static void waitForPayrollScreen(WebDriverWait wait) {
        LOGGER.log(INFO, "Waiting for payroll screen tbodies(10 minutes until timeout)...");
        
        wait.until(d -> {
            List<WebElement> tbodies = d.findElements(By.cssSelector("tbody"));
            if (tbodies.size() > 2) LOGGER.log(WARN, "Found tbodies: " + tbodies.size() + ", Expected: 2");

            boolean headerFound = false;
            boolean mainTableFound = false;
            for (int i = 0; i < tbodies.size(); i++) {
                try {
                    if (tbodies.get(i).getText().contains(HEADER_TABLE_IDENTIFIER)) {
                        LOGGER.debug("Located header tbody ({}/2)", mainTableFound ? 2 : 1);
                        headerFound = true;
                    } else if (tbodies.get(i).getText().contains(MAIN_TABLE_IDENTIFIER)) {
                        LOGGER.debug("Located main tbody ({}/2)", headerFound ? 2 : 1);
                        mainTableFound = true;
                    }
                } catch (StaleElementReferenceException ex) {
                    LOGGER.warn("Stale element exception whilst locating tbodies. Retrying...");
                    tbodies = d.findElements(By.cssSelector("tbody"));
                    i = 0;
                    headerFound = false;
                    mainTableFound = false;
                }
            }
            return headerFound && mainTableFound;
        });
        LOGGER.info("Both tbodies were found. Wait has concluded.");
    }

    /**
     * If the pay period shown in qbo is not correct, according to correctPayPeriodInQbo, prompts console input to
     * proceed anyway.
     * @param driver The WebDriver that represents the qbo payroll screen.
     */
    private static void verifyCorrectPayPeriodInQbo(WebDriver driver) {
        if (!correctPayPeriodInQbo(driver)) {
            Scanner input = new Scanner(System.in);
            System.out.println("The pay period displayed on qbo does not match the pay period at time of program execution - 1! Proceed anyway? (Y/n)");
            if (!input.nextLine().equalsIgnoreCase("y"))
                System.exit(1);
        }
    }

    /**
     * Locates the combobox that displays the pay period window in the format "dd/MM/yyyy to dd/MM/yyyy",
     * extracts the second date of that string (the end date), and verifies that it matches with the pay period end
     * date of the pay period that is one before the pay period of the program execution date.<br>
     * Therefore, this should only be called if the payroll being entered is for the pay period 1 before current.
     * @param driver The driver that represents the payroll screen.
     * @return true if the pay period in qbo matches the automatic pay period. false if otherwise.
     */
    private static boolean correctPayPeriodInQbo(WebDriver driver) {
        LocalDate endOfPayPeriod = payPeriod.getEndOfPayPeriod();
        // Find "combobox" that displays the pay period date in format "dd/MM/yyyy to dd/MM/yyyy"
        // and extract the value from the attribute "value".
        List<String> candidates = driver.findElements(By.cssSelector("input[aria-label=\"Select a pay period\"]")).stream().map(element -> element.getAttribute("value")).toList();
        String displayedPayPeriod;
        if (candidates.isEmpty()) {
            LOGGER.error("Could not locate the element in qbo that displays what pay period is being inputted!");
            return false;
        } else if (candidates.size() > 1) {
            // In the rare occurrence that there are multiple elements with "Select a pay period",
            // filter down the candidates by a series of checks of the label contents.
            List<String> filteredCandidates = candidates.stream().filter(candidate -> candidate.contains(" to ")).toList();
            if (filteredCandidates.size() != 1) {
                return false;
            } else {
                displayedPayPeriod = filteredCandidates.getFirst();
            }
        } else {
            displayedPayPeriod = candidates.getFirst();
        }

        // Extract the pay period ending date from "dd/MM/yyyy to dd/MM/yyyy"
        LOGGER.debug("Detected displayed pay period in qbo to be: {}", displayedPayPeriod);
        String[] parts = displayedPayPeriod.split(" ");
        if (parts.length != 3) {
            LOGGER.error("{}, split by ' ' should be of length 3, not length {}. Pay period check been terminated.", displayedPayPeriod, parts.length);
            return false;
        } else {
            LOGGER.debug("Comparing end of pay period displayed in qbo: {} to expected end of pay period based on program execution time: {}", parts[2], endOfPayPeriod.toString());
            int[] ddMMyyyy = Arrays.stream(parts[2].split("/")).mapToInt(component -> Integer.parseInt(component)).toArray();
            return ddMMyyyy[0] == endOfPayPeriod.getMonthValue()
                    && ddMMyyyy[1] == endOfPayPeriod.getDayOfMonth()
                    && ddMMyyyy[2] == endOfPayPeriod.getYear();
        }
    }

    /**
     * Searches for tbody elements, and identifies which tbody represents the table headers by searching for a
     * tbody.getText.contains(HEADER_IDENTIFIER). <br>
     * 
     * Once the header tbody has been located, the text is extracted and split("\n"). Then, each element in the
     * returned array represents the text of a single column header, in order, because split() maintains order of
     * appearance. <br>
     * 
     * This array is then converted to a Map&lt;header name, columnNumber&gt; and returned.
     * 
     * @param driver the WebDriver to use to search for tbody elements.
     * @return a Map&lt;header name, column number&gt; for the table header row. Header name is the name that appears
     * in qbo, not on the spreadsheet.
     */
    private static Map<String, Integer> getQboColumnHeaders(WebDriver driver) {
        LOGGER.info("Parsing positions of each column. Setting zoom to 25%.");
        
        ((JavascriptExecutor) driver).executeScript("document.body.style.zoom='25%'");
        // maybe add wait here
        
        // get List<WebElement> via findElements -> Stream<WebElement> -> Stream<String> via getText()
        // -> filter the stream to Strings that contain HEADER_IDENTIFIER -> convert to List.
        List<String> headerTextCandidates = driver.findElements(By.cssSelector("tbody")).stream().map(WebElement::getText).filter(s -> s.contains(HEADER_TABLE_IDENTIFIER)).toList();

        if (headerTextCandidates.size() != 1) {
            LOGGER.fatal("Found multiple <tbody> elements that contains(): {}", HEADER_TABLE_IDENTIFIER);
            throw new RuntimeException(String.format("Found multiple <tbody> elements that contains(): %s", HEADER_TABLE_IDENTIFIER));
        }
        // headerTextCandidates is guaranteed to be of size 1 here.
        // Each header text is separated by a newline.
        List<String> headers = Arrays.stream(headerTextCandidates.get(0).split("\n")).toList();
        // Check if each column defined in column-references.json is present.
        if (!allHeadersInList(headers, columnReferences)) {
            throw new RuntimeException("Required column header was not found.");
        }
        // Restore the default zoom.
        ((JavascriptExecutor) driver).executeScript("document.body.style.zoom=''");
        
        return convertListToMapWithIndex(headers, 1);
    }

    /**
     * Checks if all values in column-references.json are present in headersFound, except for Name.
     * @param headersFound a List of headers that were found in the payroll screen.
     * @param columnReferences asMap() of column-references.json.
     * @return true if all values in column-references.json are present in headersFound. false if one is not in headersFound.
     */
    private static boolean allHeadersInList(List<String> headersFound, JsonObject columnReferences) {
        AtomicReference<Boolean> allHeadersInList = new AtomicReference<>(true);
        Map<String, JsonElement> columns = columnReferences.asMap();
        columns.forEach((String key, JsonElement value) -> {
            String requiredName = value.getAsJsonObject().get("quickbooks").getAsJsonObject().get("name").getAsString();
            if (headersFound.stream().noneMatch(header -> header.contains(requiredName))) {
                LOGGER.fatal("The required column header: {} could not be located!", requiredName);
                allHeadersInList.set(false);
            }
        });

        return allHeadersInList.get();
    }

    /**
     * Converts a List into a Map of &lt;List element, element index + offset&gt;.
     * @param list A List that is presumably in the correct order you want.
     * @param offset What constant to add to each element index.
     * @return A Map of the List elements with their corresponding indices.
     */
    private static Map<String, Integer> convertListToMapWithIndex(List<String> list, int offset) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i), i + offset);
        }
        return map;
    }

    /**
     * Uses driver to search for all the current &lt;tbody&gt; elements, and returns the first one that contains() MAIN_TABLE_IDENTIFIER.
     * @param driver The WebDriver to search for tbody elements.
     * @return WebElement of the first tbody element whose getText() contains() MAIN_TABLE_IDENTIFIER.
     */
    private static WebElement getMainTableElement(WebDriver driver) {
        List<WebElement> tbodies = driver.findElements(By.cssSelector("tbody"));
        for (WebElement tbody : tbodies) {
            if (tbody.getText().contains(MAIN_TABLE_IDENTIFIER))
                return tbody;
        }
        LOGGER.error("Could not find the main table tbody using the identifier: {}", MAIN_TABLE_IDENTIFIER);
        throw new RuntimeException(String.format("Could not find the main table tbody using the identifier: %s", MAIN_TABLE_IDENTIFIER));
    }

    /**
     * Fills the Quickbooks payroll table with the spreadsheet data defined by spreadsheetPath.
     * @param spreadsheetPath The spreadsheet to obtain the data to be inputted.
     */
    public static void fillTable(String spreadsheetPath) {
        LOGGER.info("Filling table.");
        Map<String, Integer> qboColumnHeaders = getQboColumnHeaders(driver);
        Map<String, Map.Entry<Integer, JsonElement>> columnPositions = getColumnPositions(qboColumnHeaders, columnReferences); 
        
        WebElement table = getMainTableElement(driver);
        List<WebElement> rows = table.findElements(By.cssSelector("tr"));
        
        List<Map<String, String>> spreadsheetData;
        List<Employee> spreadsheetEmployeeList;
        spreadsheetData = CsvToMaps.getData(spreadsheetPath, true);
        // todo make this a hashmap of <name, data>
        spreadsheetEmployeeList = Employee.getEmployeeList(spreadsheetData);
        
        List<String> namesOnQboNotInputted = new ArrayList<>(0);
        
        // looping through each row in qbo
        for (int i = 0; i < rows.size(); i++) {
            try {
                rows.get(i).click();
                // reinitialize elements/data
                table = getMainTableElement(driver);
                rows = table.findElements(By.cssSelector("tr"));
                List<WebElement> tds = rows.get(i).findElements(By.cssSelector("td"));
                
                // Clear all columns defined in COLUMNS_TO_CLEAR
                LOGGER.debug("Clearing row: {}", i);
                for (String columnToClear : COLUMNS_TO_CLEAR) {
                    Integer positionOfColumnToClear = columnPositions.get(columnToClear).getKey();
                    try {
                        WebElement cellToClear = tds.get(positionOfColumnToClear).findElement(By.cssSelector("input"));
                        pressControlA(cellToClear, OPERATING_SYSTEM);
                        cellToClear.sendKeys("0");
                    } catch (Exception ex) {}
                }
                // for all rows, loop through every employee in the arraylist/spreadsheet
                boolean dataInputtedForThisRow = false;
                for (Employee employee : spreadsheetEmployeeList) {
                    // If the employee on the spreadsheet is the same name as one appearing on this qbo row
                    String employeeNameInSpreadsheet = employee.data.get(getJsonColumnName("employeeName", "spreadsheet", columnReferences));
                    String nameOnCurrentRow = tds.get(columnPositions.get("employeeName").getKey()).findElement(By.cssSelector("button")).getText();
                    if (employeeNameInSpreadsheet.equalsIgnoreCase(nameOnCurrentRow)) {
                        //employees.get(j).inputted = true; // this is the optimal location to put this but it feels bad to put inputted before doing anything
                        LOGGER.debug("Found employee name match for row {}. Name on spreadsheet: {}. Name displayed on quickbooks: {}", i, employeeNameInSpreadsheet, nameOnCurrentRow);
                        fillRow(tds, employee, columnPositions, getPayType(rows.get(i), employee, columnPositions));
                        employee.inputted = true;
                        dataInputtedForThisRow = true;
                    }
                }
                // Add the name of the employee on Qbo that was not added.
                if (!dataInputtedForThisRow) {
                    LOGGER.warn("Employee on Qbo that was not on spreadsheet: {}", tds.get(columnPositions.get("employeeName").getKey()).getText());
                    namesOnQboNotInputted.add(tds.get(columnPositions.get("employeeName").getKey()).findElement(By.cssSelector("button")).getText());
                }
            } catch (StaleElementReferenceException ex) {
                //refresh the elements upon error: element went "stale" (properties changed), then i -= 1 to restart the line
                LOGGER.warn("Stale element reference exception. Restarting line...");
                i -= 1;
            } catch (ElementClickInterceptedException ex) {
                // error occurs when element is off-screen. to fix, uses a javascript script to scroll, then i -= 1 to restart the line
                // WebElement scrollTo = driver.findElement(By.xpath(tablePath + "/tr[" + (i + 1) + "]/td[1]/label/span/input"));
                LOGGER.warn("ElementClickInterceptedException. Scrolling and restarting row...");
                WebElement scrollTo = rows.get(i + 1);
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", scrollTo);
                i -= 1;
            } catch (NoSuchElementException ex) {
                // this error should only occur if a text box does not exist in qbo but there is a value that needs to be inputted
                System.out.println("NoSuchElementException: there should be a text box/element but there isn't any. exiting. Likely caused if there is a value in the spreadsheet without a corresponding text box in qbo. This error occured at qbo line: " + (i + 1));
                ex.printStackTrace();
                System.exit(-1);
            }
        }
        LOGGER.warn("Employees on qbo that were not inputted: {}", namesOnQboNotInputted);
        checkIfAllInputted(spreadsheetEmployeeList);
    }

    /**
     * Creates a Map&lt;universal column name, Pair&lt;column position in qbo, JsonElement of root child in columnReferences&gt;&gt; of each entry in columnReferences, by matching
     * each entry in columnReferences to the headers (and their positions) found in qboHeaders.
     * @param qboHeaders A Map&lt;column name described by qbo, column position in qbo&gt; for each header that appears in qbo. 
     * @param columnReferences columnReferences.json to match the universal column name to quickbooks column position by determining if the found qbo column .contains() the "name" value in columnReferences.json.
     * @return A Map&lt;universal column name, Pair&lt;column position in qbo, JsonElement of root child in columnReferences&gt;&gt; of each entry in columnReferences.
     */
    private static Map<String, Map.Entry<Integer, JsonElement>> getColumnPositions(Map<String, Integer> qboHeaders, JsonObject columnReferences) {
        Map<String, Map.Entry<Integer, JsonElement>> columnPositions = new HashMap<>();
        
        for (Map.Entry<String, JsonElement> pair : columnReferences.entrySet()) {
            String expectedQboName = getJsonColumnName(pair.getKey(), "quickbooks", columnReferences);
            Integer position = -1;
            boolean foundMatch = false;
            for (Map.Entry<String, Integer> qboHeader : qboHeaders.entrySet()) {
                if (qboHeader.getKey().contains(expectedQboName)) {
                    if (foundMatch) {
                        LOGGER.fatal("Found multiple quickbooks column candidates for {}! Check column-references.json.", expectedQboName);
                        throw new AssertionError("Found multiple quickbooks column candidates for " + expectedQboName);
                    }
                    position = qboHeader.getValue();
                    foundMatch = true;
                }
            }
            columnPositions.put(pair.getKey(), new AbstractMap.SimpleImmutableEntry<>(position, pair.getValue()));
            if (position == -1) {
                LOGGER.fatal("Could not match the expected header for {}: {} with any header displayed in Qbo!", pair.getKey(), expectedQboName);
                throw new RuntimeException(String.format("Could not match the expected header for %s: %s with any header displayed in Qbo!", pair.getKey(), expectedQboName));
            } else {
                LOGGER.debug("Matched expected header for {} in quickbooks via string: {}", pair.getKey(), expectedQboName);
            }
        }
        return columnPositions;
    }

    /**
     * Fills in the row, represented by a List&lt;WebElement&gt; of &lt;td&gt; with Employee data from employee.
     * @param tds The row to be filled, represented by a List of td elements.
     * @param employee The employee object to fill the row with.
     * @param columnPositions Positions of the universal column names that are displayed in quickbooks.
     * @param payType "SALARY" or "REGULAR" 
     */
    private static void fillRow(List<WebElement> tds, Employee employee, Map<String, Map.Entry<Integer, JsonElement>> columnPositions, String payType) {
        if (payType.equals("SALARY")) {
            // Verify that only Sick and/or Vacation pay have values in the spreadsheet
            List<String> shouldNotHaveValues = Stream.of("fringeHW", "overtime", "holidayPay").map(master -> getJsonColumnName(master, "spreadsheet", columnReferences)).toList();
            for (String spreadsheetHeader : shouldNotHaveValues) {
                if (employee.data.get(spreadsheetHeader) != null && !employee.data.get(spreadsheetHeader).isEmpty()) {
                    LOGGER.error("Salaried employee: {} was found with a value of: {} for the spreadsheet column: {}", employee.data.get(getJsonColumnName("employeeName", "spreadsheet", columnReferences)), employee.data.get(spreadsheetHeader), spreadsheetHeader);
                }
            }
            // Fill in Sick pay and Holiday pay.
            List<String> columnsToFill = List.of("sickPay", "vacationPay");
            fillCells(tds, employee, columnsToFill, columnPositions);
        } else if (payType.equals("REGULAR")) {
            List<String> columnsToFill = List.of("regularPay", "fringeHW", "overtime", "sickPay", "vacationPay", "holidayPay");
            fillCells(tds, employee, columnsToFill, columnPositions);
        }
    }

    private static void fillCells(List<WebElement> tds, Employee employee, List<String> columnsToFill, Map<String, Map.Entry<Integer, JsonElement>> columnPositions) {
        for (String columnToFill : columnsToFill) {
            if (employee.data.get(getJsonColumnName(columnToFill, "spreadsheet", columnReferences)) != null && !employee.data.get(getJsonColumnName(columnToFill, "spreadsheet", columnReferences)).isEmpty()) {
                // Get the position of the column in QBO
                Integer qboColumnPosition = columnPositions.get(columnToFill).getKey();
                /*
                    Qbo cells are represented as the xpath: tr[row]/td[col].
                    Use findElements("td") to get a list of td elements, then get the right cell by using the column
                    position as defined by columnPositions. Then we findElement("input") to get the input box
                    (as long as there is only one input box in the cell)
                */
                WebElement inputBox = tds.get(qboColumnPosition).findElement(By.cssSelector("input"));
                String valueToBeWritten = employee.data.get(getJsonColumnName(columnToFill, "spreadsheet", columnReferences));
                pressControlA(inputBox, OPERATING_SYSTEM);
                inputBox.sendKeys(valueToBeWritten);
            }
        }
    }

    /**
     * Checks both quickbooks and the spreadsheet to determine whether employee is a salaried employee or not.<br>
     * Throws if both checks contradict each other.
     * @param row The row WebElement, to grab information to determine if quickbooks represents the employee as salaried.
     * @param employee Employee object to determine if the spreadsheet represents the employee as salaried.
     * @param columnPositions Map of the positions of universal column names, to determine which column is the Name column for the quickbooks check.
     * @return "SALARY" or "REGULAR" based on quickbooks and spreadsheet checks for salary status.
     */
    private static String getPayType(WebElement row, Employee employee, Map<String, Map.Entry<Integer, JsonElement>> columnPositions) {
        boolean spreadsheetSalaried = false;
        boolean qboSalaried = false;
        // Check if spreadsheet says the employee is salaried.
        if (employee.data.get(getJsonColumnName("regularPay", "spreadsheet", columnReferences)).equals("SALARY")) {
            spreadsheetSalaried = true;
        }
        // Check if qbo says the employee is salaried.
        String NAME_COLUMN_QUERY = "/year";
        if (row.findElements(By.cssSelector("td")).get(columnPositions.get("employeeName").getKey()).getText().contains(NAME_COLUMN_QUERY)) {
            qboSalaried = true;
        }
        // Mismatch checks
        if ((spreadsheetSalaried && !qboSalaried) || (!spreadsheetSalaried && qboSalaried)) {
            LOGGER.fatal(
                    "Could not determine if employee: {} is salaried because spreadsheet says employee is salaried: {}, and qbo says employee is salaried: {}.", 
                    employee.data.get(getJsonColumnName("employeeName", "spreadsheet", columnReferences)), spreadsheetSalaried, qboSalaried
            );
            throw new RuntimeException(String.format("Could not determine if employee: %s is salaried.", getJsonColumnName("employeeName", "spreadsheet", columnReferences)));
        }
        
        if (spreadsheetSalaried && qboSalaried) {
            LOGGER.debug("Detected employee: {} as salaried.", employee.data.get(getJsonColumnName("employeeName", "spreadsheet", columnReferences)));
            return "SALARY";
        } else if (!spreadsheetSalaried && !qboSalaried) {
            LOGGER.debug("Detected employee: {} as not salaried.", employee.data.get(getJsonColumnName("employeeName", "spreadsheet", columnReferences)));
            return "REGULAR";
        }
        throw new RuntimeException("Could not determine if salaried.");
    }

    /**
     * Iterates through a list of employees to verify that each employee's instance variable, inputted, is true.<br>
     * This result is displayed via log4j.
     * @param employees The list of employees to check if any of them has not been inputted.
     */
    public static void checkIfAllInputted(List<Employee> employees) {
        for (Employee employee : employees) {
            if (!employee.inputted) {
                LOGGER.error("Employee on spreadsheet wasn't inputted: {}", employee.data.get(getJsonColumnName("employeeName", "spreadsheet", columnReferences)));
            }
        }
    }

    public static void printAllNoArgGetters(WebElement element) {
        System.out.println("Accessible Name: " + element.getAccessibleName());
        System.out.println("ARIA Role: " + element.getAriaRole());
        System.out.println("Location: " + element.getLocation());
        System.out.println("Rectangle: " + element.getRect());
        System.out.println("Size: " + element.getSize());
        System.out.println("Tag Name: " + element.getTagName());
        System.out.println("Text: " + element.getText());
        System.out.println("Is Displayed: " + element.isDisplayed());
        System.out.println("Is Enabled: " + element.isEnabled());
        System.out.println("Is Selected: " + element.isSelected());
    }
}






/*
    public static By byConfig(JsonObject byPair) {
        String value = byPair.get("value").getAsString();
        System.out.println("key: " + byPair.get("by").getAsString() + "value: " + value);
        switch (byPair.get("by").getAsString()) {
            case "XPATH":
                return By.xpath(value);
            case "CLASSNAME":
                return By.className(value);
            case "CSSSELECTOR":
                return By.cssSelector(value);
            case "ID":
                return By.id(value);
            case "LINKTEXT":
                return By.linkText(value);
            case "NAME":
                return By.name(value);
            case "PARTIALLINKTEXT":
                return By.partialLinkText(value);
            case "TAGNAME":
                return By.tagName(value);
            default:
                throw new RuntimeException("Could not classify By: " + byPair.get("by"));
        }
    }
    
     * Waits for each element in elementLocators to be isDisplayed().
     * Limitation: performs findElement() for each By, so findElements() won't work.
     * @param elementLocators List of By to wait for isDisplayed().
     * @param wait the wait to use.
    private static void waitForAllDisplayed(List<By> elementLocators, WebDriverWait wait) {
        wait.until(d -> {
            List<WebElement> elements = new ArrayList<>();
            elementLocators.forEach(element -> elements.add(d.findElement(element)));
            return elements.stream().allMatch(WebElement::isDisplayed);
        });
    }
    private static List<By> jsonArrayToByList(JsonArray byArray) {
        List<By> byList = new ArrayList<>();
        for (JsonElement pair : byArray) {
            byList.add(byConfig(pair.getAsJsonObject()));
        }
        return byList;
    }
    public static void savePayroll() {
        driver.findElement(By.xpath("/html/body/div[8]/div[2]/div/div/section/div/div/div[2]/div[3]/div[1]/button")).click();
    }
    
 */