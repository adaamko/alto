/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.pruning;

import de.up.ling.irtg.automata.Rule;

/**
 *
 * @author koller
 */
class RulePair implements Comparable<RulePair> {
    public Rule left;
    public Rule right;
    public double value;

    public RulePair(Rule left, Rule right, double value) {
        this.left = left;
        this.right = right;
        this.value = value;
    }

    @Override
    public int compareTo(RulePair o) {
        return Double.compare(value, o.value);
    }
    
}
