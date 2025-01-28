package org.example.chatapplication;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ClientController {

    @FXML
    private ImageView imageView;

    @FXML
    private TextArea txtArea;

    @FXML
    private TextField txtMessage;

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private FileChooser fileChooser;
    private File file;
    private String clientName;

    @FXML
    void btnSendOnAction(ActionEvent event) {
        String messageToSend = txtMessage.getText().trim();

        if (!messageToSend.isEmpty()) {
            try {
                dataOutputStream.writeUTF("text");
                dataOutputStream.writeUTF(clientName + ": " + messageToSend);
                dataOutputStream.flush();
                txtMessage.clear();
                appendMessage("You: " + messageToSend);
            } catch (IOException e) {
                appendMessage("Failed to send message: " + e.getMessage());
            }
        }
    }

    public void initialize() {
        clientName = "Client" + System.currentTimeMillis();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                socket = new Socket("localhost", 5000);
                appendMessage("Connected to server as " + clientName);

                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                dataOutputStream.writeUTF("name");
                dataOutputStream.writeUTF(clientName);
                dataOutputStream.flush();

                listenForMessages();
            } catch (IOException | InterruptedException e) {
                appendMessage("Failed to connect to the server: " + e.getMessage());
            }
        }).start();
    }

    private void listenForMessages() {
        try {
            while (true) {
                String messageType = dataInputStream.readUTF();
                switch (messageType) {
                    case "text":
                        handleTextMessage();
                        break;
                    case "file":
                        handleFileMessage();
                        break;
                    case "image":
                        handleImageMessage();
                        break;
                    default:
                        appendMessage("Unknown message type received.");
                        break;
                }
            }
        } catch (IOException e) {
            appendMessage("Disconnected from server: " + e.getMessage());
        }
    }

    private void handleTextMessage() {
        try {
            String message = dataInputStream.readUTF();
            appendMessage(message);
        } catch (IOException e) {
            appendMessage("Error receiving text message: " + e.getMessage());
        }
    }

    private void handleImageMessage() {
        try {
            int length = dataInputStream.readInt();
            if (length > 0) {
                byte[] bytes = new byte[length];
                dataInputStream.readFully(bytes);
                Image image = new Image(new ByteArrayInputStream(bytes));
                Platform.runLater(() -> imageView.setImage(image));
                appendMessage("Image received.");
            }
        } catch (IOException e) {
            appendMessage("Error receiving image: " + e.getMessage());
        }
    }

    private void handleFileMessage() {
        try {
            String fileName = dataInputStream.readUTF();
            long fileSize = dataInputStream.readLong();
            File receivedFile = new File("Received_" + fileName);

            try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;

                while (totalRead < fileSize) {
                    int bytesRead = dataInputStream.read(buffer);
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }
            appendMessage("File received: " + receivedFile.getAbsolutePath());
        } catch (IOException e) {
            appendMessage("Error receiving file: " + e.getMessage());
        }
    }


    @FXML
    public void btnFileChooserOnAction(ActionEvent event) {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Zip files", "*.zip"));
        file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            sendFile();
        }
    }

    @FXML
    public void btnImageChooserOnAction(ActionEvent event) {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg"));
        file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            sendImage();
        }
    }

    private void sendFile() {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            dataOutputStream.writeUTF("file");
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            dataOutputStream.flush();
            appendMessage("File sent: " + file.getName());
        } catch (IOException e) {
            appendMessage("Error sending file: " + e.getMessage());
        }
    }

    private void sendImage() {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = fileInputStream.readAllBytes();

            dataOutputStream.writeUTF("image");
            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
            dataOutputStream.flush();

            appendMessage("Image sent: " + file.getName());
        } catch (IOException e) {
            appendMessage("Error sending image: " + e.getMessage());
        }
    }

    private void appendMessage(String message) {
        Platform.runLater(() -> txtArea.appendText("\n" + message));
    }
}
