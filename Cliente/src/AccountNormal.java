
import java.io.Serializable;

public class AccountNormal  extends Estado implements Serializable{
    static final long serialVersionUID = 1L;
    
    @Override
    public Estado mudaEstado(int opcao){
        if(opcao == 1){
            return new CreateNewAccount();
        }
        else if(opcao == 2){
            return new LoginAccount();
        }
        else{
            return new AccountNormal();
        }
    }
    
    @Override
    public Estado getEstado(){
        return this;
    }

}