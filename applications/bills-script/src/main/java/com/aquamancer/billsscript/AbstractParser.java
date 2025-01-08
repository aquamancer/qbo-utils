package com.aquamancer.billsscript;

import org.apache.logging.log4j.LogManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;


import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractParser {
    File bill;
    public PDDocument document;
    public PDPageTree pageTree;
    public String allText;
    public String[] allTextLines;
    public String[] allPagesAsText;

    public PDFTextStripper textStripper;
    public boolean pdfLoadSuccess;
    private static final Logger LOGGER = LogManager.getLogger(AbstractParser.class);

    public AbstractParser(File bill) {
        PDFTextStripperByArea textStripperByArea;

        this.bill = bill;
        pdfLoadSuccess = false;

        try {
            this.textStripper = new PDFTextStripper();

            document.save(bill);
            
            pageTree = document.getPages();
            allText = textStripper.getText(document);
            allTextLines = allText.split("\n");
            
            // load string array of pages as text
            allPagesAsText = new String[document.getNumberOfPages()];
            for (int i = 0; i < allPagesAsText.length; i++) {
                textStripper.setStartPage(i + 1);
                textStripper.setEndPage(i + 1);
                allPagesAsText[i] = textStripper.getText(document);
            }


            pdfLoadSuccess = true;
        } catch (IOException ex) {
            System.out.println("Parser.java failed to convert PDF to object!");
        }
    }

    /**
     * Returns a List of Strings that were in the coordinates specified by Identifiers.json for each page
     * @param vendorName "name" key in the Identifiers.json
     * @param jsonKey a key in the "searchCoordinates" object in Identifiers.json that specifies the coordinates to extract text on each page
     * @return a list of Strings that were in the coordinates of jsonKey
     */
    public List<String> getTextFromArea(String vendorName, String jsonKey) {
        List<String> textPerPage = new ArrayList<>();
        LOGGER.debug("parsing {} coordinates from JSON", jsonKey);
        JSONObject coords = Identifiers.identifiers.getJSONObject(Identifiers.getIndexOfVendor(vendorName)).getJSONObject("searchRectangles").getJSONObject(jsonKey);
        float x0 = coords.getJSONArray("x").getFloat(0) * 72;
        float x1 = coords.getJSONArray("x").getFloat(1) * 72;
        float y0 = coords.getJSONArray("y").getFloat(0) * 72;
        float y1 = coords.getJSONArray("y").getFloat(1) * 72;

        try {
            PDFTextStripperByArea textStripper = new PDFTextStripperByArea();

            // loops through each page and extracts the text from the coordinates given in the json
            LOGGER.debug("parsing {} from area from each page", jsonKey);
            textStripper.addRegion("text", new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0));
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                textStripper.extractRegions(document.getPage(i));
                String text = textStripper.getTextForRegion("text").replace("\n", "");
                LOGGER.debug("parsed invoice number for page {}: {}", i, text);
                textPerPage.add(text);
            }
        } catch (IOException ex) {
            LOGGER.error("failed to initiailize PDFTextStripper");
        }

        return textPerPage;
    }

    /**
     * Returns the String found in the coordinates specified by Identifiers.json for a single page
     * @param vendorName "name" key in Identifiers.json
     * @param jsonKey a key in the "searchCoordinates" object in Identifiers.json that specifies the coordinates to extract text
     * @param pageNumber the target page to extract text, index begins at 0
     * @return
     */
    public String getTextFromArea(String vendorName, String jsonKey, int pageNumber) {
        LOGGER.debug("parsing {} coordinates from JSON", jsonKey);
        JSONObject coords = Identifiers.identifiers.getJSONObject(Identifiers.getIndexOfVendor(vendorName)).getJSONObject("searchRectangles").getJSONObject(jsonKey);
        float x0 = coords.getJSONArray("x").getFloat(0) * 72;
        float x1 = coords.getJSONArray("x").getFloat(1) * 72;
        float y0 = coords.getJSONArray("y").getFloat(0) * 72;
        float y1 = coords.getJSONArray("y").getFloat(1) * 72;

        try {
            PDFTextStripperByArea textStripper = new PDFTextStripperByArea();

            LOGGER.debug("parsing {} from area from each page", jsonKey);
            textStripper.addRegion("text", new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0));
            textStripper.extractRegions(document.getPage(pageNumber));
            String text = textStripper.getTextForRegion("text").replace("\n", "");
            LOGGER.debug("parsed element for page {}: {}", pageNumber, text);
            return text;
        } catch (IOException ex) {
            LOGGER.error("failed to initiailize PDFTextStripper");
        }
        return null;
    }

    /**
     * Returns the String found in the coordinates specified by Identifiers.json for a page range
     * @param vendorName "name" key in Identifiers.json
     * @param jsonKey a key in the "searchCoordinates" object in Identifiers.json that specifies the coordinates to extract text
     * @param startPage page to start the extraction
     * @param endPage page to end the extraction, exclusive
     * @return
     */
    public List<String> getTextFromArea(String vendorName, String jsonKey, int startPage, int endPage) {
        List<String> textPerPage = new ArrayList<>();
        LOGGER.debug("parsing {} coordinates from JSON", jsonKey);
        JSONObject coords = Identifiers.identifiers.getJSONObject(Identifiers.getIndexOfVendor(vendorName)).getJSONObject("searchRectangles").getJSONObject(jsonKey);
        float x0 = coords.getJSONArray("x").getFloat(0) * 72;
        float x1 = coords.getJSONArray("x").getFloat(1) * 72;
        float y0 = coords.getJSONArray("y").getFloat(0) * 72;
        float y1 = coords.getJSONArray("y").getFloat(1) * 72;

        try {
            PDFTextStripperByArea textStripper = new PDFTextStripperByArea();

            // loops through each page and extracts the text from the coordinates given in the json
            LOGGER.debug("parsing {} from area from page range", jsonKey);
            textStripper.addRegion("text", new Rectangle2D.Float(x0, y0, x1 - x0, y1 - y0));
            for (int i = startPage; i < endPage; i++) {
                textStripper.extractRegions(document.getPage(i));
                String text = textStripper.getTextForRegion("text").replace("\n", "");
                LOGGER.debug("parsed invoice number for page {}: {}", i, text);
                textPerPage.add(text);
            }
        } catch (IOException ex) {
            LOGGER.error("failed to initiailize PDFTextStripper");
        }

        return textPerPage;
    }

    public boolean pdfIsExpectedDimensions(String vendorName) {
        int expectedWidth = Identifiers.identifiers.getJSONObject(Identifiers.getIndexOfVendor(vendorName)).getJSONArray("expectedDimension").getInt(0);
        int expectedHeight = Identifiers.identifiers.getJSONObject(Identifiers.getIndexOfVendor(vendorName)).getJSONArray("expectedDimension").getInt(1);

        for (int i = 0; i < this.pageTree.getCount(); i++) {
            int actualWidth = (int) this.pageTree.get(i).getMediaBox().getWidth() / 72;
            int actualHeight = (int) this.pageTree.get(i).getMediaBox().getHeight() / 72;

            if (actualWidth != expectedWidth || actualHeight != expectedHeight) {
                LOGGER.warn("page {} expected dimensions of {}, {}; got {}, {}", i, expectedWidth, expectedHeight, actualWidth, actualHeight);
                return false;
            }
        }
        return true;
    }
}
