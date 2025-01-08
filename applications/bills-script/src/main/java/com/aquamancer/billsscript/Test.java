//package org.aquamancer.bills;
//
//import org.apache.pdfbox.cos.COSDictionary;
//import org.apache.pdfbox.cos.COSName;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDPageTree;
//import org.apache.pdfbox.pdmodel.PDResources;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.apache.pdfbox.text.PDFTextStripperByArea;
//import org.json.JSONObject;
//
//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class Test {
//    public static void main(String[] args) {
//        File file = new File("/home/aqua/Downloads/petroleumtraders.pdf");
//        try {
//            PDDocument document = PDDocument.load(file);
//            PDPageTree pages = document.getPages();
//
//            List<String> invoiceNumbers = new ArrayList<>();
//        logger.debug("parsing invoice number coordinates from JSON");
//        JSONObject coords = Identifiers.identifiers.getJSONObject(Identifiers.getIndexOfVendor("Petroleum Traders")).getJSONObject("searchRectangles").getJSONObject("invoiceNumber");
//            int x0 = (int) (coords.getJSONArray("x").getFloat(0) * 72);
//            int x1 = (int) (coords.getJSONArray("x").getFloat(1) * 72);
//            int y0 = (int) (coords.getJSONArray("y").getFloat(0) * 72);
//            int y1 = (int) (coords.getJSONArray("y").getFloat(2) * 72);
//
//            System.out.println("x0: " + x0 + ", x1: " + x1 + "y0: " + y0 + "y1: " + y1);
//        try {
//            PDFTextStripperByArea textStripper = new PDFTextStripperByArea();
//
//            textStripper.addRegion("invoiceNumber", new Rectangle(x0, y0, x1 - x0, y1 - y0));
//            for (int i = 0; i < document.getNumberOfPages(); i++) {
//                textStripper.extractRegions(document.getPage(i));
//                System.out.println("page " + i + ": " + textStripper.getTextForRegion("invoiceNumber"));
//            }
//
//        } catch (IOException ex) {
//            logger.error("failed to initiailize PDFTextStripper");
//        }
//
//
//
//
//            for (PDPage page : pages) {
//                System.out.println("width: " + page.getMediaBox().getWidth() + " height: " + page.getMediaBox().getHeight());
//            }
//
//            //  extract text of entire document
//            PDFTextStripper stripper = new PDFTextStripper();
//            String documentText = stripper.getText(document);
//
//            // extract all images
//            List<BufferedImage> allImages = new ArrayList<>();
//
//            for (int i = 0; i < pages.getCount(); i++) {
//                // get list of pdf element names
//                PDResources resources = pages.get(i).getResources();
//                COSDictionary dict = resources.getCOSObject();
//                Iterable<COSName> XObjectNames = pages.get(i).getResources().getXObjectNames();
//                for (COSName name : XObjectNames) {
////                    System.out.println(name.toString());
////                    if (resources.isImageXObject(name)) {
////                        System.out.println("image found");
////                        allImages.add(((PDImageXObject) resources.getXObject(name)).getImage());
////                    }
//                }
//            }
//            for (int i = 0; i < allImages.size(); i++) {
//                File imageOut = new File(i + ".png");
//                ImageIO.write(allImages.get(i), "png", imageOut);
//            }
//            // try to compare bufferedimage to loaded jpg file
//            BufferedImage imageIn = ImageIO.read(new File("/home/aqua/IdeaProjects//0"));
////            BufferedImage imageIn = ImageIO.read(Test.class.getResource("/home/aqua/IdeaProjects//        0.jpg"));
//            for (int i = 0; i < allImages.size(); i++) {
//                if (bufferedImagesEqual(allImages.get(i), imageIn)) {
//                    System.out.println("match");
//                } else {
//                    System.out.println("not match");
//                }
//            }
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//    public static boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
//        if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight()) {
//            for (int x = 0; x < img1.getWidth(); x++) {
//                for (int y = 0; y < img1.getHeight(); y++) {
//                    if (img1.getRGB(x, y) != img2.getRGB(x, y))
//                        return false;
//                }
//            }
//        } else {
//            return false;
//        }
//        return true;
//    }
//
//}