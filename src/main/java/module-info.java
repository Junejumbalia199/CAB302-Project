module com.formcoach {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens com.formcoach to javafx.graphics;
    opens com.formcoach.landingpage to javafx.graphics;

    exports com.formcoach;
}