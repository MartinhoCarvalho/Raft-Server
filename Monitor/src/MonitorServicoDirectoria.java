
import java.rmi.RemoteException;


public class MonitorServicoDirectoria {
     
    public static void main(String [] args) {
        try{
            if (args.length < 2) {
                System.err.println("Erro sintax errada<IP servico de directoria>");
                System.exit(-1);
            }
            
            System.setProperty("java.rmi.server.hostname",args[1]);

            Monitor monitor = new Monitor(args);
            printInformacao();
            monitor.run(); 
        }
        catch(RemoteException ob){
            System.out.println("O servico nao foi encontrado");
        }
    }
    
    private static void printInformacao(){
     System.out.println("+-------------------------------------------------------+\n" +
                        "|                   MONITOR RMI                         |\n" +
                        "+-------------------------------------------------------+\n\n");
    }
}
