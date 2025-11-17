package com.example.administracion_restaurante;
//Actualizar cada poco
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MenuController {
    @FXML
    public Circle m11,m12,m13,m14,m21,m22,m23,m24,m31,m32,m33,m34,m41,m42,m43,m44,m51,m52,m53,m54;
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


    private Timeline timeline;
    @FXML
    private void initialize() {
        mapearButton();
        cargarMesas();
        conectarWebSocket();
        iniciarActualizacionAutomatica();
    }
    //ACTUALIZAR CADA 2 SEGUNDOS
    private void iniciarActualizacionAutomatica() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            cargarMesas();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); // Ejecutar indefinidamente
        timeline.play();
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
                .url("ws://localhost:5000?type=admin")
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

                System.out.println("=== INICIANDO SERVICIO MESA " + numeroMesa + " ===");
                System.out.println("Clientes a servir: " + clientes.length());

                // PRIMERO: Cambiar estado de los clientes a "servido"
                for (int i = 0; i < clientes.length(); i++) {
                    JSONObject clienteObj = clientes.getJSONObject(i);
                    String clienteId = clienteObj.getString("_id");
                    String estadoActual = clienteObj.getString("estado");
                    System.out.println("Cliente " + clienteId + " - Estado actual: " + estadoActual);

                    servirClienteEnServidor(clienteId);
                }

                // SEGUNDO: Enviar mensaje WebSocket a TODOS los clientes
                for (int i = 0; i < clientes.length(); i++) {
                    JSONObject clienteObj = clientes.getJSONObject(i);
                    String clienteId = clienteObj.getString("_id");
                    enviarMensajeWebSocket(clienteId, numeroMesa);
                }

                // TERCERO: Limpiar pedidos de los clientes
                for (int i = 0; i < clientes.length(); i++) {
                    JSONObject clienteObj = clientes.getJSONObject(i);
                    String clienteId = clienteObj.getString("_id");
                    limpiarPedidosCliente(clienteId);
                }

                // VERIFICAR: Obtener el estado actualizado de la mesa
                String mesaActualizadaData = getMesaFromServer(mesaId);
                JSONObject mesaActualizada = new JSONObject(mesaActualizadaData);
                String estadoFinal = mesaActualizada.getString("estado");

                System.out.println("=== SERVICIO COMPLETADO MESA " + numeroMesa + " ===");
                System.out.println("Estado final de la mesa: " + estadoFinal);

                javafx.application.Platform.runLater(() -> {
                    actualizarTextFill(numeroMesa, "servido", clientes.length());
                    System.out.println("✓ UI actualizada - Mesa " + numeroMesa + " marcada como SERVIDA");
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("✗ Error en servirMesa: " + e.getMessage());
            }
        }).start();
    }
    private void limpiarPedidosCliente(String clienteId) {
        try {
            URL url = new URL("http://localhost:5000/clientes/" + clienteId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = "{\"pedidos\": [], \"totalPedidos\": 0}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            if (code == 200) {
                System.out.println("Pedidos limpiados para cliente: " + clienteId);
            } else {
                System.out.println("Error limpiando pedidos: " + code);
            }
        } catch (Exception e) {
            System.err.println("Error en limpiarPedidosCliente: " + e.getMessage());
        }
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
        Circle[] circulosMesa = getCirculosByMesa(numeroMesa);

        if (circulosMesa != null) {
            // Resetear todos los círculos de la mesa
            for (Circle circle : circulosMesa) {
                circle.setFill(javafx.scene.paint.Color.GRAY); // Color por defecto
            }

            // Colorear solo los círculos de clientes activos
            for (int i = 0; i < numClientes && i < 4; i++) {
                Color color = getColorByEstado(estado);
                circulosMesa[i].setFill(color);
            }
        }

    }
    private Circle[] getCirculosByMesa(int numeroMesa) {
        switch (numeroMesa) {
            case 1: return new Circle[]{m11, m12, m13, m14};
            case 2: return new Circle[]{m21, m22, m23, m24};
            case 3: return new Circle[]{m31, m32, m33, m34};
            case 4: return new Circle[]{m41, m42, m43, m44};
            case 5: return new Circle[]{m51, m52, m53, m54};
            default: return null;
        }
    }
    private Color getColorByEstado(String estado) {
        switch (estado) {
            case "libre":
                return Color.GREEN;
            case "ocupada":
                return Color.ORANGE;
            case "esperando":
                return Color.RED;
            case "pidiendo":
                return Color.YELLOW;
            case "servido":
                return Color.BLUE;
            default:
                return Color.GRAY;
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