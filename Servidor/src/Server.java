import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    //Atributos
    private String[] args   = null;
    private File directoria = null;
    private String erroDirectoria = null;
    private MulticastSocket socketUdpDirectoria = null;
    private ServerSocket server  = null;
    private DadosDoServidor dados = null;
    private int porto = 0;
    private Boolean running = true, runingTcp = true;
    private static final String HostDoGrupo = "225.15.15.15";
    private int portoUdp = 7000;
    private ThreadHeartbeats threadRecebeHearBeats = null;
    private enviaHeartbeats threadEnviaHeartebeast = null;
    private ContaPeriodos threadPeriodos = null;
    private boolean runningServer = true;
    private Socket nextClient = null;
    private Socket socketPrimario = null;
    private DadosDoServidor Primario = null;
    private double time = 0.0f;
    private  Runnable runn; 
    private ObjectInputStream in = null ;
    private ObjectOutputStream out = null;
    private int contador = 0;
    private Boolean podenEnviar = false;
    private Boolean faseArranque = true;
    private List<DadosDoServidor> listaDeServidores = null; 
    private boolean tempoDecisao       = true;
    private boolean estabelecerConexao = true;
    private List<threadAtendimento> listaDeClientes = null;
    private recebeDadosDoServidorPrimario threadRecebeDadosPrimario = null;
    private static int MAXSIZEFILE = 4086;
    private int  verificaTotal = 0;
    private RMIInterfaceSericoDirectoria servico = null;
    private ContaHeartBeadtsPrimario contaHeartBeadtsPrimario = null;
    
    //Construtor por defeito
    public Server(){ }

    //Construtor por parametros
    public Server(String[] args) {
       this.inicializarAtributos(args);
    }
    
    //################################################################################
    //#                                METODOS                                       #
    //################################################################################
    // java rmi
    private Boolean servicoJavaRmi(){
        try {
            String objectUrl;
            
            objectUrl = "//"+args[2]+"/ServicoMonitor";
            
            this.servico = (RMIInterfaceSericoDirectoria)Naming.lookup(objectUrl);
            
            return true;
        } 
        catch (RemoteException ex){
            System.err.println("RemoteException");
            return false;
        } 
        catch (NotBoundException ex){
            System.err.println("NotBoundException");
            return false;
        } 
        catch (MalformedURLException ex){
            System.err.println("MalformedURLException");
            return false;
        }
    }
     
    // inicializar atributos
    private void inicializarAtributos(String args[]) {
        this.args = args;
        
        if(this.args.length < 3){
            System.err.println("Erro syntax errada <Directory> e <port> servico de directoria <ip servico Directory java rmi>");
            System.err.println("Servidor vai terminar");
            System.exit(-1);
        }
        //fazer um lookup do servico java rmi
        
        if(this.servicoJavaRmi() == false){
            System.err.println("Nao encontrou nenhum servico ativo");
            System.exit(-1);
        }
        this.listaDeServidores = new ArrayList<>();
        this.listaDeClientes = new ArrayList<>();
        //verificar se a direcoria em questao existe e se tem permissoes de escrita
  
        this.directoria = new File(this.args[0]);

        //criar porto
        this.porto = Integer.parseInt(this.args[1]);
        if (this.porto > 60000) {//vefiricar se o porto é demasiado grande
            this.args = null;
            this.listaDeServidores = null;
            this.listaDeClientes = null;
            System.err.println("Erro por causa do porto é demasiado grande");
            System.gc();
            System.exit(-1);
        }
        this.threadPeriodos = new ContaPeriodos();

        this.threadPeriodos.start();

        try {
            InetAddress IP=InetAddress.getLocalHost();

            dados = new DadosDoServidor(IP, this.porto);
        } 
        catch (UnknownHostException ex) {
            System.err.println("Erro ao criar dados do servidor");
        }
    }
    
    //criar upd Servico
    private void createUdpServico(){
        try{
            InetAddress group = InetAddress.getByName(HostDoGrupo);
            this.socketUdpDirectoria = new MulticastSocket(this.portoUdp);
            //this.socketUdpDirectoria.setNetworkInterface(NetworkInterface.getByName("nome da minha placa de rede"));
            this.socketUdpDirectoria.joinGroup(group);
            
            //lancar thread do grupo multicast para receber
            this.threadRecebeHearBeats = new ThreadHeartbeats(socketUdpDirectoria, group, porto);
            this.threadRecebeHearBeats.start();
            
            //lancar para enviar
            this.threadEnviaHeartebeast = new enviaHeartbeats(socketUdpDirectoria, group, porto);
            this.threadEnviaHeartebeast.start();
        }

        catch(IOException ob){
            System.err.println("Erro ao criar MulticastSocket");
            System.exit(-1);
        }
    }
    
    //close conexoes
    private void closeConexoes() {
        try {
            this.running = false;
        } 
        finally {
            this.args = null;
            this.directoria = null;
            this.erroDirectoria = null;
            this.socketUdpDirectoria.close();
            this.dados = null;
            this.porto = 0;
            this.running = false;
            try{
            this.server.close();
            }
            catch(IOException ob){
                ob.printStackTrace();
            }
            this.server = null;
            this.socketUdpDirectoria = null;
            System.gc();// garbage colector
            this.threadPeriodos.requestStop();
        }
    }
    
    //Cliente run
    public void Run(){
        if(this.args.length < 2){
            System.err.println("Error wrong syntax <Directory of work>  <port> ");
            System.err.println("The client is finished");
            return;
        }
       
        if(this.checkDirectory() == false){//verificar se a directoria tem erro
            System.err.println(this.erroDirectoria);
            return;
        }
        this.createTcpServer();
        this.createUdpServico();
        this.atendeNovosClientes();
    }
    
    //Verifica as permissoes do sistema
    private boolean checkDirectory(){
        
         if (this.directoria.isDirectory() == false) {
             this.erroDirectoria = this.args[0]+ " is Directory  false";
             return false;
        }

        if (this.directoria.exists() == false) {
            this.erroDirectoria = this.args[0]+ "  dont exists";
            return false;
        }

        if (this.directoria.canWrite() == false) {
            this.erroDirectoria = this.args[0]+ " is canWrite";
            return false;
        }
       
        if (this.directoria.canRead() == false) {
            this.erroDirectoria = this.args[0]+ " is can read";
            return false;
        }
        
        return true;
    }    
    
    //Parar thread e fexar conexoes
    private void stopThreadCloseConexoes(){
        try{
            if(this.server != null){
                this.server.close();
            }
            this.threadPeriodos.requestStop();
        }
        catch(IOException ob){
            ob.printStackTrace();   
        }
    
    }
    
    //############################################################
    //#                 ATENDER CLIENTES  OU SERVIDORES          #
    //############################################################
    
    //criar tcp server
    private void createTcpServer(){
        try{
            this.server = new ServerSocket(this.porto);           
        }
        catch(IOException ob){
            System.err.println("Erro ao criar tcp server");
            this.stopThreadCloseConexoes();
            System.gc();
            System.exit(0);
        }
    }
    
    //atendeClientes
    private void atendeNovosClientes(){
        while(true){
            try{
                this.in = null;
                //System.out.println("Servidor run tcp"+ this.server.getInetAddress().getHostName() + " " + this.server.getLocalPort() );
                this.nextClient = this.server.accept();
                //System.out.println("--->Criei conexao com um cliente ou servidor");  
                this.contador++;
                threadAtendimento nextCliente = new threadAtendimento(this.nextClient,this.contador);
                this.listaDeClientes.add(nextCliente);
                
                nextCliente.start();
                
                //atualizar numero de clientes
                synchronized(dados){
                    dados.setNumeroClientes(this.contador);
                }
                //Java rmi
                rmiInformacaoDoSevervidor();
            }
            catch(IOException ob){
                System.err.println("Erro ao atender cliente");
               //System.exit(-1);
                continue;
            }
        }
            
    }
    
    //############################################################
    //#                 THREAD CLIENTES || servidores            #
    //############################################################
    class threadAtendimento extends Thread{
        //Atributos
        private Socket socket;
        private ObjectOutputStream out                 = null;
        private ObjectInputStream in                   = null;
        private boolean runn                           = true;
        private int numero                             = 0;
        private MsgCliente cliente                     = null;
        private MsgServidorSecundario  server          = null;
        private gravarFicheiros gravar                 = null;
        private gravarFicheirosDoCliente gravarCliente = null;
        private boolean igonarUpload                   = false;
        private MsgServidorSecundario msgServidor      = null;
        private int contadorDeSecundarios              = 0;
        private String ficheiroEmCausa                 = " ";
        
        //Construtor por parametros
        public threadAtendimento(Socket s, int numero){
            this.socket = s;
            this.numero = numero;
        }
      
        //############### TROCA DE INFO ENTRE PRIMARIO   <--- SECUNADIO ##################

        //Tratar da informacao do Secundario
        private synchronized void tratarDaInformacaoDoServidorSecundario(MsgServidorSecundario msg){
           //veficar o que o servidor Secundario pretende fazer
            if(msg.getComando().contains("CopyAll") == true){
                this.copiaTodosOsFicheiroDaDirectoria();
            }
            else if(msg.getComando().contains("EliminacaoCommit") == true){
                //enviarPedidoDeCommitParaSecundarios(this.msgServidor);
                if(procura(this.msgServidor.getFileName()) == false){
                    if(this.ficheiroEmCausa.contains(msg.getFileName()) != true){
                        setFicheiroEmCausa(msg.getFileName());
                        //guardar o numero total de servidores no momento
                        enviaTotalDeServersSecundarios(msg);// enviar pedido para apagar
                        setTotal(0);
                    }
                }
            }
            else if(msg.getComando().contains("EliminacaoNegativa") == true){
                //Esquecer o ficheiro em causa
                setTotal(0);
                //this.ficheiroEmCausa = " ";
                setFicheiroEmCausa(" ");
            }
            else if (msg.getComando().contains("EliminacaoPositiva") == true){
                //voltar a verificar numero de server no momento e se algum servidor arrancou no momento
                if (procura(this.msgServidor.getFileName()) == false) {
                    setTotal(1);  
                    if(getTotal() == getTotalDeServersSecundarios()){//todos dizem que sim 
                        totalNulo();
                        //envia pedido de confirmacao para apagar
                        //primeiro eu vou apagar o ficheiro
                        this.apagaFicheiro(msg.getFileName());
                        //depois enviar para server secundario apaga ficheiro
                        msg.setComando("Commit");
                        enviaCommit(msg);
                        //this.ficheiroEmCausa = " ";
                        setFicheiroEmCausa(" ");
                        //Enviar para todos os clientes neste caso um pedido para apagar ficheiro
                        notificaClientes(msg.getFileName(),"ApagarFIcheiro");    
                        //Java rmi
                        rmiInformacaoDoSevervidor();
                    }
                }
            }
            else if(msg.getComando().contains("FicheiroApagadoComSucesso") == true){
               
            }
            else if (msg.getComando().contains("Carregar") == true){
                if(this.ficheiroEmCausa.contains(msg.getFileName()) != true){
                    //this.ficheiroEmCausa = msg.getFileName();
                    setFicheiroEmCausa(msg.getFileName());                 
                    this.carregarFicheiroServerSecundario(msg);
                    this.changeFile(msg.getFileName(), false);
                    setTotal(0);
                }
            }
            else if(msg.getComando().contains("UploadNegativo") == true){
                //Esquecer o ficheiro em causa
                setTotal(0);
                setFicheiroEmCausa(" ");
                //Apagar ficheiro sem permissoes 
                this.apagaFicheiro(msg.getFileName());
                msg.setComando("CommitUploadNegativo");
                enviaCommit(msg);//envia commit
            }
            else if (msg.getComando().contains("UploadPositivo") == true) {
                setTotal(1);
                if (getTotal() == getTotalDeServersSecundarios()) {//todos dizem que sim 
                    totalNulo();
                    //depois enviar para secundario o commit de upload para mudar as permissoes
                    //tornar o file visivel para todos os clientes 
                    this.changeFile(msg.getFileName(),true);
                    msg.setComando("CommitUpload");
                    enviaTotalDeServersSecundariosUpload(msg);
                    setFicheiroEmCausa(" ");
                    //Enviar para todos os clientes neste caso um pedido para apagar ficheiro
                    notificaClientes(msg.getFileName(),"NovoFicheiro");
                    rmiInformacaoDoSevervidor();
                }
                else if(getTotal() > getTotalDeServersSecundarios()){//recebi pedidos a mais 
                     this.apagaFicheiro(msg.getFileName());
                }
            }
        }
        
        //tornar ficheiro visivel
        private void changeFile(String nome, Boolean condicao){
             File f = null;
             
             try{
                 f = new File(directoria+File.separator+nome.trim());
                 f.setReadable(condicao,condicao);
                 f.setWritable(condicao,condicao);
             }
             catch(Exception ob){
                 System.err.println("Erro ao mudar permissoes do ficheiro "+nome);
             }
        }
        
        //carregar ficheiro do server secundario
        private void carregarFicheiroServerSecundario(MsgServidorSecundario msg) {
            boolean condicao = true;
            String nomeFicheiro = msg.getFileName();
            try {
                ObjectInputStream in;
         
                List<MsgServidorSecundario> conjunto = new ArrayList<>();
                //guardar o nome 
                while (condicao) {
                    in = new ObjectInputStream(this.socket.getInputStream());

                    Object returnObj = in.readObject();

                    if (returnObj instanceof MsgServidorSecundario) {
                        MsgServidorSecundario ob = (MsgServidorSecundario) returnObj;
                        ob.setFileName(nomeFicheiro);
                        conjunto.add(ob);

                        if (ob.getNbytes() == 0) {
                            condicao = false;
                        }
                    }
                    in = null;
                    returnObj = null;
                    System.gc();
                }

                this.gravar = new gravarFicheiros(conjunto);                
                this.gravar.start();
                this.gravar.join();//esperar que a thread grave um ficheiro
                
                //enviar para secundarios o ficheiro sem permissoes
                enviaNovoFicheiro(nomeFicheiro);
            } 
            catch (EOFException ob) {
                condicao = false; 
            } 
            catch (IOException ex) {
                System.err.println("Erro ao ler dados dos cliente neste caso in carregarFicheiroServerSecundario");
            } 
            catch (ClassNotFoundException ex) {
                System.err.println("Class nao reconhecida carregarFicheiroServerSecundario");
            } 
            catch (InterruptedException ex) {
                System.err.println("Erro thread de gravar ficheiro foi enterrompida no carregarFicheiroServerSecundario");
            } 
        }
    
        //envia commit apaga ficheiro
        public  void apagaFicheiro(MsgServidorSecundario msg){
            try{
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                msg.setComando("commit");
                this.out.writeObject(msg);
                this.out.flush();
            }
            catch(IOException ob){
                System.err.println("Erro ao enviar commitParaApagarFicheiro");
            }
  
        }

        //Copiar todos os ficheiros da minha directoria
        private void copiaTodosOsFicheiroDaDirectoria() {
            try {
                File f = new File(directoria.getCanonicalPath()); // current directory // current directory

                File[] files = f.listFiles();

                for (File file : files) {

                    if (!file.isDirectory()){
                        String nome = file.getName().trim();
                        //enviar o file
                        this.enviarFicheiro(nome, file);
                    }
                }
                //fim da copia
                MsgServidorSecundario msg = new MsgServidorSecundario();

                //enviar comando
                msg.setComando("FimCopyAll");
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                this.msgServidor.setComando(" ");
            } 
            catch (IOException ex) {
                System.err.println("Erro ao abrir directoria");
            }
        }

        //enviar os ficheiros
        private void enviarFicheiro(String nome, File file) {
            FileInputStream fileInputStream = null;

            byte[] bFile = new byte[MAXSIZEFILE];

            try {
                //convert file into array of bytes
                MsgServidorSecundario msg = new MsgServidorSecundario();

                //enviar comando
                msg.setComando("CopiaDoPrimario");
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                //Enviar o nome
                msg.setFileName(nome);//copiar o nome do ficheiro

                //depois comecar a copia dos bytes para o servidor
                FileInputStream fileIn;

                fileIn = new FileInputStream(file.getCanonicalPath());

                int nbytes;

                while ((nbytes = fileIn.read(bFile)) > 0) {
//                    System.out.println("a enviar dados");
                    msg.setbFile(bFile);
                    msg.setNbytes(nbytes);
                    msg.setFimDaEscrita(true);
                    this.out = new ObjectOutputStream(this.socket.getOutputStream());
                    this.out.writeObject(msg);
                    this.out.flush();
                }
                //fim da escrita
                msg.setFimDaEscrita(false);
                msg.setNbytes(0);
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                fileIn.close();
                System.gc();//Libertar memoria
            } 
            catch (IOException ob) {
                System.err.println("Erro ao copiar ficheiro dentro do envia ficheiro");
            }
        }
        
        //enviar os ficheiros
        private void enviarFicheiroUplod(String nome, File file) {
            FileInputStream fileInputStream = null;

            byte[] bFile = new byte[MAXSIZEFILE];

            try {
                //convert file into array of bytes
                MsgServidorSecundario msg = new MsgServidorSecundario();

                //enviar comando
                msg.setComando("UploadDoPrimario");
                //Enviar o nome
           
                File f = new File(nome);

                msg.setFileName(f.getName().trim());//copiar o nome do ficheiro
                
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                
                //depois comecar a copia dos bytes para o servidor
                FileInputStream fileIn;

                fileIn = new FileInputStream(directoria+File.separator+nome);
                int nbytes;

                while ((nbytes = fileIn.read(bFile)) > 0) {
                    msg.setbFile(bFile);
                    msg.setNbytes(nbytes);
                    msg.setFimDaEscrita(true);
                    this.out = new ObjectOutputStream(this.socket.getOutputStream());
                    this.out.writeObject(msg);
                    this.out.flush();
                }
                //fim da escrita
                msg.setFimDaEscrita(false);
                msg.setNbytes(0);
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                fileIn.close();
                System.gc();//Libertar memoria
            } 
            catch (IOException ob) {
                System.err.println("Erro ao copiar ficheiro dentro do envio ficheiro");
            } 
        }
        
        //############### TROCA DE INFO ENTRE SECUNADIO  ---> PRIMARIO ##################
        
        //###################  METODOS PARA O CLIENTE #####################
        
         //carregar ficheiro do server secundario
        private void carregarFicheiroServerDoCliente(MsgCliente msg) {
            boolean condicao = true;
            String nomeFicheiro = msg.getFileName();
            try {
                ObjectInputStream in;
         
                List<MsgCliente> conjunto = new ArrayList<>();
                //guardar o nome 

                while (condicao) {
                    in = new ObjectInputStream(this.socket.getInputStream());

                    Object returnObj = in.readObject();

                    if (returnObj instanceof MsgCliente) {
                        MsgCliente ob = (MsgCliente) returnObj;
                        ob.setFileName(nomeFicheiro);
                        conjunto.add(ob);

                        if (ob.getNbytes() == 0) {
                            condicao = false;
                        }
                    }
                    in = null;
                    returnObj = null;
                    System.gc();
                }

                this.gravarCliente = new gravarFicheirosDoCliente(conjunto);                
                this.gravarCliente.start();
                this.gravarCliente.join();//esperar que a thread grave um ficheiro
                
                //enviar para secundarios o ficheiro sem permissoes
                enviaNovoFicheiro(nomeFicheiro);
            } 
            catch (EOFException ob) {
                condicao = false; 
            } 
            catch (IOException ex) {
                System.err.println("Erro ao ler dados dos cliente neste caso in carregarFicheiroServerSecundario");
            } 
            catch (ClassNotFoundException ex) {
                System.err.println("Class nao reconhecida carregarFicheiroServerSecundario");
            } 
            catch (InterruptedException ex) {
                System.err.println("Erro thread de gravar ficheiro foi enterrompida no carregarFicheiroServerSecundario");
            } 
        }
      
        //private void copiar ficheiro
        private void getFindFile(String nome){
            Boolean flag = false;
            try {
                File f = new File(directoria.getCanonicalPath()); // current directory // current directory

                File[] files = f.listFiles();

                for (File file : files) {

                    if (!file.isDirectory()) {
                       // System.out.print("directory:");
                        String name = file.getName().trim();
                        if(nome.contains(name)== true){
                           //enviar os dados para o cliente
                            this.enviarFicheiroParaCliente(name, file);
                            flag = true;
                        }
                    }
                }
                //enviar informacao que nao econtrei file
                if(flag == false){
                    this.setFicheiroNaoEncontrado();
                }
            } 
            catch (IOException ex) {
                System.err.println("Erro ao abrir directoria");
            }
        }
        
        //enviar informacao que nao eocntrei file
        private void setFicheiroNaoEncontrado(){
            try {
                this.cliente.setComando("notfound");     
                this.cliente.setNbytes(0);
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(this.cliente);
                this.out.flush();
                System.gc();
            } 
            catch (IOException ex) {
                System.err.println("Ficheiro nao foi encontrado");
            }
        }
        
        //Apagar ficheiro
        private void apagaFicheiro(String ficheiro) {
            String nome = ficheiro;
            try {
                File f = new File(directoria.getCanonicalPath()); // current directory // current directory

                File[] files = f.listFiles();

                for (File file : files) {

                    if (!file.isDirectory()) {
                        // System.out.print("directory:");
                        String name = file.getName().trim();
                        if (nome.contains(name) == true) {
                            //enviar os dados para o cliente
                            file.delete();
                        }
                    }
                }
            } 
            catch (IOException ex) {
                System.err.println("Erro ao apagar ficheiro directoria");
            }
        }
               
        //nome do ficheiro invalido
        private void ficheiroInvalido(){
            try {
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.cliente.setComando("FicheiroInvalido");
                this.out.writeObject(this.cliente);
                this.out.flush();
            } 
            catch (IOException ex) {
                System.err.println("Erro ao enviar nome do ficheiro invalido");
            }
           
        }
        
        //escrever para cliente
        private void enviarCliente(){
            try{
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(this.cliente);
                this.out.flush();
            }
            catch(IOException ob){
                System.err.println("Erro ao enviar dados para cliente");
            }
        }
        
        //VerificaComandoCliente
        private void verificaComando(String comando){
            if(comando.contains("Listar") == true){
                //gravar todos os dados dos files a enviar para o cliente
                this.cliente.clearLista();
                this.gravarDadosDosFicheiros();
                this.enviarCliente();
            }
            else if(comando.contains("Download") == true){
                //("Tenho que copiar este file do cliente"+this.cliente.getNomeDoFicheiroACopiar());
                //procurar o ficheiro ver se ele existe
                if(this.ficheiroEmCausa.contains(this.cliente.getFileName()) == false){
                    try{
                        this.verificaSeExisteEsseFicheiro();
                        this.getFindFile(this.cliente.getNomeDoFicheiroACopiar());
                    }
                    catch(IOException ob){
                        //ficheiro nao existe 
                        //enviar sms a dizer que ficheiro nao existe
                        this.setFicheiroNaoEncontrado();
                    }
                }
            }
            else if (comando.contains("Eliminacao") == true) {
                //enviar informacao para o servidor primario 
                MsgServidorSecundario msg = new MsgServidorSecundario();
                msg.setComando("EliminacaoCommit");
                msg.setFileName(this.cliente.getFileName());
                if (socketPrimario != null) {//nao sou o primario
                    try {
                        ObjectOutputStream envia = new ObjectOutputStream(socketPrimario.getOutputStream());
                        envia.writeObject(msg);
                        envia.flush();
                    } 
                    catch (IOException ex) {
                        System.err.println("Erro ao enviar notificao para apagar ficheiro para o Primario");
                    }
                }
                else{//Sou o primario
                    if(procura(msg.getFileName()) == false){//verificar se esta tudo bem
                        //guardar o numero total de servidores no momento
                        if(getTotalDeServersSecundarios() == 0){//verificar se tenho servidores se for o unico basta apagar
                            //verificar o ficheiro em causa
                            if(procura(msg.getFileName()) != true){
                                setFicheiroEmCausa(msg.getFileName());
                                this.apagaFicheiro(msg.getFileName());
                                setFicheiroEmCausa(" ");
                                //Enviar para todos os clientes neste caso um pedido para apagar ficheiro
                                notificaClientes(msg.getFileName(),"ApagarFIcheiro");
                                //Java rmi
                                rmiInformacaoDoSevervidor();
                            }
                        }
                        else{//senao tenho que enviar pedido de apagar porque nao sou o unico server e esperar pela resposta
                             //verificar o ficheiro em causa
                            if(procura(msg.getFileName()) != true){   
                                setFicheiroEmCausa(msg.getFileName());        
                                enviaTotalDeServersSecundarios(msg);// enviar pedido para apagar   
                                //Enviar para todos os clientes neste caso um pedido para apagar ficheiro
                                notificaClientes(msg.getFileName(),"ApagarFIcheiro");
                                setFicheiroEmCausa(" ");
                            }
                        }
                    }
                }
            }
            else if(comando.contains("Carregar") == true){
                try{
                    this.verificaSeJaExisteEsteFicheiro();
                    this.igonarUpload = false;
                    //enviar para server primario o pedido
                    if(socketPrimario != null){//nao sou o primario
                        this.enviaUploadParaPrimario(cliente);
                    }
                    else{//Sou primario
                        this.carregarFicheiroServerDoCliente(cliente);
                        this.changeFile(this.cliente.getFileName(), false);
                      
                        if(getTotalDeServersSecundarios() == 0){// nao existem servidores secundarios
                             this.changeFile(this.cliente.getFileName(), true);
                             //Java rmi
                             rmiInformacaoDoSevervidor();
                        }
                        else{//tenho que enviar pedido de commit para os outros servidores
                            setTotal(0);
                        }
                        //Enviar para todos os clientes neste caso um pedido para apagar ficheiro
                        notificaClientes(this.cliente.getFileName(),"NovoFicheiro");
                    }
                }
                catch(IOException ob){
                    this.igonarUpload  = true;
                } 
            }
            else if(comando.contains(" ")== true){
                this.enviarCliente();
            }
            else if(comando.contains("Sair") == true){
                this.runn = false;
            }
        }
             
        //enviar o pedido de carregar para um server primario
        private void enviaUploadParaPrimario(MsgCliente ob){
            MsgServidorSecundario msg = new MsgServidorSecundario();
            msg.setComando("Carregar");
            msg.setFileName(ob.getFileName());
            msg.setbFile(ob.getbFile());
            msg.setNbytes(ob.getNbytes());
            if(socketPrimario != null){// nao sou o primario
                try {
                    
                    this.out = new ObjectOutputStream(socketPrimario.getOutputStream());
                    this.out.writeObject(msg);
                    this.out.flush();
                } 
                catch (IOException ex) {
                    System.err.println("Erro ao enviar dados para o primario upload");
                }
            }
            
        }
        
        //Verifica se ja existe esse ficheiro
        private void verificaSeJaExisteEsteFicheiro() throws IOException{
            
            File localDirectoria = new File(directoria+File.separator+this.cliente.getFileName());

            if(!localDirectoria.exists()){
                if(localDirectoria.isFile()){
                    throw new IOException();//se existe lanca um ficheiro com este nome
                }
            }
            else{
                throw new IOException();
            }
        }
        
        //verificar se existe este ficheiro
        private void verificaSeExisteEsseFicheiro() throws IOException{
            File localDirectoria = new File(directoria+File.separator+this.cliente.getFileName());

            if(!localDirectoria.exists()){
                if(!localDirectoria.isFile()){
                    throw new IOException();//se nao existe lanca IOEXCEPTION
                }
            }
        }

        //carregar novo ficheiro
        private void carregarFicheiro() {
            boolean condicao = true;
            String nomeFicheiro = this.cliente.getFileName();
            try {
                ObjectInputStream in;
         
                List<MsgCliente> conjunto = new ArrayList<>();
                //guardar o nome 

                while (condicao) {
                    in = new ObjectInputStream(this.socket.getInputStream());

                    Object returnObj = in.readObject();

                    if (returnObj instanceof MsgCliente) {
                        MsgCliente ob = (MsgCliente) returnObj;
                        ob.setFileName(nomeFicheiro);
                        conjunto.add(ob);

                        if (ob.getNbytes() == 0) {
                            condicao = false;
                        }
                    }
                    in = null;
                    returnObj = null;
                    System.gc();
                }

                if(this.igonarUpload == false){
                    this.gravarCliente = new gravarFicheirosDoCliente(conjunto);
                    this.gravarCliente.start();
                }
            } 
            catch (EOFException ob) {
                condicao = false; 
            } 
            catch (IOException ex) {
                System.err.println("Erro ao ler dados dos cliente neste caso in");
            } 
            catch (ClassNotFoundException ex) {
                System.err.println("Class nao reconhecida");
            } 
        }
        
        //Enviar informacao que o pedido foi ignorado
        private void indicaSeOFicheiroFoiIgnorado() {
            if (igonarUpload) {
                this.cliente.setComando("Ficheiro ignorado");
            } 
            else {
                this.cliente.setComando("Ficheiro aceite");
            }
            try {
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(this.cliente);
                this.out.flush();
            } 
            catch (IOException ex) {
                System.err.println("Erro ao indicar se o ficheiro foi aceite");
            }
            System.gc();
        }
        
        //enviar os ficheiros
        private void enviarFicheiroParaCliente(String nome, File file) {
            FileInputStream fileInputStream = null;

            byte[] bFile = new byte[MAXSIZEFILE];
            FileInputStream fileIn = null;

            try {
                //convert file into array of bytes
                MsgCliente msg = new MsgCliente();

                //enviar comando
                msg.setComando("Download");
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                
                //Enviar o nome
                msg.setFileName(nome);//copiar o nome do ficheiro

                //depois comecar a copia dos bytes para o servidor

                fileIn = new FileInputStream(file.getCanonicalPath());

                int nbytes;

                while ((nbytes = fileIn.read(bFile)) > 0) {
                    msg.setbFile(bFile);
                    msg.setNbytes(nbytes);
                    msg.setFimDaEscrita(true);
                    this.out = new ObjectOutputStream(this.socket.getOutputStream());
                    this.out.writeObject(msg);
                    this.out.flush();
                }
                //fim da escrita
                msg.setFimDaEscrita(false);
                msg.setNbytes(0);
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                fileIn.close();
                System.gc();//Libertar memoria
            } 
            catch (IOException ob) {
                System.err.println("Erro ao copiar ficheiro");
                if(fileIn != null){
                    try {
                        fileIn.close();
                    } 
                    catch (IOException ex) {
                        System.err.println("Erro ao fechar ficheiro");
                    }
                }
            } 
        }
             
        //Gravar todos os dados dos files
        private void gravarDadosDosFicheiros(){
            try {
                File f = new File(directoria.getCanonicalPath()); // current directory // current directory

                File[] files = f.listFiles();

                for (File file : files) {

                    if (!file.isDirectory() && file.canRead() == true && file.canWrite() == true) {
                        String nome = file.getName().trim();
                        //Gravar
                        this.cliente.setLista(nome, file.length()); 
                    }
                }
            } 
            catch (IOException ex) {
                System.err.println("Erro ao abrir directoria");
            }
        }

        //verifica se um servidor
        public MsgServidorSecundario getMsgServidor() {
            return msgServidor;
        }

        //retorna socket
        public Socket getSocket() {
            return socket;
        }        
        
        //setFicheiro em causa
        public void setFicheiroEmCausa(String ficheiro){
            this.ficheiroEmCausa = ficheiro;
        }
        
        //###################  METODOS PARA O CLIENTE #####################
        
        //METODO para verificar se alguem esta utilizar um dos comando copyall ou download
        public Boolean verificaSeAlguemEstaAfazerDownload(String ficheiro){
     
            if (this.msgServidor != null) {// é um servidor
                //se for um server verificar o comando dele
                if (this.msgServidor.getComando().contains("Download") == true) {
                    if (this.msgServidor.getFileName().contains(ficheiro) == true) {
                        return true;
                    }
                } else if (this.msgServidor.getComando().contains("CopyAll") == true) {
                    return true;
                }
            }

            if (this.cliente != null) {// é um cliente
                //se for um cliente verificar o comando dele
                if (this.cliente.getComando().contains("Download") == true) {
                    if (this.cliente.getFileName().contains(ficheiro) == true) {
                        return true;
                    }
                }
            }

            return false;
        }
        
        //enviar commit de elinacao
        public void enviaCommitEliminacao(MsgServidorSecundario msg){
            try {
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
            } 
            catch (IOException ex) {
                System.err.println("Erro eo enviar EliminacaoCommit");
            }
        }
        
        //enviar commit de elinacao
        public void enviaCommitUpload(MsgServidorSecundario msg){
            try {
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
            } 
            catch (IOException ex) {
                System.err.println("Erro eo enviar EliminacaoCommit");
            }
        }
          
        //notifica clientes de alteracoes
        public void setAlteracaoDoServidor(MsgCliente ob){
            try{
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(ob);
                this.out.flush();
            }
            catch(IOException erro){
                System.err.println("Erro notifica clientes de alteracoes");
            }
        }
        
        public int getNumero(){//numero da thread
            return this.numero;
        }
        
        @Override
        public void run() {
            if (this.socket == null) {
                return;
            }
            while (this.runn) {
                try {
                    try {
                        //receber dados
                        this.in = null;

                        this.in = new ObjectInputStream(this.socket.getInputStream());
                        Object obj = this.in.readObject();
                        //Se for um cliente executa certos metodos 
                        if (obj instanceof MsgCliente) {
                            synchronized (dados) {
                                if (dados.getEmConfiguracao() == false) {
                                    this.cliente = (MsgCliente) obj;
                                    //enviar dados
                                    this.cliente.setEstadoPedido(false);
                                    this.verificaComando(this.cliente.getComando());
                                    if(this.cliente.getComando().contains("Carregar") == true){
                                         this.indicaSeOFicheiroFoiIgnorado();
                                    }
                                }
                                else{
                                    this.cliente = (MsgCliente) obj;                                    
                                    //enviar dados
                                    this.cliente.setEstadoPedido(true);
                                    this.out = new ObjectOutputStream(this.socket.getOutputStream());
                                    this.out.writeObject(this.cliente);
                                    this.out.flush();
                                }
                            }
                        }

                        //Se for um servidor secundario faz certos metodos
                        if (obj instanceof MsgServidorSecundario) {
                            this.server = (MsgServidorSecundario) obj;
                            this.msgServidor = (MsgServidorSecundario) obj;
                            this.tratarDaInformacaoDoServidorSecundario(server);
                        }
                    }                                               
                    catch (ClassNotFoundException ex) {
                        System.err.println("recebi um objeto de um tipo desconhecido ");
                        continue;
                    }
                } 
                catch (IOException ex){
                     this.runn = false;
                }
            }
            try {
                this.socket.close();
            } 
            catch (IOException ob) {
                System.err.println("Erro ao fechar socket " + ob);
            }
            //remover este cliente do arryList
            removeClientes(this);
            //Java rmi
            rmiInformacaoDoSevervidor();
        }
    }
     
    //Remover clientes
    private synchronized void removeClientes(threadAtendimento ob) {
        comparaClientes comp = new comparaClientes();
        synchronized (this.listaDeClientes) {
            for (int i = 0; i < this.listaDeClientes.size(); i++) {//O(N)
                if (comp.compare(ob, this.listaDeClientes.get(i)) == 0) {//Apaga este cliente
                    this.listaDeClientes.remove(i);
                    synchronized (this.listaDeClientes) { //Atualiza dados
//                        System.out.println("total e clientes " + this.listaDeClientes.size());
                    }
                }
            }
        }
    }

    //enviar notifica da alteracao de um ficheiro a um cliente
    private synchronized void notificaClientes(String nomeFicheiro, String comando){
       synchronized(this.listaDeClientes){
           for(int i = 0; i < this.listaDeClientes.size(); i++){
               if(this.listaDeClientes.get(i).getMsgServidor() == null){//Encontrei um cliente
                   MsgCliente msg = new MsgCliente();
                   msg.setFileName(nomeFicheiro);
                   msg.setComando(comando);
                   this.listaDeClientes.get(i).setAlteracaoDoServidor(msg);// notifica alteracoes ao cleinte
               }
           }
       }
    } 
    
    //set ficheiro em causa
    private synchronized void setFicheiroEmCausa(String ficheiro){
        synchronized(this.listaDeClientes){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                this.listaDeClientes.get(i).setFicheiroEmCausa(ficheiro);
            }
        }
    }
    
    //enviar para todos os servers a novo ficheiro
    private synchronized void enviaNovoFicheiro(String nome){
        //directoria+File.separator+this.lista.get(0).getFileName());
        File f = new File(directoria+File.separator+nome);
        
        if(this.listaDeClientes != null){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                if(this.listaDeClientes.get(i).getMsgServidor() != null){
                    this.listaDeClientes.get(i).enviarFicheiroUplod(nome, f);
                }
            }
        }
    }
    
    //copiar dados
    private  Boolean procura(String ficheiro){
        if(this.listaDeClientes != null && this.listaDeClientes.size() > 0){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                if(this.listaDeClientes.get(i).verificaSeAlguemEstaAfazerDownload(ficheiro) == true){
                    return true;
                }
            }
        }
        return false;//ninguem esta fazer nada de especial
    } 
    
    //set Total
    private synchronized void setTotal(int numero){
        this.verificaTotal+=numero;
    }
    
    //get total
    private synchronized int getTotal(){
        return this.verificaTotal;
    }
    
    //coloca total a zero
    private synchronized void totalNulo(){
        this.verificaTotal = 0;
    }
    
    //Contar todos os server secundarios que tenho e enviar um pedido de commit
    private  void enviaTotalDeServersSecundarios(MsgServidorSecundario msg){

        if(this.listaDeClientes.size() > 0){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                if(this.listaDeClientes.get(i).getMsgServidor() != null){//encontrei um server
                    this.listaDeClientes.get(i).enviaCommitEliminacao(msg);
                }
            }
        }
    }
    
    //Contar todos os server secundarios que tenho e enviar um pedido de commit
    private  void enviaTotalDeServersSecundariosUpload(MsgServidorSecundario msg){

        if(this.listaDeClientes.size() > 0){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                if(this.listaDeClientes.get(i).getMsgServidor() != null){//encontrei um server
                    this.listaDeClientes.get(i).enviaCommitUpload(msg);
                }
            }
        }
    }
    
    //total de servidores
    private synchronized int getTotalDeServersSecundarios(){
        int total = 0;
        if(this.listaDeClientes.size() > 0){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                if(this.listaDeClientes.get(i).getMsgServidor() != null){//encontrei um server
                   total++;
                }
            }
        }
        
        return total++;
    }
    
    //total de servers no momento
    private int totalDeServersNoMomento() {
        int total = 0;

        if (this.listaDeClientes.size() > 0) {
            for (int i = 0; i < this.listaDeClientes.size(); i++) {
                if (this.listaDeClientes.get(i).getMsgServidor() != null) {//encontrei um server
                    total++;
                }
            }
        }

        return total;
    }
   
    //envia pedido para apagar
    private void enviaCommit(MsgServidorSecundario msg){
        if(this.listaDeClientes.size() > 0 && this.listaDeClientes != null){
            for(int i = 0; i < this.listaDeClientes.size(); i++){
                if(this.listaDeClientes.get(i).getMsgServidor() != null){
                    this.listaDeClientes.get(i).enviaCommitEliminacao(msg);// e um servidor 
                }
            }
        }
    }
  
    //############################################################
    //#                    JAVA RMI                              #
    //############################################################
  
    //envia a informacao para o servico
    private synchronized void rmiInformacaoDoSevervidor(){
        try {
            StringBuilder sb = new StringBuilder();
               
            //tipo do servidor
            sb.append("> Tipo de servidor "+this.tipoDoServidor()+"\n");
            
            //Ip do servidor
            sb.append("> Ip "+this.dados.getIp()+"\n");
            
            //porto de escuta tcp
            sb.append("> Porto tcp "+ this.server.getLocalPort()+"\n");
            
            //Lista de ficheiros
            sb.append(this.getListaDeFicheiros());

            //get numero de clientes conectadados
            sb.append("\n >Total de clientes = "+this.getNumeroDeClientes());
            //enviar dados para o servidor
            if(this.servico != null){
                this.servico.updateServicoServidor(sb.toString());
            } 
        }
        catch (RemoteException ex) {
            System.err.println("Erro ao enviar dados para o servico directoria java rmi");
            //Tenho que ir fazendo lookup caso de um erro
        }
    }
    //Total de clientes
    private synchronized int getNumeroDeClientes(){
        int total = 1;
        synchronized (this.listaDeClientes){
            for (int i = 0; i < this.listaDeClientes.size(); i++){
                if (this.listaDeClientes.get(i).getMsgServidor() == null){//Encontrei um cliente
                    total++;
                }
            }
        }
        return total;
    }
    
    //Tipo do servidor
    private synchronized String tipoDoServidor(){
        if(this.dados.getTipoHearBeat() == 1){
            return "Primario";
        }
        else {//Secundario
            return "Secundario";
        }
    }
    
    //get informacao dos ficheiros
    private String getListaDeFicheiros() {
        StringBuilder st = new StringBuilder();
        st.append("Lista de ficheiros\n");
        try {
            File f = new File(directoria.getCanonicalPath()); // current directory // current directory

            File[] files = f.listFiles();

            for (File file : files) {

                if (!file.isDirectory()) {
                    //nome do ficheiro
                    st.append(file.getName().trim() + "\n");
                }
            }
        } 
        catch (IOException ob) {
            System.err.println("Erro ao ler dados dos ficheiros no servico java rmi");
        }

        return st.toString();
    }
    //############################################################
    //#               THREAD  ENVIA HEARTBEATS                   #
    //############################################################
    
    class enviaHeartbeats extends Thread{
        //Atributos 
        private MulticastSocket socket     = null;
        private DatagramPacket pakcet      = null;
        private ByteArrayOutputStream buff = null;
        private ObjectOutputStream out     = null;
        private boolean running            = true;
        private InetAddress group          = null;
        private int portoDoServidor        = 0;
       
         //Construtor
        public enviaHeartbeats(MulticastSocket s, InetAddress gp, int port){
            this.socket = s;
            this.pakcet = new DatagramPacket(new byte[Server.MAXSIZEFILE],Server.MAXSIZEFILE);
            this.buff = new ByteArrayOutputStream();
   
            this.group = gp;
            this.portoDoServidor = port;    
             
          
        }
        
        @Override
        public void run(){ 
            while(this.running){
                try{
                    //Enviar dados para o servico
                    synchronized(podenEnviar){
                        if(podenEnviar == true){
                            synchronized(dados){
                                this.envia(dados);
                            }
                        }
                    }
                    Thread.sleep(5000);
                } 
                catch (InterruptedException ob) {
                    ob.printStackTrace();
                }
                catch(IOException ob){
                    System.err.println("Erro ao enviar dados");
                    continue;
                }
            }
        }
    
          //Criei um stop
        public void requestStop(){
            this.running = false;
            this.stop();
        }
        
        //enviar
        private void envia(DadosDoServidor dados)throws IOException{
            this.buff = new ByteArrayOutputStream();
            this.out = new ObjectOutputStream(buff);
            this.out.writeObject(dados);
            this.out.flush();
            this.out.close();

            this.pakcet = new DatagramPacket(buff.toByteArray(), buff.size(),
                    group, portoUdp);

            this.socket.send(this.pakcet);

        }
    
    } 
    
    //############################################################
    //#               THREAD RECEBE HEARTBEATS                   #
    //############################################################
          
    class ThreadHeartbeats extends Thread{
        //Atributos
        private MulticastSocket socket     = null;
        private DatagramPacket pakcet      = null;
        private ByteArrayOutputStream buff = null;
        private ObjectInputStream in       = null;
        private boolean running            = true;
        private InetAddress group          = null;
        private int portoDoServidor        = 0;
        private int fasePrimaria           = 0;
        private boolean faseDeArranque     = true;
        private boolean lancarThread       = true;
        private Decisao   decide           = null;
        private Boolean criarConexao       = false; 
        private Boolean flag               = true;//flag para criar nova conexao com servidor Primario
        private comparatorServidor comp    = new comparatorServidor();
        private boolean lancarContadorPrimario = false;
        private double tempoPrimario = 0.0f;

        
        //Construtor
        public ThreadHeartbeats(MulticastSocket s, InetAddress gp, int port){
            this.socket = s;
            this.pakcet = new DatagramPacket(new byte[Server.MAXSIZEFILE],Server.MAXSIZEFILE);
            this.buff = new ByteArrayOutputStream();
   
            this.group = gp;
            this.portoDoServidor = port;    

        }
       
        @Override
        public void run(){
          
            while(this.running){
                try{
                    //receber                  
                    this.in = null;
                
                    this.pakcet.setData(new byte[Server.MAXSIZEFILE],0, Server.MAXSIZEFILE);
                    
                    this.socket.receive(pakcet);
                    
                    this.in = new ObjectInputStream(new ByteArrayInputStream
                                                   (this.pakcet.getData()
                                                   ,0
                                                   ,this.pakcet.getLength()));
                    Object obj = this.in.readObject();
                   
                    if(obj instanceof DadosDoServidor){
                        stopTread();
                        //veriricar quem deve ser primario
                        DadosDoServidor ob = (DadosDoServidor) obj;
                        addServidores(ob);
                              
                        if(ob.getTipoHearBeat() == 1){// recebi um primario
                            if(ob.getEstadoDeArranque() == true){//ja arrancou entao devo ser secundario
                                //verificar se o servidor primario deixa de enviar Heartbeats do tipo primario
                                //caso haja uma quebra de ligacao entre o servidor primario e scundario
                                tempoPrimario = System.currentTimeMillis();
 
                                if(contaHeartBeadtsPrimario != null){
                                    contaHeartBeadtsPrimario.setTempo(tempoPrimario);
                                }
                                
                                if(this.lancarContadorPrimario == false){
                                   this.lancarContadorPrimario = true;
                                   contaHeartBeadtsPrimario = new ContaHeartBeadtsPrimario();
                                   contaHeartBeadtsPrimario.start();
                                }
                               
                                if(this.flag == false){
                                    this.criarConexao = false;
                                    this.flag = true;
                                }
                                comparatorServidor comp = new comparatorServidor();                       
                                if(comp.compare(dados, ob) != 0){// se nao for o proprio
                                    synchronized(dados){//ja arrancou um servidor entao devo ser secundario
                                        dados.setTipoHearBeat(2);
                                        dados.setEmConfiguracao(false);
                                    
                                        //Estabelecer conexao
                                        if(this.criarConexao == false){
                                            this.criarConexao = true;
                                            criarConexao(ob);
                                            rmiInformacaoDoSevervidor();
                                        }
                                        if(this.decide != null){//Parar thread do tempo de decisao
                                            this.decide.requestStop();
                                        }
                                    }
                                }
                            }
                        }
                        if(tempoDecisao == true){//tempo de decisao
                              if(this.lancarThread == true){
                                  //lancar thread do tempo de decisao para decidir
                                  decide = new Decisao(tempoDecisao,estabelecerConexao);
                                  decide.start();
                              }
                              this.lancarThread = false;
                         }
                         else{
                            if(estabelecerConexao == false){
                                //Criar conexao procurar o menor
                                estabelecerConexao = true;
                                tenhoQueMudar();
                                 if(this.criarConexao == false){
                                     criarConexao(Primario);
                                     rmiInformacaoDoSevervidor();
                                     this.criarConexao = true;
                                     synchronized(dados){
                                         dados.setEmConfiguracao(false);
                                     }
                                 }
                            }
                        }
                    }
                } 
                catch(ClassNotFoundException ob){
                    ob.printStackTrace();
                    continue;
                }
                catch(IOException ob){
                    System.err.println("Erro ao enviar dados");
                    continue;
                }
            }
        }
          
        //Criei um stop
        public void requestStop(){
            this.running = false;
            this.stop();
        }
        
        public void requestStop2(){
            this.running = false;
        }
        
        //criar uma nova conexao entre o servidor primario e secundario
        public synchronized void setCriarConexao(Boolean flag){
            this.flag = flag;
        }
        
    }
    
    //verifica se tenho que mudar de tipo
    private void tenhoQueMudar(){
        synchronized (dados) {
            comparatorServidor comp = new comparatorServidor();
            DadosDoServidor menor = this.listaDeServidores.get(0);
            DadosDoServidor dados = this.dados;
            //percorrer a lista de servidores
            for(int i = 0; i < this.listaDeServidores.size(); i++){
                if(comp.compare(menor, this.listaDeServidores.get(i)) < 0){//se os ips forem diferentes
                    menor = this.listaDeServidores.get(i);
                }
            }
            
            //verificar se sou eu o menor
        
            if (comp.compare(dados, menor) == 0) {
                dados.setTipoHearBeat(1);
                dados.setEstadoDeArranque(true);
            } 
            else {
                dados.setTipoHearBeat(2);
                dados.setEstadoDeArranque(true);
            }
            this.dados = dados;//copiar os meus novos dados
            this.Primario = menor;//Copiar os dados do primario
        }
    }
   
    //############################################################
    //#            CRIAR CONEXAO COM O SERVIDOR SECUNDARIO       #
    //############################################################
    
     //Se o servidor for secundario a primeira coisa é apagar todos os ficheiros da sua directoria
     private synchronized void apagaFicheiros() throws IOException{
         synchronized(this.dados){
             if(this.dados.getTipoHearBeat() == 2){//sou um servidor secundario
                 //Primeiro devo apagar todos os ficheiros da minha directoria
                 File f = new File(this.directoria.getCanonicalPath()); // current directory

                 File[] files = f.listFiles();
                 for (File file : files) {
                     try {
                         if (file.isDirectory()) {
                         } 
                         else {
                             file.delete();
                         }

                     } 
                     catch (Exception ex) {
                         System.err.println("Erro ao apagar ficheiro da directoria");
                         continue;
                     }
                 }
             }
         }
     }
    
    //Quando existe uma quebra de ligacao TCP entre um servidor primario e o secundario
    private void criarNovaLigacao(){   
        if(this.threadRecebeHearBeats != null){
            this.threadRecebeHearBeats.setCriarConexao(false);
        }
    }
    
    //Criar conexao
    private synchronized void criarConexao(DadosDoServidor dados){
        comparatorServidor comp = new comparatorServidor();

        //Criar conexao
        if(comp.compare(this.dados, dados) != 0){
            try{
                this.socketPrimario = new Socket(dados.getIp().getHostAddress(), dados.getServicePort());
                this.apagaFicheiros();
                this.threadRecebeDadosPrimario = new recebeDadosDoServidorPrimario(this.socketPrimario);
                this.threadRecebeDadosPrimario.start();
                
                 try{
                    MsgServidorSecundario msg = new MsgServidorSecundario();
                    msg.setComando("CopyAll");
                    ObjectOutputStream out = new ObjectOutputStream( this.socketPrimario.getOutputStream());
                    out.writeObject(msg);
                    out.flush();
                    
                    while( this.threadRecebeDadosPrimario.getFimDaCopyall() == false){
                        Thread.sleep(1000);
                    } 
                }
                catch(IOException ob){
                    System.err.println("Erro ao enviar dados ao servidor Primario");
                     this.criarNovaLigacao();
                } 
                 catch (InterruptedException ex) {
                     System.err.println("Sleep foi interrompido");
                }
            }
            catch(IOException ob){
                System.err.println("Erro ao criar conexao ao Servidor Primario");
                this.reconfigura();
            }
        } 
    }
    //############################################################
    //#       Thread para receber dados do Servidor Primario     #
    //############################################################
    class recebeDadosDoServidorPrimario extends Thread{
         //Atributos
         private Socket socket;
         private boolean run = true;
         private ObjectInputStream in = null;
         private ObjectOutputStream out;
         private gravarFicheiros gravar = null;
         private Boolean fimDaCopyall = false;
         private gravarFicheirosUpload gravarUpload = null;

         //Construtor
         public recebeDadosDoServidorPrimario(Socket socket) {
            this.socket = socket;

         }
         
      
        //#       TROCA DE INFO ENTRE PRIMARIO ---> SECUNDARIO       #

        public Boolean getFimDaCopyall() {
            return fimDaCopyall;
        }
        
        //ficheiro que estou a copiar deve ser apagado
        private void verificaEsteFile(MsgServidorSecundario ob, List<MsgServidorSecundario> conjunto) {

            for(int i = 0; i < conjunto.size(); i++){
                if(conjunto.get(i).getFileName().contains(ob.getFileName()) == true){
                   try {
                       ob.setComando("EliminacaoNegativa");
                        out = new ObjectOutputStream(this.socket.getOutputStream());
                        out.writeObject(ob);
                        out.flush();
                    } 
                   catch (IOException ex) {
                         System.err.println("Erro ao enviar pedido");
                    }
                    break;
                }
            }
        }
          
        //Copiar ficheiros
        private void copiarFicheiros() throws IOException,ClassNotFoundException{
            //guardar o nome
            boolean condicao = true;
          
            
            List<MsgServidorSecundario> conjunto = new ArrayList<>();
            while (condicao) {
                double  tempoPrimario = System.currentTimeMillis();
                contaHeartBeadtsPrimario.setTempo(tempoPrimario);
                this.in = new ObjectInputStream(this.socket.getInputStream());

                Object returnObj = this.in.readObject();

                if (returnObj instanceof MsgServidorSecundario) {

                    MsgServidorSecundario ob = (MsgServidorSecundario) returnObj;
                    //System.out.println("A gravar ficheiro " + ob.getFileName() + ob.getFimDaEscrita());
                    conjunto.add(ob);
                  
                    if (ob.getFimDaEscrita() == false) {
                        condicao = false;
                    }          
                }
                in = null;
                returnObj = null;
                System.gc();
             
            }
            this.gravar = new gravarFicheiros(conjunto);
            this.gravar.start();
        }
         
        //Copiar ficheiros
        private void copiarFicheirosUpload() {
            try {
                //guardar o nome
                boolean condicao = true;

                List<MsgServidorSecundario> conjunto = new ArrayList<>();
                while (condicao) {
                    double tempoPrimario = System.currentTimeMillis();
                    contaHeartBeadtsPrimario.setTempo(tempoPrimario);
                    this.in = new ObjectInputStream(this.socket.getInputStream());

                    Object returnObj = this.in.readObject();

                    if (returnObj instanceof MsgServidorSecundario) {

                        MsgServidorSecundario ob = (MsgServidorSecundario) returnObj;
//                    System.out.println("A gravar ficheiro " + ob.getFileName() + ob.getFimDaEscrita());
                        conjunto.add(ob);

                        if (ob.getFimDaEscrita() == false) {
                            condicao = false;
                        }
                    }
                    in = null;
                    returnObj = null;
                    System.gc();
                }
                this.gravarUpload = new gravarFicheirosUpload(conjunto);
                this.gravarUpload.start();
            } 
            catch (ClassNotFoundException ob) {
                System.err.println("Erro -" + ob);
            } 
            catch (IOException ex) {
                System.err.println("Erro - "+ex);
            }
        }
           
        //enviar pedido
        private void enviarCommitPositivoOuNegativo(Boolean flag, MsgServidorSecundario msg){
            if(flag == true){//EliminacaoNegativa
                msg.setComando("EliminacaoNegativa");
            }
            else{//EliminacaoPositiva
                msg.setComando("EliminacaoPositiva");
            }
            //enviar pedido 
            try{
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
            }
            catch(IOException ob){
                System.err.println("Erro eo enviar enviarCommitPositivoOuNegativo");
            }
        }
        
          //enviar pedido
        private void enviarCommitPositivoOuNegativoUpload(Boolean flag, MsgServidorSecundario msg){
            if(flag == true){//EliminacaoNegativa
                msg.setComando("UploadNegativo");
            }
            else{//EliminacaoPositiva
                msg.setComando("UploadPositivo");
            }
            //enviar pedido 
            try{
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
            }
            catch(IOException ob){
                System.err.println("Erro eo enviar enviarCommitPositivoOuNegativo");
            }
        }
        
        //envia confirmacaoApagaFicheiro
        private void enviaConfirmacaoAPagaFicheiro(MsgServidorSecundario msg){
            
            msg.setComando("FicheiroApagadoComSucesso");
            //enviar pedido 
            try{
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
            }
            catch(IOException ob){
                System.err.println("Erro eo enviar enviarCommitPositivoOuNegativo");
            }
        }
       
         //tornar ficheiro visivel
        private void changeFile(String nome, Boolean condicao){
             File f = null;
             
             try{
                 f = new File(directoria+File.separator+nome.trim());
                 f.setReadable(condicao,condicao);
                 f.setWritable(condicao,condicao);
             }
             catch(Exception ob){
                 System.err.println("Erro ao mudar permissoes do ficheiro "+nome);
             }
        }
           
        //"EliminacaoCommit"
        private void verificaComando(MsgServidorSecundario msg) {
            try {
                if (msg.getComando().contains("CopiaDoPrimario") == true) {
                    this.copiarFicheiros();
                }
                else if (msg.getComando().contains("UploadDoPrimario") == true) {
                    if(procura(msg.getFileName()) == false){//Esta tudo bem
//                        System.out.println("Esta tudo bem pedido enviado ---->  "+msg.getFileName());
                        this.copiarFicheirosUpload();
                        setFicheiroEmCausa(msg.getFileName());
                        this.enviarCommitPositivoOuNegativoUpload(false, msg);
//                        System.out.println("Acabei a copia ---->  "+msg.getFileName());
                    }
                    else{// nao esta tudo bem
                        this.enviarCommitPositivoOuNegativoUpload(true, msg);
                        setFicheiroEmCausa(" ");
                        this.apagaFicheiro(msg.getFileName());
                    }
                }
                else if(msg.getComando().contains("CommitUpload") == true){//alterar permissoes do ficheiro
//                    System.out.println("Recebi um commit upload positivo");
                    this.changeFile(msg.getFileName(),true);
                    //Enviar para todos os clientes neste caso um pedido para verrem o novo ficheiro ficheiro
                    notificaClientes(msg.getFileName(),"NovoFicheiro");
                }
                else if(msg.getComando().contains("CommitUploadNegativo") == true){//ficheiro deve ser apagado
//                    System.out.println("Recebi um commit upload Negativo");
                    
                    this.apagaFicheiro(msg.getFileName());
                }
                else if(msg.getComando().contains("FimCopyAll") == true){//"FimCopyAll"
                    this.fimDaCopyall = true;
                }
                else if (msg.getComando().contains("EliminacaoCommit") == true){
//                    System.out.println("Recebi este pedido de eliminacao do server Primario");
//                    System.out.println(msg.getComando()+msg.getFileName());
                    //verificar se algum esta a usar esse comando
                    if(procura(msg.getFileName()) == true){
//                        System.out.println("Alguem esta a copiar");
                        //enviar para server pedido negativo
                        this.enviarCommitPositivoOuNegativo(true, msg);
                    }
                    else{
//                        System.out.println("Esta tudo bem ");
                        //guardar o ficheiro em causa
                        //enviar para Server pedido positivo
                        setFicheiroEmCausa(msg.getFileName());
                        
                        this.enviarCommitPositivoOuNegativo(false, msg);
                    }
                }
                else if(msg.getComando().contains("Commit") == true){
//                    System.out.println("Vou apagar este ficheiro "+msg.getFileName());
                    //vou apagar o ficheiro
                    this.apagaFicheiro(msg.getFileName());
                    //enviar para serverPrimario confirmacao que o ficheiro foi apagado
                    this.enviaConfirmacaoAPagaFicheiro(msg);   
                    setFicheiroEmCausa(" ");
                    //Enviar para todos os clientes neste caso um pedido para apagar ficheiro
                    notificaClientes(msg.getFileName(),"ApagarFIcheiro");
                }
            }
            catch (IOException ex) {
                System.err.println("erro -"+ ex);
            } 
            catch (ClassNotFoundException ex) {
                System.err.println("Erro - "+ex);
            } 
            
        }
        
          //Apagar ficheiro
        private void apagaFicheiro(String ficheiro) {
            String nome = ficheiro;
            try {
                File f = new File(directoria.getCanonicalPath()); // current directory // current directory

                File[] files = f.listFiles();

                for (File file : files) {

                    if (!file.isDirectory()) {
                        // System.out.print("directory:");
                        String name = file.getName().trim();
                        if (nome.contains(name) == true) {
                            //enviar os dados para o cliente
                            file.delete();
                        }
                    }
                }
            } 
            catch (IOException ex) {
                System.err.println("Erro ao apagar ficheiro directoria");
            }
        }
         
        //comando enviado pelo primario para copiar ficheiro
        @Override
        public void run() {
            if (this.socket == null) {
                return;
            }
            while (this.run) {
                try {
                   
                    this.in = new ObjectInputStream(this.socket.getInputStream());
        
                    Object obj = this.in.readObject();

                    if (obj instanceof MsgServidorSecundario) {
                        MsgServidorSecundario msg = (MsgServidorSecundario) obj;
                        this.verificaComando(msg);
                    }

                    this.in = null;
                } 
                catch (ClassNotFoundException ob) {
                    System.err.println("Erro ao receber objeto do servidor primario");
                    try {
                        System.err.println("Erro ao receber dados do servidor primario");
                        this.run = false;

                        this.socket.close();
                        this.fimDaCopyall = true;
                    } 
                    catch (IOException oB) {
                        System.err.println("Erro ao fechar socket");
                    }
                } 
                catch (IOException ex) {
                    try {
                        System.err.println("Erro ao receber dados do servidor primario");
                        this.run = false;

                        this.socket.close();  
                        this.fimDaCopyall = true;
                    } 
                    catch (IOException ob) {
                        System.err.println("Erro ao fechar socket");
                    }
                }
            }
//            System.out.println("Fim da thread");
        }
     }
     
    //Thread para copiar ficheiros
    class gravarFicheiros extends Thread{
         //Atributos
         private List<MsgServidorSecundario> lista= null;
         
         
         //Construtor
         public gravarFicheiros(List<MsgServidorSecundario> listaTemp){
             //Copiar ficheiro
             this.lista = new ArrayList<>();
             for (MsgServidorSecundario ob : listaTemp) {
                 this.lista.add(ob);
             } 
         }
         
         
         @Override
         public void run(){
             
             if(this.lista.size() == 0){
                 return;
             }
             
            //convert array of bytes into file
	    FileOutputStream fileOuputStream;
             try {
                 //fileOuputStream = new FileOutputStream(directoria + "\\" + this.lista.get(0).getFileName());
             
                 fileOuputStream = new FileOutputStream(directoria+File.separator+this.lista.get(0).getFileName());
                 for (MsgServidorSecundario dados : this.lista) {
                    if(dados.getNbytes() > 0){
                       fileOuputStream.write(dados.getbFile(), 0, dados.getNbytes());
                    }
                 }
                 fileOuputStream.close();
             } 
             catch (FileNotFoundException ex) {
                System.err.println("Erro ao gravar "+this.lista.get(0).getFileName());
             } 
             catch (IOException ex) {
                 System.err.println("Erro ao escrver ficheiro");
             }          
             return;
         }
     }
     
    //Thread para copiar ficheiros
    class gravarFicheirosDoCliente extends Thread{
         //Atributos
         private List<MsgCliente> lista= null;
         
         
         //Construtor
         public gravarFicheirosDoCliente(List<MsgCliente> listaTemp){
             //Copiar ficheiro
             this.lista = new ArrayList<>();
             for (MsgCliente ob : listaTemp) {
                 this.lista.add(ob);
             } 
         }
         
         
         @Override
         public void run(){
             
             if(this.lista.size() == 0){
                 return;
             }
             
            //convert array of bytes into file
	    FileOutputStream fileOuputStream;
             try {
                 fileOuputStream = new FileOutputStream(directoria+File.separator+this.lista.get(0).getFileName().trim());
                 for (MsgCliente dados : this.lista) {
                    if (dados.getFimDaEscrita() == true) {
                         if (dados.getNbytes() > 0) {
                             fileOuputStream.write(dados.getbFile(), 0, dados.getNbytes());
                        }
                    }
                 }
                 fileOuputStream.close();
             } 
             catch (FileNotFoundException ex) {
                 System.err.println("Erro ao gravar "+this.lista.get(0).getFileName());
             } 
             catch (IOException ex) {
                 System.err.println("Erro ao escrever ficheiro");
             }
                              
             return;
         }
     }
    
     //Thread para copiar ficheiros em caso de upload sem permissoes
    class gravarFicheirosUpload extends Thread{
         //Atributos
         private List<MsgServidorSecundario> lista= null;
         
         
         //Construtor
         public gravarFicheirosUpload(List<MsgServidorSecundario> listaTemp){
             //Copiar ficheiro
             this.lista = new ArrayList<>();
             for (MsgServidorSecundario ob : listaTemp) {
                 this.lista.add(ob);
             } 
         }
         
         
         @Override
         public void run(){
             
             if(this.lista.size() == 0){
                 return;
             }
             
            //convert array of bytes into file
	    FileOutputStream fileOuputStream;
             try {
                 File F = new File(this.lista.get(0).getFileName());
                 fileOuputStream = new FileOutputStream(directoria+File.separator+F.getName());
                 for (MsgServidorSecundario dados : this.lista) {
                    if (dados.getFimDaEscrita() == true) {
                         if (dados.getNbytes() > 0) {
                             fileOuputStream.write(dados.getbFile(), 0, dados.getNbytes());
                        }
                    }
                 }
                 fileOuputStream.close();
                 //Mudar as permissoes
                 File f = new File(directoria+File.separator+F.getName());
                 f.setReadable(false,false);
                 f.setWritable(false, false);
             } 
             catch (FileNotFoundException ex) {
                 System.err.println("Erro ao gravar "+this.lista.get(0).getFileName());
             } 
             catch (IOException ex) {
                 System.err.println("Erro ao escrever ficheiro");
             }
                         
             return;
         }
     }
    
    //############################################################
    //#            CRIAR CONEXAO COM O SERVIDOR PRIMARIO         #
    //############################################################
    
    //reconfigurar o sistema qunado o Primario deixa de enviar 
    private void reconfigura(){
        try {
//            System.out.println("Em reconfiguracao");
            this.threadRecebeHearBeats.requestStop2();
            this.threadEnviaHeartebeast.requestStop();
            this.listaDeClientes.clear();
            this.listaDeServidores.clear();
            synchronized(dados){
                this.dados.setEstadoDeArranque(false);
                this.dados.setTipoHearBeat(1);
            }
            this.socketPrimario = null;
            this.Primario = null;
            this.tempoDecisao = true;
            this.estabelecerConexao = false;
            this.podenEnviar = false;
            //Lancar threads
            this.threadPeriodos = new ContaPeriodos();
            this.threadPeriodos.start();
            //lancar thread do grupo multicast para receber
            InetAddress group = InetAddress.getByName(HostDoGrupo);
            this.threadRecebeHearBeats = new ThreadHeartbeats(socketUdpDirectoria, group, porto);
            this.threadRecebeHearBeats.start();
            //lancar para enviar
            this.threadEnviaHeartebeast = new enviaHeartbeats(socketUdpDirectoria, group, porto);
            this.threadEnviaHeartebeast.start();
           
            System.gc();
            //Enviar para clientes sms a dizer estou em configuração
            
        } 
        catch (UnknownHostException ex) {
            System.err.println("Erro ao reconfigurar");
            //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //############################################################
    //#            CONTA O TEMPO DO SERVIDOR PRIMARIO            #
    //############################################################
    class ContaHeartBeadtsPrimario extends Thread{
        //Atributos
        private boolean running = true;
        private double tempo = System.currentTimeMillis();
        private double tempoPrimario = 0.0f;
        
        //Construtor
        public ContaHeartBeadtsPrimario() {

        }
        
        private synchronized void setTempo(double tempoPrimario ){
            this.tempoPrimario = tempoPrimario;
        }
        
        //Criei um stop
        public void requestStop(){
            this.running = false;
        }
        
        @Override
        public void run() {
            while (this.running == true) {
                double tempo1=System.currentTimeMillis();
                double tempofinal=(tempo1)-(tempoPrimario);
//                System.out.println("Tempo "+tempofinal);
                if(tempofinal > 18000 && tempofinal < 60000){//Acabou os 15 segundos
                    this.running = false; 
                    reconfigura();
                    synchronized(dados){
//                        System.out.println("Em configuracao");
                        dados.setEmConfiguracao(true);
                    }
                    return;
                }
               
                try {
                    Thread.sleep(1000);
                } 
                catch (InterruptedException ob) {
                    ob.printStackTrace();
                }
            }
            synchronized(podenEnviar){//O servidor pode comecar a enviar os heartbeats
                podenEnviar = true;
            }
            return;
        }
    }
    
    //############################################################
    //#                  THREAD  TEMPO DE DECISAO                #
    //############################################################
    class Decisao extends Thread{
           //Atributos
           private boolean running = true;
           private boolean decisao,estabelecerConexao;

           public Decisao(boolean d, Boolean ebc) {
               this.decisao = d;
               this.estabelecerConexao = ebc;
           }


           public void requestStop(){
               this.running = false;
               this.stop();
           }

           @Override
           public void run() {
              double tempo = System.currentTimeMillis();
               while (this.running == true) {

                   double tempo1=System.currentTimeMillis();
                   double tempofinal=(tempo1)-(tempo);

                   if(tempofinal > 15000){//Acabou os 15 segundos
                       this.running = false;
                       mudaCondicoes();
                   }

                   try {
                       Thread.sleep(1000);
                   } 
                   catch (InterruptedException ob) {
                       ob.printStackTrace();
                   }
               }
               synchronized(podenEnviar){//O servidor pode comecar a enviar os heartbeats
                   podenEnviar = true;
               }
               return;
           }
       }
    
    //muda condicoes
    private synchronized void mudaCondicoes() {
        tempoDecisao = false;
        estabelecerConexao = false;
    }
    
    //############################################################
    //#                  THREAD  Conta PERIODOS                  #
    //############################################################
    class ContaPeriodos extends Thread{
        //Atributos
        private boolean running = true;

        //Construtor
        public ContaPeriodos() {
            
        }
        
        //Criei um stop
        public void requestStop(){
            this.running = false;
        }
        
        @Override
        public void run() {
           double tempo = System.currentTimeMillis();
            while (this.running == true) {

                double tempo1=System.currentTimeMillis();
                double tempofinal=(tempo1)-(tempo);
              
                if(tempofinal > 15000){//Acabou os 15 segundos
                    this.running = false;    
                }
               
                try {
                    Thread.sleep(1000);
                } 
                catch (InterruptedException ob) {
                    ob.printStackTrace();
                }
            }
            synchronized(podenEnviar){//O servidor pode comecar a enviar os heartbeats
                podenEnviar = true;
            }
            return;
        }
    }
    
    //parar a ContaPERIODOS  
    private void stopTread() {
        this.threadPeriodos.requestStop();
        this.running = false;
        runingTcp = false;
        synchronized(podenEnviar){//O servidor pode comecar a enviar os heartbeats
           podenEnviar = true;
        }
    }
   
    //############################################################
    //#               Comparator comparaPortos                   #
    //############################################################
    class comparaPortos implements Comparator<Integer>{
        @Override
        public int compare(Integer o1, Integer o2) {
             if (o1 == o2) {
                return 0;
            }
            else if (o1 > o2) {
                return 1;
            }
            else {
                return -1;
            }
        }
    
    }
        
    //############################################################
    //#               Comparator DadosDoServidor                 #
    //############################################################
   class comparatorServidor implements Comparator<DadosDoServidor>{
        
        @Override
        public int compare(DadosDoServidor d1, DadosDoServidor d2){
            int ip1 = 0, ip2 = 0;
            
            ip1 = gettotalIp(d1.getIp().getHostAddress());
            ip2 = gettotalIp(d2.getIp().getHostAddress());

//            System.out.println("ip1 "+ip1+ " <----> Ip2 "+ip2);
            if(ip1 == ip2){
                //Comparar os portos
                if(d1.getServicePort() == d2.getServicePort()){
                    return 0;
                }
                else if (d1.getServicePort() > d2.getServicePort()){
                    return -1;
                }
                else{
                    return 1;
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
   
    //############################################################
    //#               Comparator de Clientes                     #
    //############################################################
    class comparaClientes implements Comparator< threadAtendimento>{

        @Override
        public int compare(threadAtendimento o1, threadAtendimento o2) {
           if(o1.getNumero() == o2.getNumero()){
               return 0;//iguais
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
    
    //Adicionar servidores 
    private void  addServidores(DadosDoServidor ob){
        boolean flag = false;
        synchronized (this.listaDeServidores) {
            comparatorServidor comp = new comparatorServidor();

            for (int i = 0; i < this.listaDeServidores.size(); i++) {
                if (comp.compare(this.listaDeServidores.get(i), ob) == 0) {
                    flag = true;
                }
            }

            if (flag == false) {
                this.listaDeServidores.add(ob);
            }       
        }
    }
   
    //############################################################
    //#                     MyException                          #
    //############################################################
    //minha exception 
     public class MyException extends IOException {
        // TODO Auto-generated constructor stub

        public MyException(String message) {
            super(message);
            // TODO Auto-generated constructor stub
        }

        public MyException(Throwable cause) {
            super(cause);
            // TODO Auto-generated constructor stub
        }

        public MyException(String message, Throwable cause) {
            super(message, cause);
            // TODO Auto-generated constructor stub
        }
    }
    //########################## METODOS ###################################
}