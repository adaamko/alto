/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.irtg.signature.SignatureMapper;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 * @param <State>
 */
public class ConcreteTreeAutomaton<State> extends TreeAutomaton<State> {
    public ConcreteTreeAutomaton() {
        this(new Signature());
    }

    public ConcreteTreeAutomaton(Signature signature) {
        super(signature);
        ruleStore.setExplicit(true);
    }

    @Override
    public int addState(State state) {
        return super.addState(state);
    }

    @Override
    public void addFinalState(int state) {
        super.addFinalState(state);
    }
    

    public void addRule(Rule rule) {
        storeRuleBottomUp(rule);
        storeRuleTopDown(rule);
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int label, int[] childStates) {
        return getRulesBottomUpFromExplicit(label, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int label, int parentState) {
        return getRulesTopDownFromExplicit(label, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return ruleStore.isBottomUpDeterministic();
    }

    @Override
    public void foreachRuleBottomUpForSets(final IntSet labelIds, List<IntSet> childStateSets, final SignatureMapper signatureMapper, final Consumer<Rule> fn) {
        ruleStore.foreachRuleBottomUpForSets(labelIds, childStateSets, signatureMapper, fn);
    }
}