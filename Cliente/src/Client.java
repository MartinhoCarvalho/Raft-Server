import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    //Atributos 
    private String[] args   = null;
    private File directoria = null;
    private String erroDirectoria = null;
    private DatagramSocket socketUdpDirectoria = null;
    private Scanner scan;
    private int ServicePortodiectory = 0;
    private InetAddress inet = null;
    private Login login;
    private Boolean condicaoParagem = true;
    private static final int maxSize = 4096,Timeout = 20000;
    private ObjectInputStream in = null, intTcp;
    private ObjectOutputStream out = null, outTcp;
    private DatagramPacket packet = null;
    private Socket socket = null;
    private boolean running = true;
    private MsgCliente msgcliente = null;
    private recebeDados ThreadRecebDadosServidor = null;
    private Boolean possoVisualizarFicheiro = false;
    private Boolean pedidoIgnorado = false;
    private int totalFicheiros = 0 ;
    private static int MAXSIZEFILE = 4096;
    private Boolean podeEnviarFicheiro = true;
    private Boolean ativaStopLogin = false;
    
    //Construtor por defeito
    public Client(){
        this.scan = new Scanner(System.in);
    }

    //Construtor por parametros
    public Client(String[] args) {
        this.args = args;
        this.inicializaAtributos();
        this.scan = new Scanner(System.in);
    }

    //######################################################################
    //#                          METODOS                                   #
    //######################################################################
    private void inicializaAtributos(){
       
        this.login = new Login(" ", "" );
        
        if(this.args.length < 3){
            System.err.println("Error wrong syntax <Directory> <IP> and <port> the directory service");
            System.err.println("The client is finished");
            System.exit(-1);
        }
        
        
       //verificar se a direcoria em questao existe e se tem permissoes de escrita
       this.directoria = new File(this.args[0]);
       this.msgcliente = new MsgCliente();
       this.checkDirectory();//verificar se a directoria tem erro
       this.apagarDirectoria();
       
       try{
           this.inet = InetAddress.getByName(this.args[1]);
           this.ServicePortodiectory = Integer.parseInt(this.args[2]);
       }
       catch(UnknownHostException ob){
           System.err.println("Error create ip service");
           System.exit(-1);
       }
    }
    
    //Cliente run
    public void Run(){
        //Criar socket de conexao UDP
        this.running = true;
         this.condicaoParagem = true;
        try{
            this.socketUdpDirectoria = new DatagramSocket();
            //this.socketUdpDirectoria.setSoTimeout(Timeout * 1000);           

            this.packet = new DatagramPacket(new byte[maxSize], maxSize,inet,this.ServicePortodiectory);
        }
        catch(IOException ob){
            System.err.println("Error create socket");
            return;
        }
        while (this.condicaoParagem == true) {
            if(this.login.getNaoExistemServidores() == true){
                System.out.println("Nao existem servidores Ligados");
            }

            if(this.login.getNaoExisteNenhumServidorAtivo() == true){
                System.out.println("Warning nao existe nenhum servidor em funcionamento");
            }
            
            //estados do login
            if (this.login.getEstado() instanceof AccountNormal) {//Estado normal
                this.estadoDefault();
                this.enviarDadosDologin();
                this.receberDados();
            } 
            else if (this.login.getEstado() instanceof CreateNewAccount) {
                this.criarConta();
                this.enviarDadosDologin();
                this.receberDados();
            } 
            else if (this.login.getEstado() instanceof LoginAccount) {
                this.loginCliente();                
                this.enviarDadosDologin();
                this.receberDados();          
            } 
            else{
                this.condicaoParagem = false;
            }
        }
 
       this.closeSocketUdp();
       this.criarSocketTcpServidor();//Criar conexao com o serviodr
       this.menuServidor();
    }
    
    //Menu de opcoes do cliente
    private void estadoDefault(){
        int op  = 0;
        
        System.out.println("+------------------------------------------------------+");
        System.out.println("|   Sistema de Armazenamento de ficheiros Fase Login   |");
        System.out.println("|   1- Criar conta                                     |");
        System.out.println("|   2- Efetuar login                                   |");
        System.out.println("|   3-Sair                                             |");
        System.out.println("+------------------------------------------------------+");
        System.out.print("> ");
        while(! this.scan.hasNextInt()){//para retirar bugs do jogo
            scan.next();//pede um inteiro
        }
        op = this.scan.nextInt();    

        if(op == 3){//sair
            this.sair();
            return;
        }
        
        this.login.setOpcao(op);
    }
    
    //Criar conta
    private void criarConta(){
        String nome;
        System.out.println("----- Criar Conta ---------");
        System.out.println("Inserir Username ");
        System.out.print("> ");
        nome = this.scan.next();
        this.login.setUsername(nome);
       
        String password;
        System.out.println("Inserir Password");
        System.out.print("> ");
        password = this.scan.next();
        this.login.setPassword(password);
    }
    
    //login
    private void loginCliente(){
        String nome;
        System.out.println("----- Efetuar login ---------");
        System.out.println("Inserir Username ");
        System.out.print("> ");
        nome = this.scan.next();
        this.login.setUsername(nome);
       
        String password;
        System.out.println("Inserir Password");
        System.out.print("> ");
        password = this.scan.next();
        this.login.setPassword(password);
    }
      
    //Verifica as permissoes do sistema
    private void checkDirectory(){
        if (!this.directoria.exists()) {
            System.err.println("A directoria " + this.directoria + " nao existe!");
            System.exit(-1);
        }

        if (!this.directoria.isDirectory()) {
            System.err.println("O caminho " + this.directoria + " nao se refere a uma directoria!");
            System.exit(-1);
        }

        if (!this.directoria.canWrite()) {
            System.err.println("Sem permissoes de escrita na directoria " + this.directoria);
            System.exit(-1);
        }
    }
    
    //enviar para o servico
    private void enviarDadosDologin() {
        try {     
            ByteArrayOutputStream  buff = new ByteArrayOutputStream();
            out = new ObjectOutputStream(buff);
            out.writeObject(this.login);
            out.flush();
            out.close();
            this.packet.setData(buff.toByteArray());
            packet.setLength(buff.size());
   
            this.socketUdpDirectoria.send(packet);
        } catch (IOException ex) {
            System.err.println("Erro ao enviar Login");
        }
    }
    
    //receber login e dados do servico
    private void receberDados() {
        this.in = null;
        this.packet.setData(new byte[maxSize], 0, maxSize);
        try {
            this.socketUdpDirectoria.setSoTimeout(Timeout);
            this.socketUdpDirectoria.receive(this.packet);

            this.in = new ObjectInputStream(new ByteArrayInputStream(
                    this.packet.getData(),
                    0,
                    this.packet.getLength()));

            Object obj;

            obj = (Login) in.readObject();
            this.in.close();

            if (obj instanceof Login){
                this.login = (Login)obj;
                
            }
        } 
        catch (ClassNotFoundException ex) {
            System.err.println("Erro ao receber login");
          
        } 
        catch (IOException ex) {

             System.err.println("Erro ao receber");
             System.exit(-1);
        }
    }
    
    //sair
    private void sair(){
        try{
            System.gc();
        }finally{
            this.socketUdpDirectoria.close();
            this.packet = null;
            this.condicaoParagem = false;
            if(this.socket != null){
                try{
                    this.socket.close();
                }
                catch(IOException ob){
                    System.err.println("Erro ao fechar socket servidor");
                }
            }
            System.gc();
            System.exit(0);
        }
    } 
        
    //############################################################
    //#                 MENU DE OPCOES SERVIDOR                  #
    //############################################################
    //Menu principal
    public void menuServidor(){
        int op;
        while (this.running) {
            System.out.println("Nome:> "+this.login.username);
            System.out.println("+------------------------------------------------------+");
            System.out.println("|   Sistema de Armazenamento de ficheiros              |");
            System.out.println("|   1- Upload                                          |");
            System.out.println("|   2- Download                                        |");
            System.out.println("|   3- Eliminacao                                      |");   
            System.out.println("|   4- Visualizacao                                    |");   
            System.out.println("|   5- Sair                                            |");
            System.out.println("+------------------------------------------------------+");
            System.out.print("> ");
            while (!this.scan.hasNextInt()) {//para retirar bugs do jogo
                scan.next();//pede um inteiro
            }
            op = this.scan.nextInt();

            //verificar qual deve ser o comando a enviar
            //Opcoes dos utilizadores
            if(op == 1){//Upload
               this.Upload();
            }
            else if(op == 2){//Download
                this.menuDonwload();
            }
            else if(op == 3){//Eliminacao
                 this.DeleteFile();
            }
            else if(op == 4){//Listar
                this.visualizarFicheiro();
            }
                      
            if (op == 5) {//sair
                this.ThreadRecebDadosServidor.setAtivaStopLogin(true);
                this.comandoSair();
                this.escreverParaOServidor();
                this.closeSocketUdpLogout();
                this.closeConexaoDoTcp();
                System.gc();
                //System.exit(0);
                this.running = false;
            }
        }
        this.Run();//so passa aqui quando faz logout
    }
    
    // menu download
    private void menuDonwload() {
        this.ListarFicheiros();
        this.escreverParaOServidor();

        //Esperar para receber dados do servidor so para ficar sincronizado
        try {
            Thread.sleep(1500);
        } 
        catch (InterruptedException ex) {
            System.err.println("Erro interrupcao no server");
        }
        
        if(ThreadRecebDadosServidor.getEstadoDoServidor() == true){
            System.out.println("Servidor em configuracao");
            return;
        }
        this.totalFicheiros = this.ThreadRecebDadosServidor.getTotalFicheiros();
        
        if (this.pedidoIgnorado == false && this.totalFicheiros > 0){
            System.out.println("Pretende realizar algum download de ficheiro sim|nao ?");
            String rsp = this.scan.next();
            if (rsp.contains("sim") == true || rsp.contains("SIM") == true) {
                System.out.println("Introduza o nome do ficheiro que pretende ver");
                String nome = this.scan.next();
                //Verificar se tenho este file na minha directoria
                if (this.procuraFicheiro(nome) == false) {
                    //realizar download
                    this.comandoDownload(nome);
                    this.escreverParaOServidor();
                } 
                else {// true
                    System.out.println("Ja possui esse ficheiro " + nome);
                }
            }
        }
        this.comandoVazio(" ");
        this.escreverParaOServidor();
    }
    
    //CarregarFicheiro
    private void Upload() {
       this.comandoVazio(" ");
       this.escreverParaOServidor();
        
        //Esperar para receber dados do servidor so para ficar sincronizado
        try {
            Thread.sleep(1500);
        } 
        catch (InterruptedException ex) {
            System.err.println("Erro interrupcao no server");
        }
        
        if(ThreadRecebDadosServidor.getEstadoDoServidor() == true){
            System.out.println("Servidor esta configuracao");
            return;
        }
        
        System.out.println("Introduza o caminho do ficheiro a carregar");
        String nomeFicheiro = this.scan.next();
        //verificar se o ficheiro existe
       try{
           this.procuraOFicheiro(nomeFicheiro);
           this.msgcliente.setComando("Carregar");
           File directoriaTemp = new File(nomeFicheiro.trim());
           this.msgcliente.setFileName(directoriaTemp.getName());
           this.escreverParaOServidor();
           
            FileInputStream fileInputStream = null;

            byte[] bFile = new byte[MAXSIZEFILE];

            try {
                //convert file into array of bytes
                MsgCliente msg = new MsgCliente();
                msg.setComando("Carregar");
                msg.setFileName(nomeFicheiro.trim());
                //depois comecar a copia dos bytes para o servidor
                FileInputStream fileIn;
                fileIn = new FileInputStream(nomeFicheiro);

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
                nbytes = 0;
                msg.setNbytes(0);
                this.out = new ObjectOutputStream(this.socket.getOutputStream());
                this.out.writeObject(msg);
                this.out.flush();
                
                System.gc();//Libertar memoria
            } 
            catch (IOException ob) {
                System.err.println("Erro ao copiar ficheiro");
            }
       } 
       catch(IOException ob){
           System.out.println("Ficheiro nao existe");
       }
       this.comandoVazio(" ");
       this.escreverParaOServidor();
    } 
   
    //procura o ficheiro
    private void procuraOFicheiro(String ficheiro)throws IOException{
       File localDirectoria = new File(ficheiro);

       if(! localDirectoria.exists()){
           throw new IOException();
       }
    }
    
    //Eliminacao
    private void DeleteFile(){
        this.ListarFicheiros();
        this.escreverParaOServidor();
        
        //Esperar para receber dados do servidor so para ficar sincronizado
        try {
            Thread.sleep(1500);
        } 
        catch (InterruptedException ex) {
            System.err.println("Erro interrupcao no server");
        }
        
        if(ThreadRecebDadosServidor.getEstadoDoServidor() == true){
            System.out.println("Servidor em configuracao");
            return;
        }
        
        this.totalFicheiros = this.ThreadRecebDadosServidor.getTotalFicheiros();
        
        if (this.pedidoIgnorado == false && this.totalFicheiros > 0){
            System.out.println("Introduza o nome do ficheiro a apagar");
            String nome = this.scan.next();
            this.msgcliente.setComando("Eliminacao");
            this.msgcliente.setFileName(nome);
            this.escreverParaOServidor();
        }
    }
    
    //Apagar directoria
    private void apagarDirectoria(){
         File f;
        try {
            f = new File(directoria.getCanonicalPath()); // current directory // current directory
            File[] files = f.listFiles();

            for (File file : files) {

                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        } 
        catch (IOException ex) {
            //Erro ao apagar ficheiro na directoria
            System.err.println("Erro ao apagar ficheiro na directoria");
        }
    
    }
    
    //Verifica se tenho este ficheiro na directoria
    private Boolean procuraFicheiro(String fileAProcurar) {
        Boolean condicao = false;

        File f;
        try {
            f = new File(directoria.getCanonicalPath()); // current directory // current directory
            File[] files = f.listFiles();

            for (File file : files) {

                if (!file.isDirectory()) {
                    String nome = file.getName().trim();
                    //Gravar
                    if(nome.contains(fileAProcurar) == true){
                        condicao = true;
                        return condicao;
                    }
                }
            }
        } 
        catch (IOException ex) {
            System.err.println("Erro a procura ficheiro "+ fileAProcurar);
        }

        return condicao;
    }
    
    //modo de visualizar ficheiro
    private void visualizarFicheiro() {
        Boolean cond = true;
       
        this.ListarFicheiros();//listar ficheiros do servidor
        this.escreverParaOServidor();//enviar dados
       
        //Esperar para receber dados do servidor so para ficar sincronizado
        try {
             Thread.sleep(1500);
        } 
        catch (InterruptedException ex) {
            System.err.println("Erro interrupcao no server");
        }
        
        if(ThreadRecebDadosServidor.getEstadoDoServidor() == true){
            System.out.println("Servidor esta configuracao");
            return;
        }
       
        this.totalFicheiros = this.ThreadRecebDadosServidor.getTotalFicheiros();

        if (this.pedidoIgnorado == false && this.totalFicheiros > 0){// se tiver ficheiros no servidor
            System.out.println("Pretende visualizar algum ficheiro sim|nao ?");
            String rsp = this.scan.next();
            if (rsp.contains("sim") == true || rsp.contains("SIM") == true) {
                System.out.println("Introduza o nome do ficheiro que pretende ver");
                String nome = this.scan.next();
                System.out.println("Introduza o nome do editor em que deseja editar o file");
                String editor = this.scan.next();

                //Verificar se tenho este file na minha directoria
                if (this.procuraFicheiro(nome) == false) {
                    //realizar download
                    this.possoVisualizarFicheiro = false;
                    this.comandoDownload(nome);
                    this.escreverParaOServidor();

                    while (this.possoVisualizarFicheiro == false) {//espero que o download acabe

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ob) {
                            System.err.println("Erro na htread pode visualizar");
                        }
                    }
                    //Ja posso visualizar
                    if (this.possoVisualizarFicheiro == true) {
                        this.comandoVazio(" ");
                        this.escreverParaOServidor();
                        try {
                             if (this.procuraFicheiro(nome) == true) {
                               Runtime.getRuntime().exec(editor + " " + this.directoria + "\\" + nome);
                            }
                        }
                        catch (IOException ex) {
                            System.err.println("Erro ao abrir editor do ficheiro");
                            cond = false;
                        }
                    }
                } 
                else {//encontrou o ficheiro na sua directoria 
                    try {
                        //Runtime.getRuntime().exec("notepad MyFile.txt");
                        Runtime.getRuntime().exec(editor + " " + this.directoria + "\\" + nome);
                    } 
                    catch (IOException ex) {
                        System.err.println("Erro ao abrir editor do ficheiro");
                        cond = false;
                    }
                }
            }
        }
    }

    //listar files do servidor
    private void ListarFicheiros() {
        this.msgcliente.setComando("Listar");
    }

    //Comando download
    private void comandoDownload(String nomeFicheiro) {
        this.msgcliente.setComando("Download");
        this.msgcliente.setDownload(nomeFicheiro);
    }
    
    //Comando download
    private void comandoVazio(String nomeFicheiro) {
        this.msgcliente.setComando(" ");
    }

    //comando sair
    private void comandoSair() {
        this.msgcliente.setComando("Sair");
    }

    //Escrever para o servidor
    private synchronized void escreverParaOServidor() {
        ObjectOutputStream out = null;
        try {
            //Write server
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(this.msgcliente);
            out.flush();
        } catch (IOException ob) {
            System.err.println("Erro ao enviar dados para o servidor");
            //atencao que devo verificar a conexao
        }
    }

    //Criar conexao ao servidor
    private void criarSocketTcpServidor() {
        try {
            this.socket = new Socket(this.login.getIp(), this.login.getPortoServidor());

            //criar thread para receber dados
            this.ThreadRecebDadosServidor = new recebeDados(socket);
            this.ThreadRecebDadosServidor.start();
        } 
        catch (IOException ob) {
            System.err.println("Erro ao criar socket "+ ob);
            System.exit(-1);
        }
    }

    //escrever uma sms a dizer que fiz logout
    private void dizAoServicoQueFizLogout(){
        try {     
            this.login.setLogout("Logout");
            ByteArrayOutputStream  buff = new ByteArrayOutputStream();
            out = new ObjectOutputStream(buff);
            out.writeObject(this.login);
            out.flush();
            out.close();
            this.packet.setData(buff.toByteArray());
            packet.setLength(buff.size());
   
            this.socketUdpDirectoria.send(packet);
        } 
        catch (IOException ex) {
            System.err.println("Erro ao enviar Login");
        }
    }
    
    //############################################################
    //#           THREAD PARA RECEBER DADOS  DO Servidor         #
    //############################################################
    //thread para receber dados
    class recebeDados extends Thread{
        //Atributos
        private boolean  run  = true;
        private Socket socket = null;
        private MsgCliente msg = null;   
        private gravarFicheiros gravar = null;
        private int totalFicheiros = 0 ;
        private boolean ativaStopLogin = false;
        
        //Construtor por parametros
        public recebeDados(Socket s) {
            this.socket = s;
        }
 
        //Stop thread
        public void requestStop(){
            this.run = false;
            if(this.socket != null){
                try {
                    //enviar para servico o logout da minha conta 
                   // dizAoServicoQueFizLogout();
                    this.run = false;
                    this.socket.close();
                    System.gc();
                    System.exit(0);
                } 
                catch (IOException ex) {
                    System.err.println("Erro ao fechar socket");
                }
            }
        }
        
        public void setAtivaStopLogin(boolean ativaStopLogin) {
            this.ativaStopLogin = ativaStopLogin;
        }
        
            //Stop thread
        public void parar(){
            this.run = false;
            if(this.socket != null){
                try {
                    //enviar para servico o logout da minha conta 
                   // dizAoServicoQueFizLogout();
                    this.run = false;
                    this.socket.close();
                    this.stop();
                    System.gc();
                } 
                catch (IOException ex) {
                    System.err.println("Erro ao fechar socket - "+ex);
                } 
                catch (Throwable ex) {
                    System.err.println("Erro ao finalizar a thread- "+ex);
                }
            }
        }
        
        //get estado do servidor
        public Boolean getEstadoDoServidor(){
            return this.msg.isEstadoPedido();
        }
        
        //Ler do servidor
        private synchronized void lerDoServidor() {
            try {
                //RECEIVE OBJECT  
//                System.out.println("Ler do servidor");
                ObjectInputStream in = new ObjectInputStream(this.socket.getInputStream());
                Object returnedObject = in.readObject();
                if (returnedObject instanceof MsgCliente) {
                    this.msg = (MsgCliente) returnedObject;

                    if (msg.isEstadoPedido() == true) {
                        System.out.println("Pedido foi ignorado");
                        pedidoIgnorado = true;                        
                        totalFicheiros = 0;
                    } 
                    else {
                        pedidoIgnorado = false;
                        this.verificaComando();
                    }
                }
                in = null;
                System.gc();
            } 
            catch (ClassNotFoundException ob) {
                System.err.println("Erro nao reconhece a class");
            } 
            catch (IOException ex) {
                if(this.ativaStopLogin == false){
                    this.requestStop();
                }
               this.run = false;
            }
        }
        
        //Apagar ficheiro
        private void apagaFicheiro(String ficheiro){
            if(procuraFicheiro(ficheiro) == true){//ficheiro existe
                File f = new File(directoria+File.separator+ficheiro);
                f.delete();//Apagar ficheiro
                System.out.println("Este ficheiro foi apagado "+ficheiro);
            }
        }
        
        //executar Download
        private void executaDownload(MsgCliente msg) {
            try {
                ObjectInputStream in;

                //guardar o nome
                boolean condicao = true;
                List<MsgCliente> conjunto = new ArrayList<>();
                while (condicao) {

                    in = new ObjectInputStream(this.socket.getInputStream());

                    Object returnObj = in.readObject();

                    if (returnObj instanceof MsgCliente) {
                        MsgCliente ob = (MsgCliente) returnObj;
                        conjunto.add(ob);
//                        System.out.println("Recebi chunck do servidor ");
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
                this.gravar.join();

               synchronized(possoVisualizarFicheiro){
                   possoVisualizarFicheiro = true;
               }
            }
            catch(EOFException ob){
                this.run = false;
                System.gc();
                System.exit(0);
            }
            catch (IOException ex) {
                System.err.println("Erro ao ler dados do servidor download ");
            } 
            catch (ClassNotFoundException ex) {
                System.err.println("Class nao reconhecida downalod");
            } 
            catch (InterruptedException ex) {
                System.err.println("Erro interrupcao na thread download");
            } 
        }

        //verifica comando
        private void verificaComando() {
            //Verificar se o servidor esta em configurao
            if(this.msg.isEstadoPedido() == true){
                System.out.println("Servidor em configuracao");
            }
            
            if (this.msg.getComando().contains("Listar") == true) {
                this.totalFicheiros = this.msg.getTotalFicheiros();
                this.msg.listarFiles();
            } 
            else if (this.msg.getComando().contains("Download") == true) {
                this.executaDownload(this.msg);
            }
            else if (this.msg.getComando().contains("notfound") == true){
               synchronized(possoVisualizarFicheiro){
                   possoVisualizarFicheiro = true;
               }
        
                System.out.println("Ficheiro invalido o servidor nao econtrou este ficheiro "+this.msg.getFileName());
               
            }
            else if (this.msg.getComando().contains("Ficheiro ignorado") == true){
                System.out.println("Ficheiro foi ignorado");
            }
            else if(this.msg.getComando().contains("ApagarFIcheiro") == true){//Apaga ficheiro
                //Ir a minha directoria apagar este ficheiro
                this.apagaFicheiro(this.msg.getFileName());
            }
            else if(this.msg.getComando().contains("NovoFicheiro") == true){//Existe um novo ficheiro no servidor
                System.out.println("Existe um novo ficheiro no servidor "+this.msg.getFileName());
            } 
        }

        //Total de ficheiros no servidor
        public int getTotalFicheiros(){
            return this.totalFicheiros;
        }
        
        @Override
        public void run() {
            if (this.socket == null) {
                return;
            }
            while (this.run) {
                this.lerDoServidor();
            }
            if (this.socket != null) {
                try {
             
                    this.run = false;
                    this.socket.close();
                    System.gc();
                } 
                catch (IOException ex) {
                    System.err.println("Erro ao fechar socket");
                }
            }
        }
    }
    
     //Thread para copiar ficheiros
     class gravarFicheiros extends Thread{
         //Atributos
         private List<MsgCliente> lista= null;
         
         //Construtor
         public gravarFicheiros(List<MsgCliente> listaTemp){
             //Copiar ficheiro
             this.lista = new ArrayList<>();
             for (MsgCliente ob : listaTemp) {
                 this.lista.add(ob);
             } 
         }  
         
         @Override
         public void run(){
             
             if(this.lista.size() == 0){
                 System.out.println("fim da trhead por causa do clone");
                 return;
             }
             
            //convert array of bytes into file
	    FileOutputStream fileOuputStream;
             try {
                 fileOuputStream = new FileOutputStream(directoria+File.separator+this.lista.get(0).getFileName());

                 for (MsgCliente dados : this.lista) {
                     if (dados.getFimDaEscrita() == true) {
                         if(dados.getNbytes() > 0){
                            fileOuputStream.write(dados.getbFile(), 0, dados.getNbytes());
                         }
                     }
                 }
                 fileOuputStream.close();
             } 
             catch (FileNotFoundException ex) {
                 System.err.println("Erro ao gravar ficheiro FileNotFoundException");
             } 
             catch (IOException ex) {
                 System.err.println("Erro ao escrver ficheiro");
             }
            
             System.out.println("Transferencia foi concluida "+this.lista.get(0).getFileName());
                         
             return;
         }
     }
    //############################################################
    //#                 CLOSE SOCKET TCP && UDP                  #
    //############################################################
    //fechar socket udp
    private void closeSocketUdpLogout(){
        try {
            System.gc();
        } 
        finally {
            if(this.socketUdpDirectoria !=null){
                this.socketUdpDirectoria.close();
                this.packet = null;
                this.condicaoParagem = false;
            } 
           System.gc();
        }
    }
    
    //fechar socket do servidor logout
    private void closeSocketUdp(){
        try {
            System.gc();
        } 
        finally {
            this.socketUdpDirectoria.close();
            this.packet = null;
            this.condicaoParagem = false;
            System.gc();
        }
    }
    
    //fechar conexao do TCP
    private void closeConexaoDoTcp(){
        if(this.socket != null){
            try{
                this.ativaStopLogin = true;
                this.socket.close();
                if(this.ThreadRecebDadosServidor != null){
                    this.ThreadRecebDadosServidor.setAtivaStopLogin(this.ativaStopLogin);
                    this.ThreadRecebDadosServidor.parar();
                    this.ThreadRecebDadosServidor = null;
                    this.login = new Login(" ", "" );
                }
            }
            catch(Exception ob){
                System.err.println("Erro ao fechar socket Tcp");
            }
        }
        ativaStopLogin = false;
        System.gc();//Limpar toda a memoria
    }
    
    //############################################################
    //#                     MyException                          #
    //############################################################
    //minha exception 
    class MyException extends IOException{
        String str1;
        MyException(String str2) {
            str1=str2;
        }
        public String toString(){ 
        return ("Output String = "+str1) ;
        }
    }  
    //########################## METODOS ###################################
}