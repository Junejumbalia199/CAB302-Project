module com.formcoach {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;

    opens com.formcoach to javafx.graphics;
    opens com.formcoach.landingpage to javafx.graphics;

    exports com.formcoach;
    exports kaggle;
}