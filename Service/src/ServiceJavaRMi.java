
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ServiceJavaRMi extends UnicastRemoteObject implements RMIInterfaceSericoDirectoria{
    //Atributos
    private List<RmiInterfaceObserver> listaObservers = new ArrayList<>();
    private static final long serialVersionUID = 1L;
    
    //Construtor
    public ServiceJavaRMi() throws RemoteException{
        super();
    }
    
    //########################################################
    //#              METODOS JAVA RMI                        #
    //########################################################
    //Adicionar observer
    @Override
    public synchronized void addObserver(RmiInterfaceObserver remoteObserver){
        if(! this.listaObservers.contains(remoteObserver)){
            this.listaObservers.add(remoteObserver);
        }
    }
    
    //notifica todos os clientes 
    private synchronized void notifica(String info){
        for(int i = 0; i < this.listaObservers.size(); i++){
            try {
                this.listaObservers.get(i).notifyMonitor(info);
            }
            catch (RemoteException ex) {
                this.removeObserver(this.listaObservers.get(i));//Remover observer
                i--;
            }
        }
    }
    
    //Remover observer
    @Override
    public void removeObserver(RmiInterfaceObserver remoteObserver){
        this.listaObservers.remove(remoteObserver);
    }
    
    //troca de informacao entre o Server e o servico
    @Override
    public synchronized void updateServicoServidor(String informacaoDoServidor)throws java.rmi.RemoteException{
        this.notifica(informacaoDoServidor);
    }
}
