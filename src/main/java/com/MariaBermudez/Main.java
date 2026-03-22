package com.MariaBermudez;

import com.MariaBermudez.GUI.VentanaGrafica;
import javax.swing.SwingUtilities;

public class  Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaGrafica().setVisible(true);
        });
    }
}