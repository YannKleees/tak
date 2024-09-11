module com.example.tak {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.tak to javafx.fxml;
    exports com.example.tak;
}