package org.example.chatapplication;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerController {

    @FXML
    private ImageView imageView;
    @FXML
    private TextArea txtArea;
    @FXML
    private TextField txtMessage;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private FileChooser fileChooser;
    private File file;

    private static final int PORT = 5000;

    public void initialize() {
        executorService = Executors.newCachedThreadPool();

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                txtArea.setText("Server started on port " + PORT);

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    txtArea.appendText("\nNew client connected: " + clientSocket.getInetAddress());
                    executorService.submit(new ClientHandler(clientSocket));
                }
            } catch (IOException e) {
                txtArea.appendText("\nServer error: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    void btnSendOnAction(ActionEvent event) {
        String messageToSend = txtMessage.getText().trim();

        if (!messageToSend.isEmpty()) {
            try {
                broadcastMessage("text", messageToSend);
                txtMessage.clear();
                txtArea.appendText("\nSent: " + messageToSend);
            } catch (IOException e) {
                txtArea.appendText("\nFailed to send message: " + e.getMessage());
            }
        }
    }

    @FXML
    void btnFileChooserOnAction(ActionEvent event) {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            try {
                sendFile();
            } catch (IOException e) {
                txtArea.appendText("\nFailed to send file: " + e.getMessage());
            }
        }
    }

    @FXML
    void btnImageChooserOnAction(ActionEvent event) {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            try {
                sendImage();
            } catch (IOException e) {
                txtArea.appendText("\nFailed to send image: " + e.getMessage());
            }
        }
    }

    private void broadcastMessage(String type, String message) throws IOException {
        for (ClientHandler clientHandler : ClientHandler.clientHandlers) {
            if (type.equals("text")) {
                clientHandler.sendTextMessage(message);
            }
        }
    }

    private void sendImage() throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.read(bytes);
            for (ClientHandler clientHandler : ClientHandler.clientHandlers) {
                clientHandler.sendImage(file.getName(), bytes);
            }
            txtArea.appendText("\nImage sent: " + file.getName());
        }
    }

    private void sendFile() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            for (ClientHandler clientHandler : ClientHandler.clientHandlers) {
                clientHandler.sendFile(file, fileInputStream);
            }
            txtArea.appendText("\nFile sent: " + file.getName());
        }
    }

    private static class ClientHandler implements Runnable {
        private static final List<ClientHandler> clientHandlers = new CopyOnWriteArrayList<>();
        private final Socket socket;
        private final DataInputStream dataInputStream;
        private final DataOutputStream dataOutputStream;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            clientHandlers.add(this);
        }

        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
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
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void handleTextMessage() throws IOException {
            String message = dataInputStream.readUTF();
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.sendTextMessage(message);
            }
        }

        private void handleFileMessage() throws IOException {
            String fileName = dataInputStream.readUTF();
            long fileSize = dataInputStream.readLong();
            File receivedFile = new File("Received_" + fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(receivedFile)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                while (totalRead < fileSize) {
                    int bytesRead = dataInputStream.read(buffer);
                    totalRead += bytesRead;
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File received: " + fileName);
        }

        private void handleImageMessage() throws IOException {
            String fileName = dataInputStream.readUTF();
            int length = dataInputStream.readInt();
            byte[] bytes = new byte[length];
            dataInputStream.readFully(bytes);

            File receivedImage = new File("Received_" + fileName);
            try (FileOutputStream fileOutputStream = new FileOutputStream(receivedImage)) {
                fileOutputStream.write(bytes);
            }

            System.out.println("Image received: " + fileName);
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.sendImage(fileName, bytes);
            }
        }

        private void sendTextMessage(String message) throws IOException {
            dataOutputStream.writeUTF("text");
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();
        }

        private void sendFile(File file, FileInputStream fileInputStream) throws IOException {
            dataOutputStream.writeUTF("file");
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            dataOutputStream.flush();
        }

        private void sendImage(String fileName, byte[] bytes) throws IOException {
            dataOutputStream.writeUTF("image");
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.writeInt(bytes.length);
            dataOutputStream.write(bytes);
            dataOutputStream.flush();
        }

        private void closeConnection() {
            clientHandlers.remove(this);
            try {
                dataInputStream.close();
                dataOutputStream.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}