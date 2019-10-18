
import java.io.Serializable;
import java.net.InetAddress;

public class DadosDoServidor implements Serializable, Cloneable {
    //Atributos
    static final long serialVersionUID = 1010L;
    private InetAddress ip;
    private int ServicePort;
    private int contador = 0;
    private double time = 0.0f;
    private Boolean warning = false;
    private int numeroClientes = 0;
    private int tipoHearBeat = 1;
    private Boolean estadoDeArranque = false;
    private Boolean emConfiguracao = true;

   
    //Construtor por defeito
    public DadosDoServidor() {

    }

    //Construtor por paramentros
    public DadosDoServidor(InetAddress ip, int porto) {
        this.ip = ip;
        this.ServicePort = porto;
    }

    //####################################################
    //#                  METODOS                         #
    //####################################################

    public Boolean getEstadoDeArranque() {
        return estadoDeArranque;
    }

    public void setEstadoDeArranque(Boolean estadoDeArranque) {
        this.estadoDeArranque = estadoDeArranque;
    }
    
    //Quando o servidor esta a reconfigurar
    public void setEmConfiguracao(Boolean emConfiguracao) {
        this.emConfiguracao = emConfiguracao;
    }

    //get estado da reconfiguracao
    public Boolean getEmConfiguracao() {
        return emConfiguracao;
    }
    
    
    
    // Total de clientes
    public int getNumeroClientes() {
        return numeroClientes;
    }
    
    //Inserir numero de clientes
    public void setNumeroClientes(int numeroClientes) {
        this.numeroClientes = numeroClientes;
    }

    //get ip do servidor
    public InetAddress getIp() {
        return ip;
    }

    //get porto
    public int getServicePort() {
        return ServicePort;
    }

    //set ip
    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    //set porto
    public void setServicePort(int ServicePort) {
        this.ServicePort = ServicePort;
    }

    //clonar obejcto
    @Override
    protected DadosDoServidor clone() throws CloneNotSupportedException {
        return (DadosDoServidor) super.clone();
    }

    //get warning
    public Boolean getWarning() {
        return warning;
    }
    
    //set warning
    public void setWarning(Boolean warnning) {
        this.warning = warnning;
    }

    //inserir tempo
    public void setTime(double time) {
        this.time = time;
    }

    //get tempo
    public double getTime() {
        return time;
    }

    //get Periodos
    public void setContador(int contador) {
        this.contador = contador;
    }

    //conta periodos
    public int getContador() {
        return contador;
    }

    //get hearbeabts
    public int getTipoHearBeat() {
        return tipoHearBeat;
    }

    //set o tipo de hearts
    public void setTipoHearBeat(int tipoHearBeat) {
        this.tipoHearBeat = tipoHearBeat;
    }
}
