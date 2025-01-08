package com.aquamancer.billsscript;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONArray;
import org.json.JSONObject;


import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class Identifiers {
    // when a variable = function, the function is not called again when you use the variable
    public static JSONArray identifiers = parseIdentifiers();
    public static void main(String[] args) {
        parseIdentifiers();
        System.out.println("bill: " + identifyBill(new File("/home/aqua/Downloads/petroleumtraders.pdf")));
    }
    public static JSONArray parseIdentifiers() {
        try {
            File jsonIn = new File("src/main/java/org/aquamancer/bills/Identifiers.json");
            BufferedReader reader = new BufferedReader(new FileReader(jsonIn));
            
            String jsonString = "";
            String line;

            while ((line = reader.readLine()) != null) {
                // line = line.replaceAll("\n", "").replaceAll(" ", "");
                jsonString = jsonString.concat(line);
            }

            return new JSONArray(jsonString);
            //System.out.println(array.getJSONObject(0).getJSONObject("identifiers").toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    public static String identifyBill(File file) {
        try {
            // load the document -> PDDocument -> PDPageTree
            PDDocument document = new PDDocument();
            document.save(file);
            
            PDPageTree pages = document.getPages();

            //  extract text of entire document
            PDFTextStripper stripper = new PDFTextStripper();
            String allText = stripper.getText(document);

            // extract all images -> BufferedImages stored in ArrayList
            List<BufferedImage> allImages = new ArrayList<>();

            for (int i = 0; i < pages.getCount(); i++) {
                // for each page:
                // load elements
                PDResources resources = pages.get(i).getResources();
                // get list of pdf element names
                Iterable<COSName> XObjectNames = resources.getXObjectNames();
                // loop through the element names and process if image
                for (COSName name : XObjectNames) {
                    // if image, add the BufferedImage to allImages list
                    if (resources.isImageXObject(name)) {
                        allImages.add(((PDImageXObject) resources.getXObject(name)).getImage());
                    }
                }
            }

            // gather matches and put them into a list
            List<String> billMatches = new ArrayList<>();

            for (int i = 0; i < identifiers.length(); i++) {
                if (billIsMatch(identifiers.getJSONObject(i), allImages, allText)) {
                    billMatches.add(identifiers.getJSONObject(i).getString("name"));
                }
            }

            if (billMatches.size() == 0) {
                System.out.println("no matches");
                return null;
            } else if (billMatches.size() == 1) {
                System.out.println("match found: " + billMatches.get(0));
                return billMatches.get(0);
            } else {
                System.out.println("more than one match for this bill. make sure there are sufficient identifiers for this bill");
                return null;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    public static boolean billIsMatch(JSONObject jsonBill, List<BufferedImage> billImages, String billText) {
        // go through each image and text identifier.
        // if a super is met, break: true
        // if an and is not met, break: false
        // if all ands are met, break: true
        // if there are no and conditions, and supers are not met: false
        // edge case: no conditions in the json file
        JSONArray image = jsonBill.getJSONObject("identifiers").getJSONArray("image");
        JSONArray text = jsonBill.getJSONObject("identifiers").getJSONArray("text");
        int numAnds = 0; // used to cover an edge case where the number of and conditions are 0. because this will return true if there were no ands that were not met

        try {
            // process images
            for (int i_image = 0; i_image < image.length(); i_image++) {
                    // import the image from the path given in the json
                BufferedImage imageCondition = ImageIO.read(new File(image.getJSONObject(i_image).getString("path")));
                String logic = image.getJSONObject(i_image).getString("logic");

                // compare the images from the bill with the image from the json
                for (int i_billImages = 0; i_billImages < billImages.size(); i_billImages++) {
                    boolean imagesEqual = areBufferedImagesEqual(billImages.get(i_billImages), imageCondition);

                    if (logic.equals("super")) {
                        if (imagesEqual) {
                            return true;
                        }
                    } else if (logic.equals("and")) {
                        numAnds++;
                        if (!imagesEqual) { // for "and" conditions, only checks for ands that were not met to return false. if there are 0 and conditions in the json, it will return true because no ands were deemed false
                            return false;
                        }
                    }
                }
            }

            // process text
            for (int i_text = 0; i_text < text.length(); i_text++) {
                String logic = text.getJSONObject(i_text).getString("logic");
                if (logic.equals("super")) {
                    if (billText.contains(text.getJSONObject(i_text).getString("text"))) {
                        return true;
                    }
                } else if (logic.equals("and")) {
                    numAnds++;
                    if (!billText.contains(text.getJSONObject(i_text).getString("text"))) {
                        return false;
                    }
                }
            }

            // final conditional catches
            if (numAnds == 0) {
                // if numAnds == 0, this means that there are no and conditions, and no supers were met
                return false;
            } else {
                // reached when there are no supers or no supers were true, and all ands were satisfied
                return true;
            }
        } catch (IOException ex) {
            System.out.println("could not read image from json file path!");
        }
        System.out.println("either true or false should've been caught whilst identifying this bill. should not have reached this point");
        return false;
    }
    public static boolean areBufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
            for (int x = 0; x < img1.getWidth(); x++) {
                for (int y = 0; y < img1.getHeight(); y++) {
                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
                        return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }
    public static int getIndexOfVendor(String vendorName) {
        for (int i = 0; i < identifiers.length(); i++) {
            if (identifiers.getJSONObject(i).get("name").equals(vendorName)) {
                return i;
            }
        }
        return -1;
    }
}
