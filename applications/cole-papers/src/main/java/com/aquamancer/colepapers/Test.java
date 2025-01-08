package com.aquamancer.colepapers;

import java.awt.*;

public class Test {
    public static void main(String[] args) {
        try {
            Robot robot = new Robot();
            System.out.println(robot.getPixelColor(300, 900));
        } catch (AWTException ex) {
            throw new RuntimeException("error");
        }
    }
}
