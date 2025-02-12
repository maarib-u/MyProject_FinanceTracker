module com.example.financetracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;
    requires java.net.http;
    requires org.json;
    requires jbcrypt; // For URL and network connections


    opens com.example.financetracker to javafx.fxml;
    exports com.example.financetracker;
}