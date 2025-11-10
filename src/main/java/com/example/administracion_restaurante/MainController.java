package com.example.administracion_restaurante;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class MainController {
    @FXML
    private Button button;
    @FXML
    private TextArea letra;
    @FXML
    private Text areaCompletar;
    @FXML
    private Text ResultTries;
    @FXML
    private Text letrasAcertadasId;
    @FXML
    private Text letrasFalladasId;
    @FXML
    private Text contador;

    private List<String> palabras = new ArrayList<>();
    private String[] dividida;
    private List<String> letrasFallidas = new ArrayList<>();
    private List<String> letrasAcertadas = new ArrayList<>();
    private int errores = 0;
    private int victorias = 0;
    private int derrotas = 0;
    private boolean acertado = false;
    private String textareaObtener;
    StringBuilder textooculto = new StringBuilder();

    @FXML
    private void initialize() {



    }






}