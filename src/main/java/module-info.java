module com.formcoach {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.formcoach to javafx.graphics;
    opens com.formcoach.landingpage to javafx.graphics;
    opens com.formcoach.auth to javafx.fxml;

    exports com.formcoach;
    exports kaggle;
    exports com.formcoach.auth;
}