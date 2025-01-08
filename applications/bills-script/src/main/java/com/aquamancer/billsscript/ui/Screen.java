package com.aquamancer.billsscript.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Screen extends JPanel implements MouseListener, ActionListener {
    private int status;
    public Screen() {

    }
    public Dimension getPreferredSize() {
        return new Dimension(1024, 768);
    }
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
