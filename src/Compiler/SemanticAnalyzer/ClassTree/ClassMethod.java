package Compiler.SemanticAnalyzer.ClassTree;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class holds method information. This class is immutable.
 * Created by Matt Levine on 4/26/14.
 */
public class ClassMethod {

    private final String /*Could make class or class method*/ returnType;
    private final ArrayList<ClassFormal> parameters = new ArrayList<>();
    private final String name;


    public ClassMethod(final String name, final String returnType,
                       final String[]... formals){
        this.name = name;
        this.returnType = returnType;
        if (formals == null)
            throw new ClassMethodConstructionException("Formal list cannot be null");
        for (String[] formal : formals){
            if (formal.length != 2)
                throw new ClassMethodConstructionException("Invalid formal parameter");
            this.parameters.add(new ClassFormal(formal[0],formal[1]));
        }
    }

    /** Checks equivalence by name, formals, and return type. This does not
     * allow for overloading (fixme).
     * @param o the object to check if equivalent
     * @return true if equivalent
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassMethod oMethod = (ClassMethod) o;

        return name.equals(oMethod.name) && parameters.size() == (oMethod.parameters.size());

    }

    /** Hashes by name, formals and return type
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result = parameters.size();
        result = 31 * result + name.hashCode();
        return result;
    }


    /** Returns a useful string representation of the method
     * @return string representation
     */
    @Override public String toString(){
        StringBuilder params = new StringBuilder("[");
        for (ClassFormal param : parameters){
            params.append("(").append(param.getType()).append(", ");
            params.append(param.getName()).append(")");
        }
        params.append("]");
        return "{"+name+" | "+returnType + " | " + params + "}";
    }

    /** Returns the name of the method
     * @return name of method
     */
    public String getName(){return name;}

}


class ClassMethodConstructionException extends RuntimeException {
    public ClassMethodConstructionException(String s) {
        super("ClassMethod Constructor Err: "+s);
    }
}
