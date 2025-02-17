package authclient;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AuthClient {
    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int SERVER_PORT = 5555;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado al servidor de autenticación.");

            while (true) {
                System.out.println(input.readLine()); 
                String username = scanner.nextLine();
                output.println(username);

                System.out.println(input.readLine()); 
                String password = scanner.nextLine();
                output.println(password);

                String response = input.readLine();
                System.out.println("Servidor " + response);

                if (response.equals("Acceso permitido.") || response.contains("bloqueado")) {
                    break;
                }
           }
       } catch (IOException e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }
}

