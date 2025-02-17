package authserver;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class AuthServer {
    private static final int PUERTO = 5000;

    // Hashmap del profe
    private static final HashMap<String, String> credenciales = new HashMap<>();

    private static final String[] usuarios = {"user01", "user02", "user03"};
    private static final int[] intentosFallidos = new int[usuarios.length];
    private static final long[] tiempoBloqueo = new long[usuarios.length];
    private static final boolean[] bloqueados = new boolean[usuarios.length]; 

    static {
        credenciales.put("user01", "one.Password");
        credenciales.put("user02", "two.Password");
        credenciales.put("user03", "three.Password");
    }

    public static void main(String[] args) {
        System.out.println("Servidor de autenticación iniciado en el puerto " + PUERTO);

        try (ServerSocket socketServidor = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socketCliente = socketServidor.accept();
                System.out.println("Nuevo cliente conectado desde " +
                        socketCliente.getInetAddress().getHostAddress() + ":" + socketCliente.getPort());

                new Thread(new VerificarCliente(socketCliente)).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static class VerificarCliente implements Runnable {
        private final Socket socket;

        public VerificarCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

                String nombreUsuario;
                int indiceUsuario;
                int intentos = 0;

                while (intentos < 3) {
                    salida.println("Ingrese su usuario:");
                    nombreUsuario = entrada.readLine();
                    indiceUsuario = obtenerIndiceUsuario(nombreUsuario);

                    if (indiceUsuario == -1) {
                        salida.println("Usuario no encontrado.");
                        continue;
                    }

                    synchronized (tiempoBloqueo) {
                        if (bloqueados[indiceUsuario] && tiempoBloqueo[indiceUsuario] > System.currentTimeMillis()) {
                            long tiempoRestante = (tiempoBloqueo[indiceUsuario] - System.currentTimeMillis()) / 1000;
                            salida.println("Demasiados intentos fallidos. Intente de nuevo en " + tiempoRestante + " segundos.");
                            continue;
                        } else if (bloqueados[indiceUsuario]) {
                            bloqueados[indiceUsuario] = false; 
                            intentosFallidos[indiceUsuario] = 0;
                        }
                    }

                    salida.println("Ingrese su contraseña:");
                    String contrasena = entrada.readLine();

                    if (credenciales.get(nombreUsuario).equals(contrasena)) {
                        salida.println("Acceso permitido.");
                        System.out.println("Cliente autenticado: " + nombreUsuario);
                        return;
                    } else {
                        intentos++;
                        if (indiceUsuario != -1) {
                            synchronized (intentosFallidos) {
                                intentosFallidos[indiceUsuario]++;
                            }
                        }
                        salida.println("Acceso denegado. Intento " + intentos + " de 3.");
                        System.out.println("Intento fallido de " + nombreUsuario + " desde " + socket.getInetAddress());

                        if (intentosFallidos[indiceUsuario] >= 3) {
                            synchronized (tiempoBloqueo) {
                                tiempoBloqueo[indiceUsuario] = System.currentTimeMillis() + 30000; // Bloqueo por 30 segundos
                                bloqueados[indiceUsuario] = true; // Marcar al usuario como bloqueado
                            }
                            salida.println("Usuario bloqueado por 30 segundos.");
                            return;
                        }
                    }
                }

            } catch (IOException e) {
                System.err.println("Error con el cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    System.out.println("Conexión cerrada con el cliente.");
                } catch (IOException e) {
                    System.err.println("Error al cerrar conexión: " + e.getMessage());
                }
            }
        }

        private int obtenerIndiceUsuario(String nombreUsuario) {
            for (int i = 0; i < usuarios.length; i++) {
                if (usuarios[i].equals(nombreUsuario)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
