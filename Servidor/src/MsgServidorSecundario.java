
import java.io.Serializable;

public class MsgServidorSecundario implements Serializable,Cloneable{
    //Atributos
    static final long serialVersionUID = 1010L;
    private String comando;
    private  byte[] bFile;
    private String fileName;
    private Boolean fimDaEscrita = true;
    private int nbytes = 0;

  
    
    //Construtor por defeito
    public MsgServidorSecundario() {
    
    }    
    
    //clonar obejcto
    @Override
    protected MsgServidorSecundario clone() throws CloneNotSupportedException {
        return (MsgServidorSecundario) super.clone();
    }
    
    //################## METODOS #############################
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
      
    //set comando
    public void setComando(String  cmd){
        this.comando = cmd;
    }

    //set nome do ficheiro
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    
    // get nome do ficheiro
    public String getFileName() {
        return fileName;
    }
    
    //get comando
    public String getComando(){
        return this.comando;
    }
    
    //set Tamanho do ficheiro
      public void setbFile(byte[] bFile) {
        this.bFile = bFile;
    }
      
      //get bytes do ficheiro 
      public byte[] getbFile() {
        return bFile;
     }
    //################## METODOS #############################
}
