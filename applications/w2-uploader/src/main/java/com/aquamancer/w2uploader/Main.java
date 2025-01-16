package com.aquamancer.w2uploader;

import com.aquamancer.w2uploader.scraper.Scraper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        //todo remove the \n after each extract
        // todo use box b's contents to verify they are correct.
        // jarPath used to output file there
        String jarPath;
        try {
            jarPath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getAbsolutePath();
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Could not locate jar path.");
        }

        PDDocument pdf = promptPdf();
        Scraper scraper = new Scraper();

        try (CSVPrinter writer = new CSVPrinter(new FileWriter(jarPath + "/output.csv"), CSVFormat.Builder.create().setHeader(scraper.getKeysWithName().toArray(new String[0])).build())) {
            // iterate through each page in the pdf
            for (PDPage page : pdf.getPages()) {
                writer.printRecord(scraper.getValues(page));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private static PDDocument promptPdf() {
        try (Scanner input = new Scanner(System.in)) {
            System.out.print("Enter path of w2 pdf: ");
            String filePath = input.nextLine();
            return Loader.loadPDF(new File(filePath));
        } catch (IOException ex) {
            throw new RuntimeException("Could not load w2 pdf.");
        }
    }
}
