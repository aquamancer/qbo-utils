package com.aquamancer.colepapers;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.lang.Exception;
import java.util.Scanner;
public class Main {
    private static Scanner scanIn = new Scanner(System.in);
    private Invoice invoice;
    private Item tempItem;
    public Main(){
        this.invoice = new Invoice();
        this.tempItem = new Item();
    }
    public static BufferedReader bufferedReader;

    public StringBuilder getInvoiceNumber(StringBuilder line){
        StringBuilder invoiceNumber = new StringBuilder();
        if (line.indexOf("INVOICE #") == 0) {
            for (int i = 1; line.charAt(line.indexOf("#") + i) != ' '; i++){
                invoiceNumber.append(line.charAt(line.indexOf("#") + i));
            }
        }
        return invoiceNumber;
    }
    public StringBuilder getInvoiceDate(StringBuilder line){
        StringBuilder invoiceDate = new StringBuilder();
        if (line.indexOf("INVOICE #") == 0) {
            for (int i = 1; i <= 10; i++){
                invoiceDate.append(line.charAt(line.lastIndexOf(" ") + i));
            }
        }
        return invoiceDate;
    }
    public String getCustomer(ArrayList<StringBuilder> address){
        String[] customers = {"PORTAL", "PEMBINA", "DUNSEITH"};
        StringBuilder line;
        for (int i = 0; i < address.size(); i++){
            line = new StringBuilder(address.get(i).toString());
            for (int j = 0; j < customers.length; j++){
                if(line.indexOf(customers[j]) != -1 && line.indexOf("Purchase Order") == -1){ //don't trust the "purchase order line", trust the ship to
                    return customers[j];
                }
            }
        }
        return "customer not found";
    }
    public String getItemName(StringBuilder line, ArrayList<Integer> whitespaces){
        return line.substring(whitespaces.get(4) + 1, whitespaces.get(5));
    }
    public int getQuantity(StringBuilder line, ArrayList<Integer> whitespaces){
        return Integer.parseInt(line.substring(whitespaces.get(5) + 1, whitespaces.get(6)).replace(",", ""));
    }
    public float getUnitPrice(StringBuilder line, ArrayList<Integer> whitespaces) {
        return Float.parseFloat(line.substring(whitespaces.get(6) + 1, whitespaces.get(7)).replace(",", ""));
    }
    public float getTotalPrice(StringBuilder line, ArrayList<Integer> whitespaces) {
        return Float.parseFloat(line.substring(whitespaces.get(7) + 1, whitespaces.get(8)).replace(",", ""));
    }
    public float getSubtotal(StringBuilder line, ArrayList<Integer> whitespaces) {
        return Float.parseFloat(line.substring(0, whitespaces.get(0)).replace(",", ""));
    }
    public float getSalesTax(StringBuilder line, ArrayList<Integer> whitespaces) {
        return Float.parseFloat(line.substring(whitespaces.get(0) + 1, whitespaces.get(1)).replace(",", ""));
    }
    public float getGrandTotal(StringBuilder line, ArrayList<Integer> whitespaces) {
        return Float.parseFloat(line.substring(whitespaces.get(1) + 1, whitespaces.get(2)).replace(",", ""));
    }
    public static void main(String[] args){
        Main main = new Main();
        String lineRead;
        int lineNumber = 1;
        StringBuilder line;
        //address
        ArrayList<StringBuilder> address = new ArrayList<StringBuilder>();
        boolean addressDone = false;
        //items
        int itemNumber = 1;
        ArrayList<Integer> whitespaces = new ArrayList<Integer>(0);
        int startingIndex = -1;
        //json and elements
        StringBuilder description = new StringBuilder(0);
        boolean itemsDone = false;
        //subtotal, tax, total
        int lineBeforeFinal = -1;
        int finalStartingIndex = -1;
        ArrayList<Integer> finalWhitespaces = new ArrayList<Integer>();
        try{
            bufferedReader = new BufferedReader(new FileReader("applications/cole-papers/src/main/resources/Paste.txt"));
            while((lineRead = bufferedReader.readLine()) != null){
                line = new StringBuilder(lineRead);
                if(lineNumber == 1){
                    main.invoice.setInvoiceNumber(main.getInvoiceNumber(line).toString());
                    main.invoice.setInvoiceDate(main.getInvoiceDate(line).toString());
                } else if (addressDone == false){
                    address.add(line);
                    if(line.indexOf("LN UOM ORDER SHIP B/O DESCRIPTION INVOICE UNIT EXTENDED TAX") == 0){
                        addressDone = true;
                        main.invoice.setCustomer(main.getCustomer(address));
                    }
                } else if (addressDone == true && itemsDone == false) {
                    while(line.indexOf(" ", startingIndex + 1) != -1){
                        whitespaces.add(line.indexOf(" ", startingIndex + 1));
                        startingIndex = line.indexOf(" ", startingIndex + 1);
                    }
                    try {
                        if(whitespaces.size() >= 3) { //testing if line is an item
                            Integer.parseInt(line.substring(0, whitespaces.get(0)).replace(",", ""));
                            for (int i = 1; i < 4; i++) { //item check
                                Integer.parseInt(line.substring(whitespaces.get(i) + 1, whitespaces.get(i + 1)).replace(",", ""));
                            }
                            if (main.tempItem.getItemName() != null) { //adds description to tempObj and ship if line is an item and it is not the first item
                                if(description.length() != 0){
                                    description.deleteCharAt(description.length() - 1);
                                }
                                main.tempItem.setDescription(description.toString());
                                main.invoice.addItem(main.tempItem);
                                main.tempItem = new Item();
                                description = new StringBuilder();
                            }
                            //tempObj.put("itemNumber", Integer.parseInt(line.substring(0, whitespaces.get(0))));
                            main.tempItem.setUOM(line.substring(whitespaces.get(0) + 1, whitespaces.get(1)));
                            main.tempItem.setOriginalOrder(Integer.parseInt(line.substring(whitespaces.get(1) + 1, whitespaces.get(2)).replace(",", "")));
                            main.tempItem.setPreviousShip(Integer.parseInt(line.substring(whitespaces.get(2) + 1, whitespaces.get(3)).replace(",", "")));
                            main.tempItem.setBO(Integer.parseInt(line.substring(whitespaces.get(3) + 1, whitespaces.get(4)).replace(",", "")));
                            main.tempItem.setItemName(line.substring(whitespaces.get(4) + 1, line.length()));
                            if (whitespaces.size() > 5 && Integer.parseInt(line.substring(0, whitespaces.get(0)).replace(",", "")) == itemNumber) { //if line actually has amounts
                                main.tempItem.setInclude(true);
                                main.tempItem.setQuantity(main.getQuantity(line, whitespaces));
                                main.tempItem.setUnitPrice(main.getUnitPrice(line, whitespaces));
                                main.tempItem.setTotalPrice(main.getTotalPrice(line, whitespaces));
                                main.tempItem.setItemName(main.getItemName(line, whitespaces));
                            } else {
                                main.tempItem.setInclude(false);
                            }
                            itemNumber++;
                        } else {
                            if(line.indexOf("REMIT TO:") != -1){ //ship the last item. trigger is remit to instead of an item
                                itemsDone = true;
                                if(description.length() != 0){
                                    description.deleteCharAt(description.length() - 1);
                                }
                                main.tempItem.setDescription(description.toString());
                                main.invoice.addItem(main.tempItem);
                                main.tempItem = new Item();
                                description = new StringBuilder();
                            }
                            if(itemsDone == false){
                                if (line.indexOf("INVOICE #" + main.invoice.getInvoiceNumber() + " Page") == -1 && line.indexOf("ORIG PREV STOCK NO. THIS") == -1 && line.indexOf("LN UOM ORDER SHIP B/O DESCRIPTION INVOICE UNIT EXTENDED TAX") == -1) {
                                    description.append(line.toString() + " ");
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        if(line.indexOf("REMIT TO:") != -1){ //ship the last item. trigger is remit to instead of an item
                            itemsDone = true;
                            if(description.length() != 0){
                                description.deleteCharAt(description.length() - 1);
                            }
                            main.tempItem.setDescription(description.toString());
                            main.invoice.addItem(main.tempItem);
                            main.tempItem = new Item();
                            description = new StringBuilder();
                        }
                        if(itemsDone == false){
                            if (line.indexOf("INVOICE #" + main.invoice.getInvoiceNumber() + " Page") == -1 && line.indexOf("ORIG PREV STOCK NO. THIS") == -1 && line.indexOf("LN UOM ORDER SHIP B/O DESCRIPTION INVOICE UNIT EXTENDED TAX") == -1) {
                                description.append(line.toString() + " ");
                            }
                        }
                    }
                    whitespaces.clear();
                    startingIndex = -1;
                } else if (addressDone == true && itemsDone == true) { //&& final == false
                    //todo tax, subtotal, total
                    if(line.indexOf("Subtotal Tax Total Deposit Amount Due Due Date") != -1){
                        lineBeforeFinal = lineNumber;
                    } else if (lineNumber == lineBeforeFinal + 1){
                        while(line.indexOf(" ", finalStartingIndex + 1) != -1){
                            finalWhitespaces.add(line.indexOf(" ", finalStartingIndex + 1));
                            finalStartingIndex = line.indexOf(" ", finalStartingIndex + 1);
                        }
                        main.invoice.setSubtotal(main.getSubtotal(line, finalWhitespaces));
                        main.invoice.setSalesTax(main.getSalesTax(line, finalWhitespaces));
                        main.invoice.setGrandTotal(main.getGrandTotal(line, finalWhitespaces));
                    }
                }
                lineNumber++;
            }
        } catch(Exception e){
            System.out.println(e.toString());
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println("Invoice object was successfully filled. Clear all item lines and click the item name box. Press enter to begin script. type cancel to cancel.");
        if (scanIn.nextLine().equals("")) {
            Script script = new Script(main.invoice);
            script.fillItems();
            System.out.println("Finished. The total should be: " + main.invoice.getGrandTotal());
        } else {
            System.exit(0);
        }
    }
}
