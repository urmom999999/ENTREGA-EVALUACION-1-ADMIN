module com.example.administracion_restaurante {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.dlsc.formsfx;

    opens com.example.administracion_restaurante to javafx.fxml;
    exports com.example.administracion_restaurante;
}