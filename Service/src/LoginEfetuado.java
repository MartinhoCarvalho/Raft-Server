import java.io.Serializable;

public class LoginEfetuado extends Estado implements Serializable{
    static final long serialVersionUID = 1L;
    
    @Override
    public Estado mudaEstado(int opcao){
       return this;
    }
    
    @Override
    public Estado getEstado(){
        return this;
    }

}
