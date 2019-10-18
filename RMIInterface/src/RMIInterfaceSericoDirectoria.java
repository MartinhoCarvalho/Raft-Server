
public interface RMIInterfaceSericoDirectoria extends java.rmi.Remote{
    public void addObserver(RmiInterfaceObserver remoteObserver) throws java.rmi.RemoteException;
    public void removeObserver(RmiInterfaceObserver remoteObserver) throws java.rmi.RemoteException;
    public void updateServicoServidor(String informacaoDoServidor) throws java.rmi.RemoteException;
}
