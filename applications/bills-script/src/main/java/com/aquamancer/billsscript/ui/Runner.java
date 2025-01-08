package com.aquamancer.billsscript.ui;
import javax.swing.*;

public class Runner {
    public static void main(String[] args) {
        JFrame fr = new JFrame("Automation");
        Screen sc = new Screen();
        fr.add(sc);
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.pack();
        fr.setVisible(true);

        fr.addMouseListener(sc);
    }
}
