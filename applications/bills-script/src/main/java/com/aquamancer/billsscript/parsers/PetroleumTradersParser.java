package com.aquamancer.billsscript.parsers;

import com.aquamancer.billsscript.Item;
import com.aquamancer.billsscript.AbstractParser;
import com.aquamancer.billsscript.ParserActions;
import com.aquamancer.billsscript.fuel.PetroleumTradersBill;
import org.apache.logging.log4j.LogManager;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.logging.log4j.Logger;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;

import java.io.File;
import java.util.List;

public class PetroleumTradersParser extends AbstractParser implements ParserActions {
    private static final Logger LOGGER = LogManager.getLogger(PetroleumTradersParser.class);

    public PetroleumTradersParser(File bill) {
        super(bill);
    }
    @Override
    public PetroleumTradersBill parseBill() {
        if (this.pdfLoadSuccess) {

        }
        return null;
    }

    /**
     * Returns a List of pairs/tuples/Map.Entry's of &lt; page number, invoice number &gt;.
     * There are multiple parse methods via text.
     * Given: IIIIIIIAAAAAA/AAA
     * where I = invoice # digit, A = account # digit
     * parse method A: first 7 digits
     * parse method B: from right to left, trimming digits until reaching "/" and then 6 more digits
     * parse method C: not relying on any positional reference, but identifying the IIIIIIIAAAAAA/AAA sequence, and using parse methods A and B
     * @return a List of <pageNumber, invoiceNumber>, using multiple different parse methods
     */

    @Override
    public List<AbstractMap.SimpleImmutableEntry<Integer, String>> getInvoiceNumbersFromText() {
        /*
            Salesperson:Invoice #:
            1995058990679/145

            I = invoice number, A = account number
            IIIIIIIAAAAAA/AAA
         */

        // find the line that includes "Salesperson:Invoice #:"
        // then parse the next line
        List<AbstractMap.SimpleImmutableEntry<Integer, String>> invoiceNumbers = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < this.allPagesAsText.length; pageNumber++) {
            String[] lines = this.allPagesAsText[pageNumber].split("\n");

            LOGGER.debug("parsing invoice number from text on page: " + pageNumber);

            for (int lineNumber = 0; lineNumber < lines.length - 1; lineNumber++) {
                if (lines[lineNumber].contains("Invoice #:")) {
                    // store the next line, which is the string we want to parse
                    String data = lines[lineNumber + 1];
                    String result;
                    LOGGER.debug("parsing invoice number from this String: " + data);

                    // Method A
                    result = data.substring(0, 7);
                    LOGGER.debug("Method A - first 7 digits: " + result);
                    invoiceNumbers.add(new AbstractMap.SimpleImmutableEntry<>(pageNumber, result));

                    // Method B
                    // make sure the string to parse contains "/" because we will run a while loop
                    if (data.contains("/")) {
                        result = data;
                        while (result.contains("/")) {
                            result = result.substring(0, result.length() - 1);
                        }
                        result = result.substring(0, result.length() - 6);
                        invoiceNumbers.add(new AbstractMap.SimpleImmutableEntry<>(pageNumber, result));
                        LOGGER.debug("Method B - trim account number off the end: " + result);
                    } else {
                        LOGGER.error("could not parse invoice number from text using Method B because there is no / in the String");
                    }
                }
                // Method C
                if (lines[lineNumber].contains("/")) {

                }
            }
        }
        return invoiceNumbers;
    }

    /**
     * Returns a list of potential invoice numbers for a single page
     * @param pageNumber the page to parse
     * @return
     */
    public List<String> getInvoiceNumbersFromText(int pageNumber) {
        List<String> invoiceNumbers = new ArrayList<>();

            String[] lines = this.allPagesAsText[pageNumber].split("\n");

            LOGGER.debug("parsing invoice number from text on page: " + pageNumber);

            for (int lineNumber = 0; lineNumber < lines.length - 1; lineNumber++) {
                if (lines[lineNumber].contains("Invoice #:")) {
                    // store the next line, which is the string we want to parse
                    String data = lines[lineNumber + 1];
                    String result;
                    LOGGER.debug("parsing invoice number from this String: " + data);

                    // Method A
                    result = data.substring(0, 7);
                    LOGGER.debug("Method A - first 7 digits: " + result);
                    invoiceNumbers.add(result);

                    // Method B
                    // make sure the string to parse contains "/" because we will run a while loop
                    if (data.contains("/")) {
                        result = data;
                        while (result.contains("/")) {
                            result = result.substring(0, result.length() - 1);
                        }
                        result = result.substring(0, result.length() - 6);
                        invoiceNumbers.add(result);
                        LOGGER.debug("Method B - trim account number off the end: " + result);
                    } else {
                        LOGGER.error("could not parse invoice number from text using Method B because there is no / in the String");
                    }
                }
                // Method C
                if (lines[lineNumber].contains("/")) {

                }
            }
        return invoiceNumbers;
    }

    @Override
    public List<String> getInvoiceDatesFromText() {
        List<String> invoiceDates = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < this.allPagesAsText.length; pageNumber++) {
            String[] lines = this.allPagesAsText[pageNumber].split("\n");
            for (int iLine = 0; iLine < lines.length - 1; iLine++) {
                if (lines[iLine].contains("Account #:P. O. Number:")) {
                    invoiceDates.add(lines[iLine + 1]);
                    break;
                }
            }
        }
        return invoiceDates;
    }


    public String getInvoiceDatesFromText(int pageNumber) {
        String[] lines = this.allPagesAsText[pageNumber].split("\n");

        for (int iLine = 0; iLine < lines.length - 1; iLine++) {
            if (lines[iLine].contains("Account #:P. O. Number:")) {
                return lines[iLine + 1];
            }
        }

        return null;
    }

    /**
     * Parses and returns a list of Petroleum Traders items
     * @return
     */
    public List<Item> getItems(int pageNumber) {

        // if we take the area on the left to capture everything before the indented descriptions we can identify which lines are names.
        // the other lines will be either the amount, or description
        // make a list of the partial lines captured
        // then match the partial lines to the entire string
        // then separate
        try {
            PDFTextStripperByArea textStripperByArea = new PDFTextStripperByArea();

            textStripperByArea.addRegion("leftIndent", new Rectangle2D.Double(0, 0, 0.7 * 72, 11 * 72));
            textStripperByArea.extractRegions(this.pageTree.get(pageNumber));

        } catch (IOException ex) {
            LOGGER.error("failed to initialize pdf text stripper");
        }


        return List.of();
    }

    public String getTextInLeftIndent(int pageNumber) {
        try {
            PDFTextStripperByArea textStripperByArea = new PDFTextStripperByArea();
            
        } catch (IOException ex) {

        }
        return null;
    }

    /**
     * For each page, multiple potential invoice numbers are generated from multiple parsing methods.
     * In the event that the invoice numbers returned from the parsing methods differ,
     * this method will weigh each possibility and determine which invoice number is the most likely.
     *
     * Matches:
     * @return a List, ordered by page number, of the most likely invoice numbers of each page
     */
    public List<String> getMostLikelyInvoiceNumbers() {
        return List.of();
    }

    /**
     * Aggregates all potential invoice numbers for each page, then compares each pages' invoice numbers. <br>
     * If there are differences, a separate bill is assumed.
     * @return a list of page indexes of when a new bill starts. 0 is added by default
     */
    public List<Integer> getIndexesOfSeparateBills() {
        List<String> invoiceNumbersFromArea = this.getTextFromArea("Petroleum Traders", "invoiceNumber");
        List<AbstractMap.SimpleImmutableEntry<Integer, String>> invoiceNumbersFromText = this.getInvoiceNumbersFromText();
        List<List<String>> allInvoiceNumbersPerPage = new ArrayList<>();

        for (int i = 0; i < this.pageTree.getCount(); i++) {
            // aggregate all invoice numbers for a single page, then compare the entire contents to the next page.
            // two arraylists are .equals() if they contain the same elements in the same order
            List<String> invoiceNumbersOnPage = new ArrayList<>();

            invoiceNumbersOnPage.add(invoiceNumbersFromArea.get(i));
            for (int j = 0; j < invoiceNumbersFromText.size(); j++) {
                if (invoiceNumbersFromText.get(i).getKey() == i)
                    invoiceNumbersOnPage.add(invoiceNumbersFromText.get(i).getValue());
            }

            allInvoiceNumbersPerPage.add(invoiceNumbersOnPage);
        }

        List<Integer> indexesOfNewInvoice = new ArrayList<Integer>();

        indexesOfNewInvoice.add(0);
        for (int i = 0; i < allInvoiceNumbersPerPage.size() - 1; i++) {
            if (!allInvoiceNumbersPerPage.get(i).equals(allInvoiceNumbersPerPage.get(i + 1))) {         // first comparison: if the list of invoice numbers are in any way different
                String numFromB0 = allInvoiceNumbersPerPage.get(i).get(2);           // second comparison: if the invoice number from text from method B (paring) last 4 digits are different
                String numFromB1 = allInvoiceNumbersPerPage.get(i + 1).get(2);

                if (!numFromB0.equals(numFromB1))
                    indexesOfNewInvoice.add(i);
            }
        }

        return indexesOfNewInvoice;
    }

    /**
     * Aggregates all potential invoice numbers for each page, then compares each pages' invoice numbers. <br>
     * If there are differences, a separate bill is assumed.
     * @return a list of page indexes of when a new bill starts. 0 is added by default
     */
    public List<Integer> getIndexOfSeparateBills0() {
        List<String> invoiceNumbersFromArea = new ArrayList<>();

        for (int i = 0; i < this.pageTree.getCount(); i++)
            invoiceNumbersFromArea.add(this.getTextFromArea("Petroleum Traders", "invoiceNumber", i));

        List<List<String>> invoiceNumbersFromText = new ArrayList<>();

        for (int i = 0; i < this.pageTree.getCount(); i++)
            invoiceNumbersFromText.add(this.getInvoiceNumbersFromText(i));

        List<List<String>> invoiceNumbersPerPage = new ArrayList<>();

        for (int i = 0; i < this.pageTree.getCount(); i++) {
            List<String> singlePage = new ArrayList<>();

            singlePage.add(invoiceNumbersFromArea.get(i));
            for (int j = 0; j < invoiceNumbersFromText.get(i).size(); j++)
                singlePage.add(invoiceNumbersFromText.get(i).get(j));
            invoiceNumbersPerPage.add(singlePage);
        }

        List<Integer> indexesOfNewInvoice = new ArrayList<>();

        indexesOfNewInvoice.add(0);
        for (int i = 0; i < invoiceNumbersPerPage.size() - 1; i++) {
            if (!invoiceNumbersPerPage.get(i).equals(invoiceNumbersPerPage.get(i + 1))) {         // first comparison: if the list of invoice numbers are in any way different
                String numFromB0 = invoiceNumbersPerPage.get(i).get(2);           // second comparison: if the invoice number from text from method B (paring) is different
                String numFromB1 = invoiceNumbersPerPage.get(i + 1).get(2);

                if (!numFromB0.equals(numFromB1))
                    indexesOfNewInvoice.add(i);
            }
        }

        return indexesOfNewInvoice;
    }

    public static void main(String[] args) {
        PetroleumTradersParser parser = new PetroleumTradersParser(new File("/home/aqua/Downloads/petroleumtraders.pdf"));
        List<String> invoiceNumbersFromArea = parser.getTextFromArea("Petroleum Traders", "invoiceNumber");
        List<String> invoiceDatesFromArea = parser.getTextFromArea("Petroleum Traders", "invoiceDate");
        List<AbstractMap.SimpleImmutableEntry<Integer, String>> invoiceNumbersFromText = parser.getInvoiceNumbersFromText();
        List<String> invoiceDatesFromText = parser.getInvoiceDatesFromText();

        for (int i = 0; i < invoiceNumbersFromArea.size(); i++) {
            System.out.println("invoice numbers from area: ");
            System.out.println(invoiceNumbersFromArea.get(i));
        }
        for (int i = 0; i < invoiceDatesFromArea.size(); i++) {
            System.out.println("invoice dates from area: ");
            System.out.println(invoiceDatesFromArea.get(i));
        }
        for (int i = 0; i < invoiceNumbersFromText.size(); i++) {
            System.out.println("invoice numbers from text: ");
            System.out.println("Page: " + invoiceNumbersFromText.get(i).getKey() + ", value: " + invoiceNumbersFromText.get(i).getValue());
        }
        for (int i = 0; i < invoiceDatesFromText.size(); i++) {
            System.out.println("invoice dates from text: ");
            System.out.println("Page: " + i + ": " + invoiceDatesFromText.get(i));
        }
        for (int i = 0; i < parser.allPagesAsText.length; i++) {
            System.out.println(parser.allPagesAsText[i]);
        }
    }

    @Override
    public List<String> getInvoiceDatesFromArea() {
        return null;
    }
    @Override
    public List<String> getInvoiceNumbersFromArea() {
        return null;
    }
}
