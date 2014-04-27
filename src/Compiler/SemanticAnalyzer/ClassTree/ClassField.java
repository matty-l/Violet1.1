package Compiler.SemanticAnalyzer.ClassTree;

/**
 * This class holds field informatin. This class is immutable.
 * Created by Matt Levine on 4/26/14.
 */
public class ClassField {
    private final String name;
    private final String type;

    public ClassField(final String name, final String type){
        this.name = name;
        this.type = type;
    }

    /** Returns true if the name and type match
     * @param o the object to compare
     * @return true if equivalent
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassField oField = (ClassField) o;

        return name.equals(oField.name) && type.equals(oField.type);

    }

    /** Returns a hash by name and type
     * @return hashcode
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    /** Returns the name of the field
     * @return the name of the field
     */
    public String getName() { return name;  }
}
