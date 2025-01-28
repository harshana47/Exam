module org.example.chatapplication {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.chatapplication to javafx.fxml;
    exports org.example.chatapplication;
}