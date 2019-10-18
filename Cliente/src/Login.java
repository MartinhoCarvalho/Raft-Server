
import java.io.Serializable;
import java.net.InetAddress;
import java.util.logging.Logger;

public class Login implements Serializable {
    //Atributos
    static final long serialVersionUID = 1010L;
    protected String username;
    protected String password;
    private Estado estado;
    private int opcao;
    private boolean loginRealizadoComSucesso = false;
    private InetAddress ip;
    private int portoServidor;
    private  boolean estadoDoServidor = false;
    private Boolean naoExistemServidores = false;
    private Boolean clienteJaEstaLogado = false;
    private String logout = " ";
    private Boolean NaoExisteNenhumServidorAtivo = false;

    //Construtor
    public Login(String username, String password) {
        this.username = username;
        this.password = password;
        this.estado = new AccountNormal();
    }
    
    //############################################################
    //#                        METODOS                           #
    //############################################################
    //Endica se existe algum servidor ativo
    public Boolean getNaoExisteNenhumServidorAtivo() {
        return NaoExisteNenhumServidorAtivo;
    }
    //indicar se existe algum servidor
    public void setNaoExisteNenhumServidorAtivo(Boolean NaoExisteNenhumServidorAtivo) {
        this.NaoExisteNenhumServidorAtivo = NaoExisteNenhumServidorAtivo;
    }
    
    public String getLogout() {
        return logout;
    }

    public void setLogout(String logout) {
        this.logout = logout;
    }
    
    public void setClienteJaEstaLogado(Boolean clienteJaEstaLogado) {
        this.clienteJaEstaLogado = clienteJaEstaLogado;
    }

    public Boolean getClienteJaEstaLogado() {
        return clienteJaEstaLogado;
    }
    
    public Boolean getNaoExistemServidores() {
        return naoExistemServidores;
    }

    public void setNaoExistemServidores(Boolean naoExistemServidores) {
        this.naoExistemServidores = naoExistemServidores;
    }
    
    public boolean isLoginRealizadoComSucesso() {
        return loginRealizadoComSucesso;
    }
    
    public String getPassword() {
        return password;
    }

    public void setLoginRealizadoComSucesso(boolean loginRealizadoComSucesso) {
        this.loginRealizadoComSucesso = loginRealizadoComSucesso;
    }
    
    public String getUsername() {
        return username;
    }
    
    public Estado getEstado() {
        return this.estado;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public int getOpcao() {
        return opcao;
    }

    public int getPortoServidor() {
        return portoServidor;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public void setPortoServidor(int portoServidor) {
        this.portoServidor = portoServidor;
    }

    public InetAddress getIp() {
        return ip;
    }
    
    public void setOpcao(int opcao) {
        this.opcao = opcao;
    }
    
    public void mudaEstado(){
         this.estado = this.estado.mudaEstado(opcao);
    }
    
    public void loginEfetuado(){
        this.estado = new LoginEfetuado();
    }

    //Verificar se o servidor ja esta a correr get
    public boolean isEstadoDoServidor() {
        return estadoDoServidor;
    }

    //verificar se o servidor ja esta a correr set
    public void setEstadoDoServidor(boolean estadoDoServidor) {
        this.estadoDoServidor = estadoDoServidor;
    } 
}
