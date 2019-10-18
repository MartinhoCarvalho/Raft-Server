
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servico {
    //Atributos
    private DatagramSocket soket = null;
    private DatagramPacket packet = null;
    private MulticastSocket multicastGroup = null;
    private int servicePort = 5001;
    private Boolean condicaoParagem = true;
    private static final int MAXSIZEFILE = 4086, Timeout = 50000;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private String ficheiroLogins = "loginFiles.txt";
    private InetAddress group;
    private int serviceGroupport;
    private Thread thread = null, threadServico, threadPeriodos = null;
    private removeServidores threadRemove = null;
    private Runnable runThread = null, runServico = null, runPeriodos = null;
    private List<DadosDoServidor> listaDeServidores = null;
    private int contaPeriodos = 0;
    private List<Login> listaDeUtilzadores = null;
    private ServiceJavaRMi servicoMonitor = null;
    private Registry r;
    private static final String RMI_ADRESS = "ServicoMonitor";
    private String [] args;
    //Construtor por  defeito
    public Servico(String [] args) throws RemoteException {
        this.args = args;
        this.inicializarGroupDosServidores();
    }

    //##########################    METODOS     ###########################################
    private void inicializarGroupDosServidores() {
        try {
            if(this.args.length < 1){
                System.err.println("Sintax errada <Ipv4 address>");
                System.exit(-1);
            }
            
            this.listaDeUtilzadores = new ArrayList<Login>();
            this.listaDeServidores = new ArrayList<>();
            this.group = InetAddress.getByName("225.15.15.15");
            this.serviceGroupport = 7000;

            this.multicastGroup = new MulticastSocket(this.serviceGroupport);
            //this.multicastGroup.setNetworkInterface(NetworkInterface.getByName("en0"));
            this.multicastGroup.joinGroup(group);
            //Iniciar thread
            this.runThread = new ThreadHeartbeats(multicastGroup, this);
            this.thread = new Thread(runThread);

            this.thread.start();
            //criar socket para clientes
            this.soket = new DatagramSocket(servicePort);
            //Criar pakcet
            this.packet = new DatagramPacket(new byte[Servico.MAXSIZEFILE], Servico.MAXSIZEFILE);
            //Remove servidores
            this.threadRemove = new removeServidores(this);
            this.threadRemove.start();
            this.start();

        } catch (UnknownHostException ob) {
            System.err.println("Erro ao criar grupo dos Heartbeats");
        } catch (SocketException ob) {
            System.err.println("Error create socket in service directory");
            System.err.println("The service is finish");
            System.exit(-1);
        } catch (IOException ob) {
            System.err.println("Erro ao criar socket do grupo Heartbeats");
        }
    }

    //colocar em modo daemon
    public void start() {

        this.runServico = new TreadRunServico(this);
        try {
            this.servicoMonitor = new ServiceJavaRMi(); 
            r = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

            r.bind(RMI_ADRESS, this.servicoMonitor);
        } 
        catch (RemoteException e) {
            try {
                this.r = LocateRegistry.getRegistry();
            } 
            catch (RemoteException ex1) {
                System.err.println("Nao consegue localizar o registo");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } 
        this.run();
    }
   
    //servico run
    private void run() {
        this.atendeClientes();
    }

    //########################################################
    //#              METODOS PARA ATENDER CLIENTE            #
    //########################################################
      //Atende clientes
    private void atendeClientes() {
        while (this.condicaoParagem == true) {
            try {
                this.packet.setData(new byte[Servico.MAXSIZEFILE], 0, Servico.MAXSIZEFILE);
                this.soket.receive(this.packet);
                Login logo = this.atendeCliente(this.packet);
                
                if(logo.getLogout().contains("Logout") == false){
                    this.respondeCliente(logo,this.packet);
                }
                else{//remover cliente
                    removerCliente(logo);
                }
            } 
            catch (IOException Ob) {
                System.err.println("Error receive packet from client");
            }
        }
    }
    
    private synchronized void removerCliente(Login logo){
        comparatorLogin comp = new comparatorLogin();
        for(int i = 0; i < this.listaDeUtilzadores.size(); i++){
            if(comp.compare(logo, this.listaDeUtilzadores.get(i)) == 0){
                this.listaDeUtilzadores.remove(i);//remover este cliente
            }
        }
    }
    
    //atender um cliente
    private Login atendeCliente(DatagramPacket pkt) {
        Object login;
        
        try{
          this.in = new ObjectInputStream(new ByteArrayInputStream(pkt.getData(),0,pkt.getLength()));
          login = this.in.readObject();
          
          if(login instanceof Login){//se for login vai tratar do login do cliente
              //Tratar dos dados do Cliente
              Login ob = (Login)login;
                            
              this.trataEstadoDoCliente(ob);
              return ob;
          }
        }
        catch(ClassNotFoundException ob){
            System.err.println("Erro nnao e um objeto da class login");
        }
        catch(IOException ob){
            System.err.println("Erro ao criar um ObjectInputStream");
            return null;
        }
        Login loginDefualt = new Login(" ", " ");
        
        return loginDefualt;
    }
    
    //responde a um cliente
    private  void respondeCliente(Login login, DatagramPacket pkt){
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try{
            out = new ObjectOutputStream(buff);
            out.writeObject(login);
            out.flush();
            out.close();

            pkt.setData(buff.toByteArray());
            pkt.setLength(buff.size());

            this.soket.send(pkt);
        }
        catch(IOException ob){
            System.err.println("Error create ObjectOutputStream");
        }
    }
     
    //estado cliente
    private  void trataEstadoDoCliente(Login login){
        if(login.getEstado() instanceof AccountNormal){//vai tratar de umas das opcoes
            this.opcaoDoCliente(login);
        }
        else if(login.getEstado() instanceof CreateNewAccount){//Criar uma nova conta
            this.criarConta(login);
            login.setOpcao(3);
            this.opcaoDoCliente(login);
        }
        else if(login.getEstado() instanceof LoginAccount){//efetuar login
            if(this.lerficheiro(login) == true){
                login.setOpcao(3);
                this.opcaoDoCliente(login);
            }else{
                login.setOpcao(3);
                this.opcaoDoCliente(login);
            }
        }
    }
    
    //Tratar das opcoes do cliente
    private  void opcaoDoCliente(Login lg){
       int op = lg.getOpcao();
        
        if(op == 1){
            lg.mudaEstado();
        }
        else if(op == 2){//vai tratar dos dados do login
            lg.mudaEstado();
        }
        else{//voltar para o mesmo estado
            lg.mudaEstado();
        }
        
    }
    
    //ler ficheiro de logins
    private void criarConta(Login logo){
        this.criarFicheiroLogins();
        
        if(this.verificaSeONomeJaExiste(logo) == true){
            return;
        }
        try {
            BufferedReader read = new BufferedReader(new FileReader(this.ficheiroLogins));

            String sCurrentLine;
            try {
                //true = append file
                FileWriter fileWritter = new FileWriter(this.ficheiroLogins, true);
                BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                bufferWritter.write(logo.username + " " + logo.password + " " + "\n");
                bufferWritter.close();
            } 
            catch (IOException ex) {
                System.err.println("Erro ao ler ficheiro");
            }
        } 
        catch (FileNotFoundException ob) {
            System.err.println("Erro nao encontrou ficheiro dos logins");
        }
    }
    
    //criar ficheiro logins caso nao exista
    private void criarFicheiroLogins(){
        File file = new File(this.ficheiroLogins);

        // if file doesnt exists, then create it
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                System.err.println("Erro ao criar ficheiro");
            }
        }
    }
    
    //verificar se o nome ja existe
    private Boolean verificaSeONomeJaExiste(Login logo){
        this.criarFicheiroLogins();
        boolean autenticacao = false;
        
        BufferedReader br = null;
        try {
            String sCurrentLine;

            br = new BufferedReader(new FileReader(this.ficheiroLogins));

            while ((sCurrentLine = br.readLine()) != null) {
                String [] vec = sCurrentLine.split(" ");
                autenticacao = this.verificaDadosLoginCriarConta(vec, logo);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return autenticacao;
    }
  
    //Ler ficheiro dos logins
    private Boolean lerficheiro(Login logo){
        this.criarFicheiroLogins();
        boolean autenticacao = false;
        
        BufferedReader br = null;
        try {
            String sCurrentLine;

            br = new BufferedReader(new FileReader(this.ficheiroLogins));

            while ((sCurrentLine = br.readLine()) != null) {
                String [] vec = sCurrentLine.split(" ");
                autenticacao = this.verificaDadosLogin(vec, logo);
            }

        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return autenticacao;
    }
    
    //vefirica se o cliente ja esta logado
    private Boolean getVerificaLogin(Login logo){
        comparatorLogin comp = new comparatorLogin();
        for(int i = 0; i < this.listaDeUtilzadores.size(); i++){
            if(comp.compare(logo, this.listaDeUtilzadores.get(i)) == 0){
                return true;
            }
        }
        return false;
    }
    
    //verifica se o nome existe
    private Boolean verificaDadosLogin(String[] vec, Login logo) {
        if (logo.username.equals(vec[0]) == true) {
            if (logo.password.equals(vec[1]) == true) {
                if (this.listaDeServidores.size() > 0) {//verificar se existe servidores ligado
                    if (this.escolheUmServidor(logo) == true) {
                        logo.setNaoExistemServidores(false);
                        logo.loginEfetuado();//logim realizado com sucesso
                        this.listaDeUtilzadores.add(logo);//cliente logado
                        logo.setNaoExisteNenhumServidorAtivo(false);
                        logo.setClienteJaEstaLogado(false);
                    } else {//Unico server que esta registado ja nao envia heartbeats a + de 7 segundos é preciso ter em conta isso
                        logo.setNaoExistemServidores(true);
                        logo.setOpcao(3);
                        logo.setNaoExisteNenhumServidorAtivo(true);
                        return false;
                    }
                } else {
                    logo.setNaoExistemServidores(true);
                    logo.setOpcao(3);
                    logo.setNaoExisteNenhumServidorAtivo(true);
                    return false;
                }
                return true;
            }
        }
        logo.setNaoExistemServidores(false);
        return false;
    }

     //verifica se o nome existe
    private Boolean verificaDadosLoginCriarConta(String [ ] vec, Login logo){
        if(logo.username.contains(vec[0]) == true){
            if(logo.password.contains(vec[1]) == true){
                return true;
            }
        }
        return false;
    }
       
    //############################################################
    //#                     THREAD RUN                           #
    //############################################################
    class TreadRunServico extends Thread{
        private Servico servico = null;
        
        
        public TreadRunServico(Servico sv){
            this.servico = sv;
        }

        @Override
        public void run(){
//            this.setDaemon(true);
            this.servico.run();
        }
    }
       
    //############################################################
    //#               THREAD HEARTBEATS                          #
    //############################################################
    class ThreadHeartbeats extends Thread{
        //Atributos
        private MulticastSocket socket     = null;
        private DatagramPacket pakcet      = null;
        private ByteArrayOutputStream buff = null;
        private ObjectOutputStream out     = null;
        private ObjectInputStream in       = null;
        private boolean running            = true;
        private Servico sv                 = null;
       
        //Construtor
        public ThreadHeartbeats(MulticastSocket s, Servico sv){
            this.socket = s;
            this.pakcet = new DatagramPacket(new byte[Servico.MAXSIZEFILE],Servico.MAXSIZEFILE);
            this.buff = new ByteArrayOutputStream();
            this.setDaemon(true) ;  
            this.sv = sv;
        }
       
        @Override
        public void run(){
            while(this.running){
                try {
                     //Recebe
                     Object obj = recebeHeartbeats(this.pakcet, this.in, this.socket);

                     if(obj instanceof DadosDoServidor){// quando obj for dados do servidor 
                         DadosDoServidor dados = (DadosDoServidor) obj;
                         
                         dados.setIp(this.pakcet.getAddress());              
                         //Adicionar servidor
                         addServidores(dados);
                         setPeriodo(dados);
                     }
                 }  
                 catch(ClassNotFoundException ob){
                     System.err.println("Erro class dount found");
                     continue;
                 }
                 catch (IOException ex) {
                     System.err.println("Erro ao receber Heartbeats");
                     continue;
                 }
             }
        }
    }
    
    //Receber hearbeats do servidor
    private Object recebeHeartbeats(DatagramPacket packet, ObjectInputStream in, DatagramSocket socket) throws ClassNotFoundException, IOException{
        //receber
        in = null;

        packet.setData(new byte[MAXSIZEFILE], 0, MAXSIZEFILE);

        socket.receive(packet);

        in = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
        Object obj = in.readObject();

        in.close();

        return obj;

    }
    
    //Para enviar heartbeats do servidor
     private /*synchronized*/ void enviarHeartbeats(DatagramPacket packet, ObjectOutputStream out, DatagramSocket socket, ByteArrayOutputStream buff ) throws ClassNotFoundException, IOException{
        buff = new ByteArrayOutputStream();
        out = new ObjectOutputStream(buff);
        out.writeObject("Sinal");
        out.flush();
        out.close();

        packet = new DatagramPacket(buff.toByteArray(), buff.size(), 
                                    group, serviceGroupport);

        socket.send(packet);    
     
     }
          
    //############################################################
    //#                     DadosDoServidor                      #
    //############################################################
    //Adicionar servidores 
    private void  addServidores(DadosDoServidor ob){
        boolean flag = false;

            comparatorServidor comp = new comparatorServidor();
            for (int i = 0; i < this.listaDeServidores.size(); i++) {
                if (comp.compare(this.listaDeServidores.get(i), ob) == 0) {
                    this.listaDeServidores.set(i, ob);
                    this.listaDeServidores.get(i).setTipoHearBeat(ob.getTipoHearBeat());
                    flag = true;
                }
            }
            
            if(flag == false){
                this.listaDeServidores.add(ob);
            }  
    }
    
    //remover servidores inativos
    private void removeServidor() {
        synchronized (this.listaDeServidores) {
            comparatorServidor comp = new comparatorServidor();
            int total = 0;
            for (int i = 0; i < this.listaDeServidores.size(); i++) {
                //Remover se o numero de hearbeats se o tempo for menor do que 15
                double tempo1 = System.currentTimeMillis();

                double tempofinal = (tempo1) - (this.listaDeServidores.get(i).getTime());

//                System.out.println("Tempo "+tempofinal+"N Hb " +" "+this.listaDeServidores.get(i).getIp().getHostAddress()+" pos "+ i);
                if (tempofinal > 7500) {
//                    System.out.println("##################################  Warning");
                    this.listaDeServidores.get(i).setWarning(true);
                } 
                else if (tempofinal < 7000) {
//                    System.out.println(" not Warning");
                    this.listaDeServidores.get(i).setWarning(false);
                }

                if (tempofinal > 15000) {//remove servidor aos 15 segundos == 3 periodos
                    synchronized (this.listaDeServidores.remove(i)) {
                    }
                }
            }
        }
    }   
    
    //THREAD remover servidores
    class removeServidores extends Thread{
        //Atributos
        private Servico sv = null;
        private Boolean run = true;
        
        
        //COntrutor
        public removeServidores(Servico s){
            this.sv  =  s;
        }
        
        @Override
        public void run(){
            while(this.run){
                try{
                    synchronized (listaDeServidores) {
                        if (listaDeServidores.size() > 0) {
                            sv.removeServidor();
                        }
                    }
                    
                    Thread.sleep(1000);//Espera um segundo
                }
                catch(InterruptedException ob){
                
                }
            }
        }
    }
    
    //escolher um servidor para um cliente
    private synchronized Boolean escolheUmServidor(Login utilizador){
        Boolean condicao = false;
        DadosDoServidor d = this.listaDeServidores.get(0);
        int menor = d.getNumeroClientes();
        
        for (DadosDoServidor ds : this.listaDeServidores) {
            if (ds.getNumeroClientes() <= menor && ds.getWarning() == false) {
                menor = ds.getNumeroClientes();
                d = ds;

                utilizador.setIp(d.getIp());
                utilizador.setPortoServidor(d.getServicePort());
                utilizador.setEstadoDoServidor(d.getEmConfiguracao());
                condicao = true;
            }
        }

        return condicao;
    }

    //############################################################
    //#               Comparator Login                           #
    //############################################################
    class comparatorLogin implements Comparator<Login>{

        @Override
        public int compare(Login o1, Login o2) {
            if(o1.username.equals(o2.username) == true){
                if(o1.password.equals(o2.password) == true){
                    return 0;// igual
                }
            }
            
            return 1;
        }
        
        
    }
    
    //############################################################
    //#               Comparator DadosDoServidor                 #
    //############################################################
    class comparatorServidor implements Comparator<DadosDoServidor>{
        
        @Override
        public int compare(DadosDoServidor d1, DadosDoServidor d2){
            int ip1 = 0, ip2 = 0;
            
               ip1 = gettotalIp(d1.getIp().getHostName());
            ip2 = gettotalIp(d2.getIp().getHostName());
            
            if(ip1 == ip2){
                //Comparar os portos
                if(d1.getServicePort() == d2.getServicePort()){
                    return 0;
                }
                else if (d1.getServicePort() > d2.getServicePort()){
                    return 1;
                }
                else{
                    return -1;
                }
            }
            else if(ip1 > ip2){
                return -1;
            }
            else{
                return 1;
            }
        }
    }
    
    //get total do ip
    private synchronized int gettotalIp(String ip){
        int total = 0;
        
        for(int i = 0; i < ip.length(); i++){
            if(ip.charAt(i)>= '0' && ip.charAt(i) <= '9'){
                total+=  Character.getNumericValue(ip.charAt(i));
            }
        }
      
        return total;
    }
     
    //########################################################
    //#              METODOS PARA CONTAR PERIODOS            #
    //########################################################
    private synchronized void setPeriodo(DadosDoServidor ob){
          comparatorServidor comp = new comparatorServidor();
        
        for(int i = 0; i < this.listaDeServidores.size(); i++){
            if(comp.compare(listaDeServidores.get(i),ob) == 0){
                if(this.listaDeServidores.size() > 0){
                    synchronized(this.listaDeServidores){
                      this.listaDeServidores.get(i).setTime(System.currentTimeMillis());
                    }
                }
            } 
        }
    }
    
    //Copiar os servidores
    private synchronized List<DadosDoServidor> getCopia(){
        List<DadosDoServidor> lt = new ArrayList<>();
        synchronized (this.listaDeServidores){
            for (int i = 0; i < this.listaDeServidores.size(); i++) {
                try {
                    lt.add(this.listaDeServidores.get(i).clone());
                } 
                catch (CloneNotSupportedException ob) {
                    ob.printStackTrace();
                }
            }
        }
        return lt;
    }
     
    //##########################    METODOS     ###########################################
}