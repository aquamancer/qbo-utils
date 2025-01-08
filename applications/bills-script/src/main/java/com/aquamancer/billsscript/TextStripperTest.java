package com.aquamancer.billsscript;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class TextStripperTest {
    public static void main(String[] args) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            PDFTextStripperByArea areaStripper = new PDFTextStripperByArea();
            stripper.setIndentThreshold(100f);
            stripper.setDropThreshold(-100f);
            stripper.setShouldSeparateByBeads(false);
            PDDocument doc = new PDDocument();
            doc.save(new File("/home/aqua/Downloads/petroleumtraders.pdf"));
            PDPage p1 = doc.getPage(0);
            areaStripper.setSortByPosition(true);           
            areaStripper.addRegion("fi", new Rectangle(0, 0, (int ) p1.getMediaBox().getWidth(), (int) p1.getMediaBox().getHeight()));
            areaStripper.extractRegions(p1);
            System.out.println(areaStripper.getTextForRegion("fi"));
        } catch (IOException ex) {

        }
    }
}
