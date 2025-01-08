package com.aquamancer.csv;
import org.apache.commons.csv.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CsvToMaps {
    //start columns are inclusive, start rows are not inclusive. INDEX STARTS AT 0. first row and column with values = 0
    //markers should be one row above the origin, and one row below the bottom right of the table
    private static int startCol, startRow, endCol, endRow;
    public static CSVParser parser;
    public static List<CSVRecord> records;
    public static List<List<String>> table = new ArrayList<>();

    /**'
     * Returns a List of employee data. The employee data is formatted as a Map&lt;column name, cell value&gt;.
     * @param filePath
     * @param bounded
     * @return each element in the List is an employee row. The map contains &lt;column header, value&gt;
     */
    public static List<Map<String, String>> getData(String filePath, boolean bounded) {
        try {
            System.out.println("attempting to read file: " + filePath);
            FileReader csvData = new FileReader(filePath);
            parser = CSVParser.parse(csvData, CSVFormat.EXCEL);
            System.out.println("successfully read file");
        } catch (IOException ex) {
            System.out.println("file error: " + filePath);
        }
        records = parser.getRecords(); 
        loadTableList();
        if (bounded) {
            parseBounds();
            trimToBounds();
        }
        List<Map<String, String>> data = new ArrayList<>();
        List<String> colHeaders = new ArrayList<>();
        // for each row in the table
        for (int i = 0; i < table.size(); i++) {
            Map<String, String> tempMap = new HashMap<>();
            if (i == 0) { // first row
                colHeaders = table.get(i);
                continue;
            }
            // for each cell, create a tempMap with <the column header, the cell value>
            for (int j = 0; j < table.get(i).size(); j++) {
                tempMap.put(colHeaders.get(j), table.get(i).get(j));
            }
            data.add(tempMap);
        }
        return data;
    }

    /**
     * converts records, an ArrayList of CSVRecords into
     * an ArrayList of ArrayList of Strings.
     * basically a 2d array but ArrayList
     */
    public static void loadTableList() {
        for (int i = 0; i < records.size(); i++) {
            List<String> tempRow = new ArrayList<String>();
            for (int j = 0; j < records.get(i).values().length; j++) {
                tempRow.add(records.get(i).values()[j]);
            }
            table.add(tempRow);
        }
    }
    /**
     * sets the indexes of bounds of the table.
     * start marker should be one cell above the first cell(top left)
     * end marker should be one cell below the last cell of the "total" row(bottom right)
     * markers are cropped out
     * end row index includes the "total" row. this row will be cropped out in the trimToBounds function
     */
    public static void parseBounds() {
        boolean beginFound = false;
        boolean endFound = false;
        for (int i = 0; i < table.size(); i++) {
            //System.out.println(table.get(i).size());
            for (int j = 0; j < table.get(i).size(); j++) {
                if (table.get(i).get(j).equals("BEGIN TABLE")) {
                    startRow = i + 1;
                    startCol = j;
                    System.out.println("successfully located BEGIN TABLE marker\ntable starts at: row = " + startRow + ", col = " + startCol);
                    beginFound = true;
                } else if (table.get(i).get(j).equals("END TABLE")) {
                    endRow = i - 1;
                    endCol = j;
                    System.out.println("successfully located END TABLE marker\ntable ends at: row = " + endRow + ", col = " + endCol);
                    endFound = true;
                }
            }
        }
        if (beginFound == false) {
            System.out.println("could not locate BEGIN TABLE marker!");
        }
        if (endFound == false) {
            System.out.println("could not locate END TABLE marker!");
        }
    }

    public static void trimToBounds() {
        //start and end are INCLUSIVE
        // trim bottom rows, including the total row
        // if the table size is 10, and endRow index is 9, remove 1 row
        // for i = 10; i > 9 => loops once
        for (int i = table.size(); i > endRow; i--) {
            table.removeLast(); //keeps removing the row in the place of the marker
        }
        // trim top rows
        // marker is already trimmed
        // e.g. startRow is row index 1, get rid of 1 row
        for (int i = 0; i < startRow; i++) {
            table.removeFirst(); // keeps removing the first row
            //endRow--; // decrement the end row index because rows are being removed. need to use this if you remove the top rows first
        }
        // trim right columns
        // if the column size = 10, endRow index is 9, remove 0 rows
        for (int i = 0; i < table.size(); i++) { // loop through each row to delete the ends of each row
            // if row length = 10, endRow index is 9 + 1, remove 0 rows
            for (int j = table.get(i).size(); j > endCol + 1; j++) {
                table.get(i).removeLast();
            }
        }
        // trim left columns
        // if the start column = 0, remove 0 rows
        for (int i = 0; i < table.size(); i++) {
            for (int j = 0; j < startCol; j++) {
                table.get(i).removeFirst();
            }
        }
    }
    public static void main(String[] args) {
    }
}
