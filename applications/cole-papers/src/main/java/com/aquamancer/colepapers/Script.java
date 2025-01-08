package com.aquamancer.colepapers;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Scanner;
public class Script {
    private static Scanner scanIn = new Scanner(System.in);
    private Invoice invoice;
    public Robot robot;
    public Script(Invoice invoice) {
        this.invoice = invoice;
        try {
            this.robot = new Robot();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    public void fillItems() {
        char c;
        Color test;
        System.out.println("Writing items in 3 seconds. Please focus the quickbooks window.");
        this.robot.delay(3000);
        for (int i = 0; i < this.invoice.getItems().size(); i++) {
            if (this.invoice.getItems().get(i).isInclude()) {
                this.writeString(this.invoice.getItems().get(i).getItemName());
                this.robot.delay(2000);
                this.tapKey(KeyEvent.VK_TAB);
            //    this.tapKey(KeyEvent.VK_TAB);
                this.robot.delay(1500);
                synchronized (this.robot) {
                    test = this.robot.getPixelColor(500, 500);
                }
                if (test.getRed() < 215 && test.getGreen() <215  && test.getBlue() < 215) {
                    System.out.println("new item detected. press enter after creating the new item(cursor should be on description");
                    scanIn.nextLine();
                    System.out.println("resuming in 3 seconds...");
                    this.robot.delay(3000);
                }
                this.tapKey(KeyEvent.VK_TAB);
                this.writeString(Integer.toString(this.invoice.getItems().get(i).getQuantity()));
                this.tapKey(KeyEvent.VK_TAB);
                this.tapKey(KeyEvent.VK_TAB);
                this.writeString(Float.toString(this.invoice.getItems().get(i).getTotalPrice()));
                this.tapKey(KeyEvent.VK_TAB);
                this.tapKey(KeyEvent.VK_TAB);
                if (this.invoice.getCustomer().equals("PORTAL")) {
                    this.writeString("North Dakota Full Maintenance (0001):ND Portal Ambrose");
                } else if (this.invoice.getCustomer().equals("PEMBINA")) {
                    this.writeString("North Dakota Full Maintenance (0001):ND Pembina");
                } else if (this.invoice.getCustomer().equals("DUNSEITH")) {
                    this.writeString("North Dakota Full Maintenance (0001):ND Dunseith St John");
                }
                robot.delay(3000);
                this.tapKey(KeyEvent.VK_TAB);
                robot.delay(500);
                this.tapKey(KeyEvent.VK_TAB);
                robot.delay(500);
                this.tapKey(KeyEvent.VK_TAB);
                robot.delay(500);
            }
        }
        this.writeString("Sales Tax (Supplies)");
        robot.delay(2500);
        this.tapKey(KeyEvent.VK_TAB);
        this.tapKey(KeyEvent.VK_TAB);
        this.tapKey(KeyEvent.VK_TAB);
        this.tapKey(KeyEvent.VK_TAB);
        this.writeString(Float.toString(this.invoice.getSalesTax()));
        this.tapKey(KeyEvent.VK_TAB);
        this.tapKey(KeyEvent.VK_TAB);
        if (this.invoice.getCustomer().equals("PORTAL")) {
            this.writeString("North Dakota Full Maintenance (0001):ND Portal Ambrose");
        } else if (this.invoice.getCustomer().equals("PEMBINA")) {
            this.writeString("North Dakota Full Maintenance (0001):ND Pembina");
        } else if (this.invoice.getCustomer().equals("DUNSEITH")) {
            this.writeString("North Dakota Full Maintenance (0001):ND Dunseith St John");
        }
        this.robot.delay(1500);
        this.tapKey(KeyEvent.VK_TAB);
    }
    public void writeString(String str) {
        char c;
        for (int i = 0; i < str.length(); i++) {
            c = str.charAt(i);
            if (c == '(') {
                this.robot.keyPress(KeyEvent.VK_SHIFT);
                this.tapKey(KeyEvent.VK_9);
                this.robot.keyRelease(KeyEvent.VK_SHIFT);
            } else if (c == ')') {
                this.robot.keyPress(KeyEvent.VK_SHIFT);
                this.tapKey(KeyEvent.VK_0);
                this.robot.keyRelease(KeyEvent.VK_SHIFT);
            } else if (c == ':') {
                this.robot.keyPress(KeyEvent.VK_SHIFT);
                this.tapKey(KeyEvent.VK_SEMICOLON);
                this.robot.keyRelease(KeyEvent.VK_SHIFT);
            } else if (c == '-') {
                this.tapKey(KeyEvent.VK_MINUS);
            } else if (c == '_') {
                this.robot.keyPress(KeyEvent.VK_SHIFT);
                this.tapKey(KeyEvent.VK_MINUS);
                this.robot.keyRelease(KeyEvent.VK_SHIFT);
            } else {
                if (Character.isUpperCase(c) == true) {
                    this.robot.keyPress(KeyEvent.VK_SHIFT);
                }
                this.tapKey(Character.toUpperCase(c));
                if (Character.isUpperCase(c) == true) {
                    this.robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
        }
    }
    public void tapKey(int e) {
        this.robot.keyPress(e);
        this.robot.delay(2);
        this.robot.keyRelease(e);
    }
}
