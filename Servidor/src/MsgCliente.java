
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MsgCliente implements Serializable, Cloneable {
    //Atributos
    static final long serialVersionUID = 1010L;
    private boolean estadoPedido = false;
    private String comando = " ";
    private List<DadosDosFicheiros> lista = null;
    private  byte[] bFile;
    private String fileName;
    private Boolean fimDaEscrita = true;
    private int nbytes;
    
    //Construtor por defeito
    public MsgCliente(){ 
        this.lista = new ArrayList<>();
    }
    
    //clonar obejcto
    @Override
    protected MsgCliente clone() throws CloneNotSupportedException {
        return (MsgCliente) super.clone();
    }
    
    
    //################## METODOS #############################
    //set estado do servidor
    public void setEstadoPedido(boolean estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    //inseriri o nome do download
    public void setDownload(String nome) {
        this.fileName = nome;
    }
    
    //nome do ficheiro a copiar 
    public String getNomeDoFicheiroACopiar(){
        return this.fileName;
    }

    //get estado do pedido
    public boolean isEstadoPedido() {
        return estadoPedido;
    }
   
    //set comando do cliente
    public void setComando(String comando) {
        this.comando = comando;
    }
   
    public String getComando() {
        return comando;
    }

    //inserir files
    public void setLista(String nome, long size) {
        DadosDosFicheiros ob = new DadosDosFicheiros(nome, size);
        this.lista.add(ob);
    }

    //lista dos files
    public List<DadosDosFicheiros> getLista() {
        return lista;
    }
    
    //Listar ficheiros
    public void listarFiles() {

        if (this.lista.size() <= 0) {
            System.out.println("Nao existem ficheiros no servidor");
        } 
        else {
            System.out.println("NOME FICHEIRO \t: \tTAMANHO    ");
            for (DadosDosFicheiros d : this.lista) {
                System.out.println(d.getNome() + "| " + d.getSize() + " KB");
            }
            System.out.println("\n\n");
        }
    }
    
    //Apagar lista de ficheiros
    public void clearLista(){
        this.lista.clear();
    }
    
    
    public int getNbytes() {
        return nbytes;
    }
    
    public void setNbytes(int nbytes) {
        this.nbytes = nbytes;
    }
    
    public void setFimDaEscrita(Boolean fimDaEscrita) {
        this.fimDaEscrita = fimDaEscrita;
    }
    
    public Boolean getFimDaEscrita() {
        return fimDaEscrita;
    }
    
    //set nome do ficheiro
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    
    // get nome do ficheiro
    public String getFileName() {
        return fileName;
    }
    
    //set Tamanho do ficheiro
    public void setbFile(byte[] bFile) {
        this.bFile = bFile;
    }
      
      //get bytes do ficheiro 
      public byte[] getbFile() {
        return bFile;
     }
    
    //total dos ficheiros
    public int getTotalFicheiros(){
        return this.lista.size();
    }
      
   
    //class Dados dos ficheiros
    class DadosDosFicheiros implements Serializable{
        //Atributos
        static final long serialVersionUID = 1010L;
        private String nome;
        private long size; 

        //Construtor
        public DadosDosFicheiros(String nome, long size) {
            this.nome = nome;
            this.size = size;
        }
    
        //################# METODOS ###############
        public String getNome() {
            return nome;
        }

        public long getSize() {
            return size;
        }
        //################# METODOS ###############
    }
    //################## METODOS #############################
}
