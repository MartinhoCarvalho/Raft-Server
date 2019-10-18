
import java.io.Serializable;

abstract class Estado implements Serializable {
    static final long serialVersionUID = 1010L;
    //METODOS
    public abstract Estado mudaEstado(int opcao);
    public abstract Estado getEstado();
   
}
