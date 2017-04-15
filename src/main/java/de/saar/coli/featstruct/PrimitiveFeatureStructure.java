/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class PrimitiveFeatureStructure<E> extends FeatureStructure {
    private E value;

    public PrimitiveFeatureStructure(E value) {
        this.value = value;
    }

    private PrimitiveFeatureStructure<E> pdereference() {
        return (PrimitiveFeatureStructure<E>) dereference();
    }

    @Override
    protected E getValueD() {
        return value;
    }

    public void setValue(E value) {
        pdereference().value = value;
    }

    @Override
    protected void appendValue(Set<FeatureStructure> visitedIndexedFs, StringBuilder buf) {
        buf.append(value.toString());
    }

    @Override
    protected FeatureStructure destructiveUnifyLocalD(FeatureStructure d2) {
        // unification with non-AVM FSs
        if (d2 instanceof PlaceholderFeatureStructure) {
            d2.forward = this;
            return this;
        } else if (d2 instanceof AvmFeatureStructure) {
            return null;
        }

        // check that value is the same
        PrimitiveFeatureStructure pd2 = (PrimitiveFeatureStructure) d2;
        if (value.equals(pd2.value)) {
            forward = d2;
            return d2;
        } else {
            return null;
        }
    }

    private static final List<String> EMPTY_PATH = new ArrayList<>();

    @Override
    protected List<List<String>> getAllPathsD() {
        return Arrays.asList(EMPTY_PATH);
    }

    @Override
    protected FeatureStructure getD(List<String> path, int pos) {
        if (pos == path.size()) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    protected boolean localEqualsD(FeatureStructure other) {
        FeatureStructure d = other.dereference();

        if (other instanceof PrimitiveFeatureStructure) {
            PrimitiveFeatureStructure pOther = (PrimitiveFeatureStructure) other;
            return value.equals(pOther.value);
        } else {
            return false;
        }
    }
}
