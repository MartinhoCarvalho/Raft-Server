
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Monitor extends UnicastRemoteObject implements RmiInterfaceObserver {

    //Atributos
    private static final long serialVersionUID = 1L;
    private String[] args;
    private String nomeResgito;
    private RMIInterfaceSericoDirectoria servico;
    private static final String SERVICE_NAME = "GetRemoteFile";
    private String myIp;

    //Construtor
    public Monitor(String[] args) throws RemoteException {
        this.args = args;
    }

    //METODOS
    public void run() {
        if (this.args.length < 2) {
            System.err.println("Erro sintax errada<IP servico de directoria>");
            System.exit(-1);
        }
         
        String  objectUrl = "//"+args[0]+"/ServicoMonitor";
        try {
            
//          Registry registry = LocateRegistry.getRegistry(args[0]);
            this.servico = (RMIInterfaceSericoDirectoria) Naming.lookup(objectUrl);
            servico.addObserver(this);
  
            System.in.read();
            
            this.servico.removeObserver(this);
            UnicastRemoteObject.unexportObject(this, true);
        } 
        catch (MalformedURLException ex) {
            System.err.println("Erro ao fazer  Naming");
            System.exit(-1);
        }
        catch (RemoteException ex) {
            System.err.println("Servico nao foi encontrado ");
            System.exit(-1);
        }
        catch (IOException ex) {
            System.out.println("400");
        } 
        catch (NotBoundException ex) {
            System.err.println("Erro ao fazer lookup");
            System.exit(-1);
        }
    }
    
    //Notificar servico que vou sair
    public void exitServico() throws RemoteException{
        try {
            servico.removeObserver(this);
            UnicastRemoteObject.unexportObject(this, true);
        } 
        catch (RemoteException ex) {
            System.err.println("Erro exitServico");
        }
    }

    //########################################################
    //#              METODOS JAVA RMI                        #
    //########################################################
    @Override
    public void notifyMonitor(String description) throws RemoteException{
        System.out.println("---------------------------------------------------------");
        System.out.println(description);
        System.out.println("---------------------------------------------------------");
    }
}

 //String objectUrl = "rmi://"+args[0]+"/ServicoMonitor"; 
