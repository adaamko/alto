/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.Iterators;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.OutputCodec;
import de.up.ling.irtg.laboratory.OperationAnnotation;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.util.Evaluator;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * An algebra over some domain E of values. An algebra can
 * <i>evaluate</i> a term t over the algebra's signature to an object in E. If
 * the algebra is furthermore <i>regularly decomposable</i> -- i.e., we can
 * compute a tree automaton for the terms denoting some object in E --, the
 * algebra can be used as the input algebra in IRTG parsing.<p>
 *
 * A concrete implementation of this abstract base class must provide two
 * methods, {@link #evaluate(java.lang.String, java.util.List) }
 * and {@link #parseString(java.lang.String) }. The evaluate method interprets
 * its first argument (a string) as a function symbol of the algebra, and
 * applies it to the given arguments. The parseString method translates a string
 * representation of a value of this algebra into the value itself. The Algebra
 * class then implements a default implementation of the method
 * {@link #evaluate(de.up.ling.tree.Tree) }, which evaluates an entire term of
 * the algebra. You may override the evaluate-term method with a specialized
 * implementation if this is more efficient.<p>
 *
 * The Algebra class also provides a default implementation of decomposition
 * automata for this class. Decomposition automata are needed for parsing. The
 * default implementation uses values of the algebra as its states, and only
 * provides bottom-up rules which simply evaluate the terminal symbol on the
 * child "states". This default implementation will allow you to get started
 * with parsing quickly, but if you want to achieve reasonably parsing
 * efficiency, you will almost certainly want to implement your own optimized
 * decompose method eventually.
 *
 * @author koller
 */
public abstract class Algebra<E> implements Serializable {

    protected Signature signature;

    public Algebra() {
        signature = new Signature();
    }

    /**
     * Applies the operation with name "label" to the given arguments, and
     * returns the result.
     *
     * @param label
     * @param childrenValues
     * @return
     */
    protected abstract E evaluate(String label, List<E> childrenValues);

    /**
     * Checks whether "value" is a valid value. The decomposition automata will
     * only contain rules in which the parent and all child states are valid
     * values, as defined by this method. The default implementation of this
     * method always returns true. You may override it to make the decomposition
     * automata smaller (e.g. by rejecting null values).
     *
     * @param value
     * @return
     */
    protected boolean isValidValue(E value) {
        return true;
    }

    /**
     * Evaluates a term over the algebra's signature into an algebra object.
     *
     * @param t a term (= tree whose nodes are labeled with algebra operation
     * symbols)
     * @return
     */
    @OperationAnnotation(code = "eval")
    public E evaluate(Tree<String> t) {
        return (E) t.dfs(new TreeVisitor<String, Void, E>() {
            @Override
            public E combine(Tree<String> node, List<E> childrenValues) {
                return evaluate(node.getLabel(), childrenValues);
            }
        });
    }

    /**
     * Returns the signature of this algebra.
     *
     * @return
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * Computes a decomposition automaton for the given value. A decomposition
     * automaton is a finite tree automaton which accepts exactly those terms
     * over the algebra's signature which evaluate to the given value.
     *
     * @param value
     * @return
     */
    @OperationAnnotation(code = "decomp")
    public TreeAutomaton decompose(E value) {
        return new EvaluatingDecompositionAutomaton(value);
    }

    /**
     * Resolves the string representation of some element of the algebra's
     * domain to this element. For instance, the method {@link TreeAlgebra#parseString(java.lang.String)
     * }
     * resolves the string "f(a,b)" into a tree with three nodes.<p>
     *
     * It is the job of an algebra class to keep track of the signature of the
     * algebra. Many algebras have a potentially infinite domain (e.g. the
     * string algebra can be used with arbitrary alphabets), so the algebra
     * class should keep track of the symbols that were actually used in the
     * current run of the program. The best practice is to update the signature
     * each time the parseString method is called. The rest of the IRTG tool
     * code takes care to call parseString of the respective algebra to obtain
     * objects of type E, so this ensures that the signature is always
     * up-to-date.
     *
     * @param representation
     * @return
     * @throws ParserException
     */
    abstract public E parseString(String representation) throws ParserException;

    
    /**
     * Returns all implementations this algebra provides for decomposition.
     * By default, his is the singleton list containing the decompose(E) function with key "Standard".
     * Returns a list of implementations. Each implementation is a pair of pairs.
     * The left pair has the name of the implementation on its left, this is shown
     * to the user and has no internal consequences. The right String of the left
     * pair is the code of the implementation, changing it removes backward
     * compatibility when recovering this implementation from a string.
     * The right pair has the actual implementing function on its left, and the return
     * type on its right.
     * @param interpretationName
     * @return 
     */
    public List<Pair<Pair<String,String>,  Pair<Function<E, Object>, Class>>> getDecompositionImplementations(String interpretationName) {
        List<Pair<Pair<String,String>,  Pair<Function<E, Object>, Class>>> ret = new ArrayList<>();
        try {
            Function<E, Object> function = value -> decompose(value);
            Pair<String, String> nameAndCode = new Pair("Standard", "std");
            //getting the return type of the actual decompose function is a bit complicated... edit: did not make it work, just using TreeAutomaton for now.
            ret.add(new Pair(nameAndCode, new Pair(function, TreeAutomaton.class)));
        } catch (java.lang.Exception e) {
            System.err.println("Could not collect decomposition implementations for interpretation " + interpretationName +": "+e.toString());
        }
        return ret;
    }
    
    /**
     * Returns all evaluation methods for this algebra. Default is an empty map.
     * @return Maps the name of the evaluation method to a function that takes
     * a pair of objects (result left, gold right) to a pair of doubles: left
     * is the score, and right is the weight (if i.e. in a corpus we want to 
     * compute the weighted average).
     */
    public List<Evaluator> getEvaluationMethods() {
        return new ArrayList<>();
    }
    
    /**
     * Returns all numerical properties this algebra provides for its objects.
     * Default is the length if the object is represented as a string.
     * @return Maps a pair of name and code of the property to a function that maps an object to the
     * property.
     */
    public List<Pair<Pair<String, String>, Function<E, Double>>> getObjectProperties() {
        List<Pair<Pair<String, String>, Function<E, Double>>> ret = new ArrayList<>();
        ret.add(new Pair(new Pair("String length", "string_length"), (Function<E, Double>) (E t) -> (double) t.toString().length()));
        return ret;
    }
    
    /**
     * Sets the options of the algebra implementation. Most algebras do not have
     * options; for these algebras, it is okay to reuse the default
     * implementation of readOptions, which simply does nothing. However, if
     * your algebra relies on external data to work properly, you may provide a
     * reader that provides a string representation of this external data using
     * this method. See {@link SetAlgebra} for an example.
     *
     * @param optionReader
     */
    public void readOptions(Reader optionReader) throws Exception {

    }

    /**
     * Sets the options of the algebra implementation from a string. This method
     * simply wraps the option string into a StringReader and then calls {@link #readOptions(java.io.Reader)
     * }.
     *
     * @see #readOptions(java.io.Reader)
     * @param string
     * @throws Exception
     */
    public void setOptions(String string) throws Exception {
        readOptions(new StringReader(string));
    }

    /**
     * Writes the options of the current algebra object to a Writer.
     *
     * @see #readOptions(java.io.Reader)
     * @param optionWriter
     * @throws Exception
     */
    public void writeOptions(Writer optionWriter) throws Exception {

    }

    /**
     * Returns true if the algebra implementation has options that would make
     * sense to be set using {@link #setOptions(java.lang.String) }.
     *
     * @return
     */
    public boolean hasOptions() {
        return false;
    }

    /**
     * Returns a Swing component that visualizes an object of this algebra. The
     * default implementation simply returns a JLabel containing a string
     * representation of the algebra object. Override this method to provide
     * more human-readable graphical presentations.
     *
     * @param object
     * @return
     */
    public JComponent visualize(E object) {
        return new JLabel(representAsString(object));
    }

    /**
     * Returns a string representation of this object. The default
     * implementation simply calls the object's {@link Object#toString() }
     * method. Concrete algebras may overwrite this implementation for
     * algebra-specific string representations. Whenever Alto knows to which
     * algebra an object belongs, it will attempt to call this method instead of
     * the generic toString to produce algebra-specific string representations.
     *
     * @param object
     * @return
     */
    public String representAsString(E object) {
        return object.toString();
    }
    
    /**
     * Returns the class of the elements of this algebra.
     * The default interpretation simply returns {@link Object};
     * you may override this with the actual class of the objects
     * of your algebra.<p>
     * 
     * This method is used in some places throughout Alto to
     * figure out what {@link OutputCodec}s are appropriate
     * for encoding objects of the algebra. By overriding the
     * method to return more specific classes than Object, you
     * make more output codecs available in those places.
     * 
     * @return 
     */
    public Class getClassOfValues() {
        return Object.class;
    }

    /**
     * Returns an iterator over all subclasses of Algebra.
     *
     * @return
     */
    public static Iterator<Class> getAllAlgebraClasses() {
        ServiceLoader<Algebra> algebraLoader = ServiceLoader.load(Algebra.class);
        return Iterators.transform(algebraLoader.iterator(), x -> x.getClass());
    }

    protected class EvaluatingDecompositionAutomaton extends TreeAutomaton<E> {

        public EvaluatingDecompositionAutomaton(E finalElement) {
            super(Algebra.this.getSignature());
            int x = addState(finalElement);
            finalStates.add(x);
        }

        @Override
        public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
            if (useCachedRuleBottomUp(labelId, childStates)) {
                return getRulesBottomUpFromExplicit(labelId, childStates);
            } else {
                Set<Rule> ret = new HashSet<Rule>();

                if (signature.getArity(labelId) == childStates.length) {

                    List<E> childValues = new ArrayList<E>();
                    for (int childState : childStates) {
                        childValues.add(getStateForId(childState));
                    }

                    String label = getSignature().resolveSymbolId(labelId);

                    if (label == null) {
                        throw new RuntimeException("Cannot resolve label ID: " + labelId);
                    }

                    E parents = evaluate(label, childValues);

                    // require that set in parent state must be non-empty; otherwise there is simply no rule
                    if (parents != null && isValidValue(parents)) {
                        int parentStateId = addState(parents);

                        Rule rule = createRule(parentStateId, labelId, childStates, 1);
                        ret.add(rule);
                        storeRuleBottomUp(rule);
                    }
                }

                return ret;
            }
        }

        @Override
        public Set<Rule> getRulesTopDown(int label, int parentState) {
            throw new UnsupportedOperationException("Decomposition automata of evaluating algebras do not support top-down queries.");
        }

        @Override
        public boolean supportsTopDownQueries() {
            return false;
        }

        @Override
        public boolean supportsBottomUpQueries() {
            return true;
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return true;
        }
    }
}
