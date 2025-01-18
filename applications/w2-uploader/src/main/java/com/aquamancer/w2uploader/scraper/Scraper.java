package com.aquamancer.w2uploader.scraper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;

public class Scraper {
    // conversion 1 inch = 72 points
    private int IN_TO_PT = 72;
    private JsonObject map;
    public PDFTextStripperByArea stripper;

    public Scraper(){
        try {
            // load json file containing the coordinates to scrape values from
            this.map = JsonParser.parseReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("map.json"))).getAsJsonObject();
            // initialize the PDFTextStripperByArea and set its regions based on the json file.
            this.stripper = new PDFTextStripperByArea();
            this.loadRegionsIntoStripper(this.map, this.stripper);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not load map.json");
        }
    }
    private void loadRegionsIntoStripper(JsonObject map, PDFTextStripperByArea stripper) {
        for (Map.Entry<String, JsonElement> region : map.entrySet()) {
            List<Double> x = region.getValue().getAsJsonObject().get("x").getAsJsonArray().asList().stream().map(element -> element.getAsDouble() * IN_TO_PT).toList();
            List<Double> y = region.getValue().getAsJsonObject().get("y").getAsJsonArray().asList().stream().map(element -> element.getAsDouble() * IN_TO_PT).toList();
            stripper.addRegion(region.getKey(), new Rectangle2D.Double(x.get(0), y.get(0), x.get(1) - x.get(0), y.get(1) - y.get(0)));
        }
    }
    public List<String> getValues(PDPage pdPage) {
        try {
            this.stripper.extractRegions(pdPage);
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, JsonElement> pair : map.entrySet()) {
                String value = this.stripper.getTextForRegion(pair.getKey()).trim();
                result.add(value);
                System.out.println(pair.getKey() + ": " + value);
            }
            return result;
        } catch (IOException ex) {
            throw new RuntimeException("Could not parse pdf!");
        }
    }
    public List<String> getKeys() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> pair : map.entrySet()) {
            result.add(pair.getKey());
        }
        return result;
    }
    public List<String> getKeysWithName() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> pair : map.entrySet()) {
            result.add(pair.getKey() + '\n' + pair.getValue().getAsJsonObject().get("name").getAsString());
        }
        return result;
    }
}
