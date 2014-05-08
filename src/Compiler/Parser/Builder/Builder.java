package Compiler.Parser.Builder;

import Compiler.Parser.Matcher.*;
import Compiler.Parser.Matcher.Chart;
import Compiler.Parser.ParserTree.ParserTreeNode;
import javafx.scene.chart.*;

/**
 * The interface that must be implemented by any classes wanting to use information
 * from a Parser.CFG Parser.Matcher.
 * Created by Matt Levine on 3/13/14.
 */
public interface Builder {
    public Object build(State inputState);
    public Object build(Chart chart);
    public ParserTreeNode getTreeHead();

    class BuildMethodNotImplemented extends RuntimeException{
        public BuildMethodNotImplemented(){
            super("Building on this input type is not supported for this Builder");
        }
    }
}

