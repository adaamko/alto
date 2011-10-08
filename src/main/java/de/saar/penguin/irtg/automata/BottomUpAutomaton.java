/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.automata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.CartesianIterator;
import de.saar.basic.Pair;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.saar.penguin.irtg.semiring.AndOrSemiring;
import de.saar.penguin.irtg.semiring.DoubleArithmeticSemiring;
import de.saar.penguin.irtg.semiring.LongArithmeticSemiring;
import de.saar.penguin.irtg.semiring.Semiring;
import de.saar.penguin.irtg.semiring.ViterbiWithBackpointerSemiring;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class BottomUpAutomaton<State> {
    protected Map<String, StateListToStateMap> explicitRules; // one for each label
    protected Map<String, SetMultimap<State, Rule<State>>> explicitRulesTopDown;
    protected Set<State> finalStates;
    protected Set<State> allStates;
    private final Map<String,State> dummyLtsSubstitution = new HashMap<String, State>();
    protected boolean isExplicit;
    protected SetMultimap<State, Rule<State>> rulesForRhsState;

    public BottomUpAutomaton() {
        explicitRules = new HashMap<String, StateListToStateMap>();
        explicitRulesTopDown = new HashMap<String, SetMultimap<State, Rule<State>>>();
        finalStates = new HashSet<State>();
        allStates = new HashSet<State>();
        isExplicit = false;
        rulesForRhsState = HashMultimap.create();
    }

    /**
     * Finds automaton rules bottom-up for a given list of child states
     * and a given parent label. The method returns a collection of
     * rules that can be used to assign a state to the parent node.
     * 
     * @param label
     * @param childStates
     * @return 
     */
    abstract public Set<Rule<State>> getRulesBottomUp(String label, List<State> childStates);

    /**
     * Finds automaton rules top-down for a given parent state and label.
     * The method returns a collection of rules that can be used to
     * assign states to the children.
     * 
     * @param label
     * @param parentState
     * @return 
     */
    abstract public Set<Rule<State>> getRulesTopDown(String label, State parentState);

    /**
     * Returns the arity of a terminal symbol used by this automaton.
     * 
     * @param label
     * @return 
     */
    abstract public int getArity(String label);

    /**
     * Returns all terminal symbols that this automaton knows about.
     * 
     * @return 
     */
    abstract public Set<String> getAllLabels();

    /**
     * Returns the final states of the automaton.
     * 
     * @return 
     */
    abstract public Set<State> getFinalStates();

    /**
     * Returns the set of all states of this automaton.
     * 
     * @return 
     */
    abstract public Set<State> getAllStates();

    /**
     * Caches a rule for future use. Once a rule has been cached,
     * it will be found by getRulesBottomUpFromExplicit and getRulesTopDownFromExplicit.
     * 
     * @param rule 
     */
    protected void storeRule(Rule<State> rule) {
        // store as bottom-up rule
        StateListToStateMap smap = getOrCreateStateMap(rule.getLabel());
        smap.put(rule);

        // store as top-down rule
        SetMultimap<State, Rule<State>> topdown = explicitRulesTopDown.get(rule.getLabel());
        if (topdown == null) {
            topdown = HashMultimap.create();
            explicitRulesTopDown.put(rule.getLabel(), topdown);
        }
        topdown.put(rule.getParent(), rule);

        // store pointer from rhs states to rule
        for (State rhs : rule.getChildren()) {
            rulesForRhsState.put(rhs, rule);
        }

        // collect states
        if (allStates != null) {
            allStates.add(rule.getParent());
            for (int i = 0; i < rule.getArity(); i++) {
                allStates.add(rule.getChildren()[i]);
            }
        }
    }

    /**
     * Like getRulesBottomUp, but only looks for rules in the
     * cache of previously discovered rules.
     * 
     * @param label
     * @param childStates
     * @return 
     */
    protected Set<Rule<State>> getRulesBottomUpFromExplicit(String label, List<State> childStates) {
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return new HashSet<Rule<State>>();
        } else {
            return smap.get(childStates);
        }
    }

    /**
     * Like getRulesTopDown, but only looks for rules in the
     * cache of previously discovered rules.
     * 
     * @param label
     * @param parentState
     * @return 
     */
    protected Set<Rule<State>> getRulesTopDownFromExplicit(String label, State parentState) {
        if (useCachedRuleTopDown(label, parentState)) {
            return explicitRulesTopDown.get(label).get(parentState);
        } else {
            return new HashSet<Rule<State>>();
        }
    }

    /**
     * Returns the set of all rules of this automaton. This method
     * is currently implemented rather inefficiently. Note that it
     * necessarily _computes_ the set of all rules, which may be expensive
     * for lazy automata.
     * 
     * @return 
     */
    public Set<Rule<State>> getRuleSet() {
        Set<Rule<State>> ret = new HashSet<Rule<State>>();

        makeAllRulesExplicit();

        for (StateListToStateMap map : explicitRules.values()) {
            for (Set<Rule<State>> set : map.getAllRules().values()) {
                ret.addAll(set);
            }
        }

        return ret;
    }

    /**
     * Returns the set of all rules, indexed by parent label and
     * children states.
     * 
     * @return 
     */
    public Map<String, Map<List<State>, Set<Rule<State>>>> getAllRules() {
        Map<String, Map<List<State>, Set<Rule<State>>>> ret = new HashMap<String, Map<List<State>, Set<Rule<State>>>>();

        makeAllRulesExplicit();

        for (String f : getAllLabels()) {
            ret.put(f, getAllRules(f));
        }

        return ret;
    }

    private Map<List<State>, Set<Rule<State>>> getAllRules(String label) {
        if (explicitRules.containsKey(label)) {
            return explicitRules.get(label).getAllRules();
        } else {
            return new HashMap<List<State>, Set<Rule<State>>>();
        }
    }

    /**
     * Returns the number of trees in the language of this
     * automaton. Note that this is faster than computing the
     * entire language. The method only works if the automaton
     * is acyclic, and only returns correct results if the
     * automaton is bottom-up deterministic.
     * 
     * @return 
     */
    public long countTrees() {
        Map<State, Long> map = evaluateInSemiring(new LongArithmeticSemiring(), new RuleEvaluator<State, Long>() {
            public Long evaluateRule(Rule<State> rule) {
                return 1L;
            }
        });

        long ret = 0L;
        for (State f : getFinalStates()) {
            ret += map.get(f);
        }
        return ret;
    }

    /**
     * Returns a map representing the inside probability of
     * each state.
     * 
     * @return 
     */
    public Map<State, Double> inside() {
        return evaluateInSemiring(new DoubleArithmeticSemiring(), new RuleEvaluator<State, Double>() {
            public Double evaluateRule(Rule<State> rule) {
                return rule.getWeight();
            }
        });
    }

    /**
     * Returns a map representing the outside probability of
     * each state.
     * 
     * @param inside a map representing the inside probability of each state.
     * @return 
     */
    public Map<State, Double> outside(final Map<State, Double> inside) {
        return evaluateInSemiringTopDown(new DoubleArithmeticSemiring(), new RuleEvaluatorTopDown<State, Double>() {
            public Double initialValue() {
                return 1.0;
            }

            public Double evaluateRule(Rule<State> rule, int i) {
                Double ret = rule.getWeight();
                for (int j = 0; j < rule.getArity(); j++) {
                    if (j != i) {
                        ret = ret * inside.get(rule.getChildren()[j]);
                    }
                }
                return ret;
            }
        });
    }

    /**
     * Computes the highest-weighted tree in the language of this
     * (weighted) automaton, using the Viterbi algorithm.
     * 
     * @return 
     */
    public Tree viterbi() {
        // run Viterbi algorithm bottom-up, saving rules as backpointers
        Map<State, Pair<Double, Rule<State>>> map =
                evaluateInSemiring(new ViterbiWithBackpointerSemiring<State>(), new RuleEvaluator<State, Pair<Double, Rule<State>>>() {
            public Pair<Double, Rule<State>> evaluateRule(Rule<State> rule) {
                return new Pair<Double, Rule<State>>(rule.getWeight(), rule);
            }
        });

        // find final state with highest weight
        State bestFinalState = null;
        double weightBestFinalState = Double.POSITIVE_INFINITY;
        for (State s : getFinalStates()) {
            if (map.get(s).left < weightBestFinalState) {
                bestFinalState = s;
                weightBestFinalState = map.get(s).left;
            }
        }

        // extract best tree from backpointers
        Tree ret = new Tree();
        extractTreeFromViterbi(ret, null, bestFinalState, map);
        return ret;
    }

    private void extractTreeFromViterbi(Tree tree, String parent, State state, Map<State, Pair<Double, Rule<State>>> map) {
        Rule<State> backpointer = map.get(state).right;
        String node = tree.addNode(backpointer.getLabel(), parent);
        for (State child : backpointer.getChildren()) {
            extractTreeFromViterbi(tree, node, child, map);
        }
    }

    /**
     * Computes the tree language accepted by this automaton.
     * This only works if the automaton is acyclic (in which case
     * the language is also finite). 
     * 
     * @return 
     */
    public List<Tree<String>> language() {
        /*
         * The current implementation is probably not particularly efficient.
         * It could be improved by using CartesianIterators for each rule.
         */
        Map<State, List<Tree<String>>> languagesForStates =
                evaluateInSemiring(new LanguageCollectingSemiring(), new RuleEvaluator<State, List<Tree<String>>>() {
            public List<Tree<String>> evaluateRule(Rule<State> rule) {
                List<Tree<String>> ret = new ArrayList<Tree<String>>();
                Tree<String> tree = new Tree<String>();
                tree.addNode(rule.getLabel(), null);
                ret.add(tree);
                return ret;
            }
        });

        List<Tree<String>> ret = new ArrayList<Tree<String>>();
        for (State finalState : getFinalStates()) {
            ret.addAll(languagesForStates.get(finalState));
        }
        return ret;
    }

    private static class LanguageCollectingSemiring implements Semiring<List<Tree<String>>> {
        public List<Tree<String>> add(List<Tree<String>> x, List<Tree<String>> y) {
            x.addAll(y);
            return x;
        }

        public List<Tree<String>> multiply(List<Tree<String>> partialTrees, List<Tree<String>> newSubtrees) {
            List<Tree<String>> ret = new ArrayList<Tree<String>>();
            for (Tree<String> partialTree : partialTrees) {
                for (Tree<String> newSubtree : newSubtrees) {
                    Tree<String> newTree = partialTree.copy();
                    newTree.insert(newSubtree, newTree.getRoot());
                    ret.add(newTree);
                }
            }

            return ret;
        }

        public List<Tree<String>> zero() {
            return new ArrayList<Tree<String>>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BottomUpAutomaton)) {
            return false;
        }

        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();
        Map<String, Map<List<State>, Set<Rule<State>>>> otherRules = ((BottomUpAutomaton) o).getAllRules();

        if (!rules.keySet().equals(otherRules.keySet())) {
//            System.err.println("not equals: labels " + rules.keySet() + " vs " +otherRules.keySet());
            return false;
        }

        for (String f : rules.keySet()) {
            if (!rules.get(f).keySet().equals(otherRules.get(f).keySet())) {
//                System.err.println("not equals: LHS for " + f + " is " + rules.get(f).keySet() + " vs " + otherRules.get(f).keySet() );
                return false;
            }

            for (List<State> states : rules.get(f).keySet()) {
                if (!new HashSet<Rule<State>>(rules.get(f).get(states)).equals(new HashSet<Rule<State>>(otherRules.get(f).get(states)))) {
//                    System.err.println("noteq: RHS for " + f + states + " is " + rules.get(f).get(states) + " vs " + otherRules.get(f).get(states));
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();

        for (String f : getAllLabels()) {
            for (List<State> children : rules.get(f).keySet()) {
                for (Rule rule : rules.get(f).get(children)) {
                    buf.append(rule.toString() + (getFinalStates().contains(rule.getParent()) ? "!" : "") + "\n");
                }
            }
        }

        return buf.toString();
    }

    /**
     * Computes all rules in this automaton and stores them in the cache.
     * This only makes a difference for lazy automata, in which rules
     * are only computed by need. After calling this function, it is
     * guaranteed that all rules are in the cache.
     */
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            Set<State> everAddedStates = new HashSet<State>();
            Queue<State> agenda = new LinkedList<State>();

            agenda.addAll(getFinalStates());
            everAddedStates.addAll(getFinalStates());

            while (!agenda.isEmpty()) {
                State state = agenda.remove();

                for (String label : getAllLabels()) {
                    Set<Rule<State>> rules = getRulesTopDown(label, state);
                    for (Rule<State> rule : rules) {
                        for (State child : rule.getChildren()) {
                            if (!everAddedStates.contains(child)) {
                                everAddedStates.add(child);
                                agenda.offer(child);
                            }
                        }
                    }
                }
            }

            isExplicit = true;
        }
    }

    /**
     * Checks whether the cache contains a bottom-up rule for
     * the given parent label and children states.
     * 
     * @param label
     * @param childStates
     * @return 
     */
    protected boolean useCachedRuleBottomUp(String label, List<State> childStates) {
        if( isExplicit ) {
            return true;
        }
            
        StateListToStateMap smap = explicitRules.get(label);

        if (smap == null) {
            return false;
        } else {
            return smap.contains(childStates);
        }
    }

    /**
     * Checks whether the cache contains a top-down rule for
     * the given parent label and state.
     * @param label
     * @param parent
     * @return 
     */
    protected boolean useCachedRuleTopDown(String label, State parent) {
        if( isExplicit ) {
            return true;
        }
        
        SetMultimap<State, Rule<State>> topdown = explicitRulesTopDown.get(label);
        if (topdown == null) {
            return false;
        } else {
            return topdown.containsKey(parent);
        }
    }

    /**
     * Intersects this automaton with another one.
     * 
     * @param <OtherState> the state type of the other automaton.
     * @param other the other automaton.
     * @return an automaton representing the intersected language.
     */
    public <OtherState> BottomUpAutomaton<Pair<State, OtherState>> intersect(BottomUpAutomaton<OtherState> other) {
        return new IntersectionAutomaton<State, OtherState>(this, other);
    }

    /**
     * Computes the pre-image of this automaton under a homomorphism.
     * 
     * @param hom the homomorphism.
     * @return an automaton representing the homomorphic pre-image.
     */
    public BottomUpAutomaton<State> inverseHomomorphism(Homomorphism hom) {
        return new InverseHomAutomaton<State>(this, hom);
    }

    /**
     * Determines whether the automaton accepts a given tree.
     * 
     * @param tree
     * @return 
     */
    public boolean accepts(final Tree tree) {
        Set<State> resultStates = run(tree);
        resultStates.retainAll(getFinalStates());
        return !resultStates.isEmpty();
    }

    /**
     * Runs the automaton bottom-up on a given tree and returns the set
     * of possible states for the root.
     * 
     * @param tree
     * @return 
     */
    public Set<State> run(final Tree tree) {
        return run(tree, dummyLtsSubstitution);
    }

    /**
     * Runs the automaton bottom-up on a given tree, assuming a certain
     * assignment of states to leaves. The LeafToStateSubstitution may
     * assign states to certain node names, which then override or replace
     * the state assignments the automaton would otherwise perform.
     * 
     * @param tree
     * @param subst
     * @return 
     */
    public Set<State> run(final Tree tree, final Map<String, State> subst) {
        final Set<State> ret = new HashSet<State>();

        tree.dfs(new TreeVisitor<Void, Set<State>>() {
            @Override
            public Set<State> combine(String node, List<Set<State>> childrenValues) {
                String f = tree.getLabel(node).toString();
                Set<State> states = new HashSet<State>();

                if (childrenValues.isEmpty()) {
                    if (subst.containsKey(node)) {
                        states.add(subst.get(node));
                    } else {
                        for (Rule<State> rule : getRulesBottomUp(f, new ArrayList<State>())) {
                            states.add(rule.getParent());
                        }
                    }
                } else {
                    CartesianIterator<State> it = new CartesianIterator<State>(childrenValues);

                    while (it.hasNext()) {
                        for (Rule<State> rule : getRulesBottomUp(f, it.next())) {
                            states.add(rule.getParent());
                        }
                    }
                }

                if (node.equals(tree.getRoot())) {
                    ret.addAll(states);
                }

                return states;
            }
        });

        return ret;
    }

    /**
     * Reduces the automaton. This means that all states and rules that
     * are not reachable bottom-up are removed.
     * 
     * @return 
     */
    public BottomUpAutomaton<State> reduceBottomUp() {
        Map<State, Boolean> productiveStates = evaluateInSemiring(new AndOrSemiring(), new RuleEvaluator<State, Boolean>() {
            public Boolean evaluateRule(Rule<State> rule) {
                return true;
            }
        });

        ConcreteBottomUpAutomaton<State> ret = new ConcreteBottomUpAutomaton<State>();

        // copy all rules that only contain productive states
        for (Rule<State> rule : getRuleSet()) {
            boolean allProductive = productiveStates.get(rule.getParent());

            for (State child : rule.getChildren()) {
                if (!productiveStates.get(child)) {
                    allProductive = false;
                }
            }

            if (allProductive) {
                ret.addRule(rule);
            }
        }

        // copy all productive final states
        for (State state : getFinalStates()) {
            if (productiveStates.get(state)) {
                ret.addFinalState(state);
            }
        }

        return ret;
    }

    /**
     * Evaluates all states of the automaton bottom-up
     * in a semiring. The evaluation of a state is the semiring sum
     * of semiring zero plus the evaluations of all rules in which it is the parent.
     * The evaluation of a rule is the semiring product of the evaluations
     * of its child states, times the evaluation of the rule itself.
     * The evaluation of a rule is determined by the RuleEvaluator argument.
     * This method only works if the automaton is acyclic, so states can be
     * processed in a well-defined bottom-up order.
     * 
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return a map assigning values in the semiring to all reachable states.
     */
    public <E> Map<State, E> evaluateInSemiring(Semiring<E> semiring, RuleEvaluator<State, E> evaluator) {
        Map<State, E> ret = new HashMap<State, E>();

        for (State s : getStatesInBottomUpOrder()) {
            E accu = semiring.zero();

            for (String label : getAllLabels()) {
                Set<Rule<State>> rules = getRulesTopDown(label, s);

                for (Rule<State> rule : rules) {
                    E valueThisRule = evaluator.evaluateRule(rule);
                    for (State child : rule.getChildren()) {
                        if (valueThisRule != null) {
                            if (ret.containsKey(child)) {
                                valueThisRule = semiring.multiply(valueThisRule, ret.get(child));
                            } else {
                                // if a child state hasn't been evaluated yet, this means that it
                                // is not reachable bottom-up, and therefore shouldn't be counted here
                                valueThisRule = null;
                            }
                        }
                    }

                    if (valueThisRule != null) {
                        accu = semiring.add(accu, valueThisRule);
                    }
                }
            }

            ret.put(s, accu);
        }

        return ret;
    }

    /**
     * Like evaluateInSemiring, but proceeds in top-down order.
     * 
     * @param <E>
     * @param semiring
     * @param evaluator
     * @return 
     */
    public <E> Map<State, E> evaluateInSemiringTopDown(Semiring<E> semiring, RuleEvaluatorTopDown<State, E> evaluator) {
        Map<State, E> ret = new HashMap<State, E>();
        List<State> statesInOrder = getStatesInBottomUpOrder();
        Collections.reverse(statesInOrder);

        for (State s : statesInOrder) {
            E accu = semiring.zero();

            if (rulesForRhsState.containsKey(s)) {
                for (Rule<State> rule : rulesForRhsState.get(s)) {
                    E parentValue = ret.get(rule.getParent());
                    for (int i = 0; i < rule.getArity(); i++) {
                        if (rule.getChildren()[i].equals(s)) {
                            accu = semiring.add(accu, semiring.multiply(parentValue, evaluator.evaluateRule(rule, i)));
                        }
                    }
                }
            } else {
                accu = evaluator.initialValue();
            }

            ret.put(s, accu);
        }

        return ret;
    }

    /**
     * Returns a topological ordering of the states, such that
     * later nodes always occur above earlier nodes in any run
     * of the automaton on a tree.
     * 
     * @return 
     */
    public List<State> getStatesInBottomUpOrder() {
        List<State> ret = new ArrayList<State>();
        SetMultimap<State, State> children = HashMultimap.create(); // children(q) = {q1,...,qn} means that q1,...,qn occur as child states of rules of which q is parent state
        Set<State> visited = new HashSet<State>();

        // traverse all rules to compute graph
        Map<String, Map<List<State>, Set<Rule<State>>>> rules = getAllRules();
        for (Map<List<State>, Set<Rule<State>>> rulesPerLabel : rules.values()) {
            for (List<State> lhs : rulesPerLabel.keySet()) {
                Set<Rule<State>> rhsStates = rulesPerLabel.get(lhs);

                for (Rule<State> rule : rhsStates) {
                    children.putAll(rule.getParent(), lhs);
                }
            }
        }

        // perform topological sort
        for (State q : getFinalStates()) {
            dfsForStatesInBottomUpOrder(q, children, visited, ret);
        }

        return ret;
    }

    private void dfsForStatesInBottomUpOrder(State q, SetMultimap<State, State> children, Set<State> visited, List<State> ret) {
        if (!visited.contains(q)) {
            visited.add(q);

            for (State parent : children.get(q)) {
                dfsForStatesInBottomUpOrder(parent, children, visited, ret);
            }

            ret.add(q);
        }
    }

    protected ListMultimap<State, Rule<State>> getRuleByChildStateMap() {
        ListMultimap<State, Rule<State>> ret = ArrayListMultimap.create();

        for (Rule<State> rule : getRuleSet()) {
            for (State child : rule.getChildren()) {
                ret.put(child, rule);
            }
        }

        return ret;
    }

    private StateListToStateMap getOrCreateStateMap(String label) {
        StateListToStateMap ret = explicitRules.get(label);

        if (ret == null) {
            ret = new StateListToStateMap(label);
            explicitRules.put(label, ret);
        }

        return ret;
    }

    protected class StateListToStateMap {
        private Map<State, StateListToStateMap> nextStep;
        private Set<Rule<State>> rulesHere;
        private int arity;
        private String label;

        public StateListToStateMap(String label) {
            rulesHere = new HashSet<Rule<State>>();
            nextStep = new HashMap<State, StateListToStateMap>();
            arity = -1;
            this.label = label;
        }

        public void put(Rule<State> rule) {
            put(rule, 0, 1);

            if (arity != -1) {
                if (arity != rule.getChildren().length) {
                    throw new UnsupportedOperationException("Storing state lists of different length: " + rule + ", should be " + arity);
                }
            } else {
                arity = rule.getChildren().length;
            }
        }

        private void put(Rule<State> rule, int index, double weight) {
            if (index == rule.getArity()) {
//                Rule<State> rule = new Rule<State>(state, label, stateList, weight);
                rulesHere.add(rule);
            } else {
                State nextState = rule.getChildren()[index];
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    sub = new StateListToStateMap(label);
                    nextStep.put(nextState, sub);
                }

                sub.put(rule, index + 1, weight);
            }
        }

        public Set<Rule<State>> get(List<State> stateList) {
            return get(stateList, 0);
        }

        private Set<Rule<State>> get(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return rulesHere;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return new HashSet<Rule<State>>();
                } else {
                    return sub.get(stateList, index + 1);
                }
            }
        }

        public boolean contains(List<State> stateList) {
            return contains(stateList, 0);
        }

        private boolean contains(List<State> stateList, int index) {
            if (index == stateList.size()) {
                return true;
            } else {
                State nextState = stateList.get(index);
                StateListToStateMap sub = nextStep.get(nextState);

                if (sub == null) {
                    return false;
                } else {
                    return sub.contains(stateList, index + 1);
                }
            }
        }

        public int getArity() {
            return arity;
        }

        public Map<List<State>, Set<Rule<State>>> getAllRules() {
            Map<List<State>, Set<Rule<State>>> ret = new HashMap<List<State>, Set<Rule<State>>>();
            List<State> currentStateList = new ArrayList<State>();
            retrieveAll(currentStateList, 0, getArity(), ret);
            return ret;
        }

        private void retrieveAll(List<State> currentStateList, int index, int arity, Map<List<State>, Set<Rule<State>>> ret) {
            if (index == arity) {
                ret.put(new ArrayList<State>(currentStateList), rulesHere);
            } else {
                for (State state : nextStep.keySet()) {
                    currentStateList.add(state);
                    nextStep.get(state).retrieveAll(currentStateList, index + 1, arity, ret);
                    currentStateList.remove(index);
                }
            }
        }

        @Override
        public String toString() {
            return getAllRules().toString();
        }
    }
}
