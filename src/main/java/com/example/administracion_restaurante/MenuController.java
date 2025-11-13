package com.example.administracion_restaurante;

import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MenuController {
    @FXML
    private Button ButtonServe1, ButtonServe2, ButtonServe3, ButtonServe4, ButtonServe5;
    @FXML
    private Button ButtonFree1, ButtonFree2, ButtonFree3, ButtonFree4, ButtonFree5;
    @FXML
    private Text TextFill1, TextFill2, TextFill3, TextFill4, TextFill5;
//guardar del 1 al 5
    private String[] mesaIds = new String[6];



    @FXML
    private void initialize() {
        mapearButton();
        cargarMesa();
    }
//CONFIGURAR CADA UNO CON EL NUMERO
//CARGAR MESAS SERVER

    private void mapearButton() {
        ButtonFree1.setOnAction(event -> liberarMesa(1));
        ButtonFree2.setOnAction(event -> liberarMesa(2));
        ButtonFree3.setOnAction(event -> liberarMesa(3));
        ButtonFree4.setOnAction(event -> liberarMesa(4));
        ButtonFree5.setOnAction(event -> liberarMesa(5));

        ButtonServe1.setOnAction(event -> servirMesa(1));
        ButtonServe2.setOnAction(event -> servirMesa(2));
        ButtonServe3.setOnAction(event -> servirMesa(3));
        ButtonServe4.setOnAction(event -> servirMesa(4));
        ButtonServe5.setOnAction(event -> servirMesa(5));
    }

    private void cargarMesa(){


    };
    private void liberarMesa(int numeroMesa) {


    }
    private void servirMesa(int numeroMesa) {


    }
//ACTUALIZAR TextFill!!



}