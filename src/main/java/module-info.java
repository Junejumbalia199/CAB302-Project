/**
 * FormCoach application module.
 * Declares all JavaFX, database, and third-party dependencies and exports
 * the packages required by the JavaFX runtime and other modules.
 */
module com.formcoach {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.swing;
    requires java.desktop;
    requires java.net.http;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires io.github.cdimascio.dotenv.java;
    requires org.json;
    requires webcam.capture;

    opens com.formcoach to javafx.graphics;
    opens com.formcoach.landingpage to javafx.graphics;
    opens com.formcoach.auth to javafx.fxml;

    exports com.formcoach;
    exports kaggle;
    exports com.formcoach.auth;
    exports com.formcoach.profile;
    exports com.formcoach.chatbot;
    opens com.formcoach.profile to javafx.fxml;
    exports com.formcoach.textoutputgen;
    opens com.formcoach.textoutputgen to javafx.graphics;
}