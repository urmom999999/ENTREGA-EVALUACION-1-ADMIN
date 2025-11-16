package com.example.administracion_restaurante;

import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MenuController {
    @FXML
    private Button ButtonServe1, ButtonServe2, ButtonServe3, ButtonServe4, ButtonServe5;
    @FXML
    private Button ButtonFree1, ButtonFree2, ButtonFree3, ButtonFree4, ButtonFree5;
    @FXML
    private Text TextFill1, TextFill2, TextFill3, TextFill4, TextFill5;

    private String[] mesaIds = {
            "6913f603bd20090f1876023c", // Mesa 1
            "6913f603bd20090f1876023d", // Mesa 2
            "6913f603bd20090f1876023e", // Mesa 3
            "6913f603bd20090f1876023f", // Mesa 4
            "6913f603bd20090f18760240"  // Mesa 5
    };
    private okhttp3.WebSocket webSocket;
    private okhttp3.OkHttpClient client;

    @FXML
    private void initialize() {
        mapearButton();
        cargarMesas();
        conectarWebSocket();
    }

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

    private void cargarMesas() {
        new Thread(() -> {
            try {
                String response = getMesasFromServer();
                JSONArray mesas = new JSONArray(response);

                for (int i = 0; i < mesas.length(); i++) {
                    JSONObject mesa = mesas.getJSONObject(i);
                    int numeroMesa = mesa.getInt("numero");
                    String estado = mesa.getString("estado");
                    JSONArray clientes = mesa.getJSONArray("clientes");

                    int finalI = i;
                    javafx.application.Platform.runLater(() -> {
                        actualizarTextFill(numeroMesa, estado, clientes.length());
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void liberarMesa(int numeroMesa) {
        new Thread(() -> {
            try {
                String mesaId = mesaIds[numeroMesa - 1];
                String response = liberarMesaEnServidor(mesaId);

                javafx.application.Platform.runLater(() -> {
                    actualizarTextFill(numeroMesa, "libre", 0);
                    System.out.println("Mesa " + numeroMesa + " liberada: " + response);
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    System.out.println("Error liberando mesa " + numeroMesa + ": " + e.getMessage());
                });
            }
        }).start();
    }
    private void conectarWebSocket() {
        client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("ws://localhost:8080?type=admin")
                .build();

        webSocket = client.newWebSocket(request, new okhttp3.WebSocketListener() {
            @Override
            public void onOpen(okhttp3.WebSocket webSocket, okhttp3.Response response) {
                System.out.println("Conectado al servidor WebSocket (Admin)");
            }

            @Override
            public void onMessage(okhttp3.WebSocket webSocket, String text) {
                System.out.println("Mensaje recibido: " + text);
            }

            @Override
            public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
                System.out.println("Conexión WebSocket cerrada: " + reason);
            }

            @Override
            public void onFailure(okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
                System.err.println("Error WebSocket: " + t.getMessage());
            }
        });
    }
    private void servirMesa(int numeroMesa) {
        new Thread(() -> {
            try {
                String mesaId = mesaIds[numeroMesa - 1];
                String mesaData = getMesaFromServer(mesaId);
                JSONObject mesa = new JSONObject(mesaData);
                JSONArray clientes = mesa.getJSONArray("clientes");

                for (int i = 0; i < clientes.length(); i++) {
                    JSONObject clienteObj = clientes.getJSONObject(i);
                    String clienteId = clienteObj.getString("_id");
                    servirClienteEnServidor(clienteId);

                    // SOLO ESTA LÍNEA:
                    enviarMensajeWebSocket(clienteId, numeroMesa);
                }

                // Enviar mensaje via WebSocket en lugar de Intent
                javafx.application.Platform.runLater(() -> {
                    actualizarTextFill(numeroMesa, "servido", clientes.length());
                    System.out.println("Mesa " + numeroMesa + " servida");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void enviarMensajeWebSocket(String clienteId, int numeroMesa) {
        try {
            JSONObject message = new JSONObject();
            message.put("type", "DISMISS_OVERLAY");
            message.put("targetClientId", clienteId);
            message.put("mesaId", numeroMesa);

            if (webSocket != null) {
                webSocket.send(message.toString());
                System.out.println("Mensaje WebSocket enviado para cliente: " + clienteId);
            }
        } catch (Exception e) {
            System.err.println("Error enviando mensaje WebSocket: " + e.getMessage());
        }
    }
    public void cerrarConexion() {
        if (webSocket != null) {
            webSocket.close(1000, "Aplicación cerrada");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }

//CAMBIAR COLOR CIRCULO
    private void actualizarTextFill(int numeroMesa, String estado, int numClientes) {
        Text textFill = getTextFillByNumber(numeroMesa);
        if (textFill != null) {
            String texto = "Mesa " + numeroMesa + " - " + estado.toUpperCase() +
                    " - Clientes: " + numClientes;
            textFill.setText(texto);

            // Cambiar color según estado
            switch (estado) {
                case "libre":
                    textFill.setFill(Color.GREEN);
                    break;
                case "ocupada":
                    textFill.setFill(Color.ORANGE);
                    break;
                case "servido":
                    textFill.setFill(Color.BLUE);
                    break;
                case "esperando-pedido":
                    textFill.setFill(Color.RED);
                    break;
                default:
                    textFill.setFill(Color.BLACK);
            }
        }
    }

    private Text getTextFillByNumber(int numero) {
        switch (numero) {
            case 1: return TextFill1;
            case 2: return TextFill2;
            case 3: return TextFill3;
            case 4: return TextFill4;
            case 5: return TextFill5;
            default: return null;
        }
    }

    // ========== MÉTODOS DE CONEXIÓN CON EL SERVIDOR ==========

    private String getMesasFromServer() {
        try {
            URL url = new URL("http://localhost:5000/mesas");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int code = connection.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                throw new RuntimeException("HTTP error code: " + code);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo mesas: " + e.getMessage());
        }
    }

    private String getMesaFromServer(String mesaId) {
        try {
            URL url = new URL("http://localhost:5000/mesas/" + mesaId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int code = connection.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                throw new RuntimeException("HTTP error code: " + code);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo mesa: " + e.getMessage());
        }
    }

    private String liberarMesaEnServidor(String mesaId) {
        try {
            URL url = new URL("http://localhost:5000/mesas/" + mesaId + "/liberar");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int code = connection.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                throw new RuntimeException("HTTP error code: " + code);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error liberando mesa: " + e.getMessage());
        }
    }

    private void servirClienteEnServidor(String clienteId) {
        try {
            URL url = new URL("http://localhost:5000/clientes/" + clienteId + "/estado");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            String jsonInputString = "{\"estado\": \"servido\"}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("HTTP error code: " + code);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error sirviendo cliente: " + e.getMessage());
        }
    }
}