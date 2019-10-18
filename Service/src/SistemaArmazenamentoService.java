
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

public class SistemaArmazenamentoService {

    public static void main(String[] args) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", args[0]);
        Servico servico = new Servico(args);

        servico.start();
    }
}
