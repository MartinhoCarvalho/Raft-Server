
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;


public class SistemaArmazenamentoCliente {

    public static void main(String[] args) {
     
        Client cliente = new Client(args);
        
        cliente.Run();   
    } 
}
