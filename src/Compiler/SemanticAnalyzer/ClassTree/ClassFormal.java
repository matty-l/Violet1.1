package Compiler.SemanticAnalyzer.ClassTree;

/**
 * This class represents a formal - it is immutable.
 * Created by Matt Levine on 4/26/14.
 */
public class ClassFormal {

    private final String type;
    private final String name;

    public ClassFormal(final String name, final String type){
        this.type = type;
        this.name = name;
    }

    public String getType(){return type;}

    public String getName(){return name;}

    /** Validates equality by type and name
     * @param o the other formal
     * @return true if equivalent
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassFormal oFormal = (ClassFormal) o;

        return name.equals(oFormal.name) && type.equals(oFormal.type);

    }

    /** Hashes the formal by type and name
     * @return hash
     */
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
