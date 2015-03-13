/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.bolinas_hrg;

import java.util.ArrayList;
import java.util.List;

/**
 * A Bolinas-style hyperedge replacement grammar.
 * 
 * @author koller
 */
class BolinasHrgGrammar {
    
    private String startSymbol;

    private final List<Rule> rules;

    public BolinasHrgGrammar() {
        rules = new ArrayList<>();
    }

    public List<Rule> getRules() {
        return rules;
    }
    
    void addRule(Rule rule) {
        rules.add(rule);
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    @Override
    public String toString() {
    
        StringBuilder buf = new StringBuilder();
        
        for( Rule rule : rules ) {
            buf.append(rule.toString()).append("\n");
        }
        
        return buf.toString();
    }
}
