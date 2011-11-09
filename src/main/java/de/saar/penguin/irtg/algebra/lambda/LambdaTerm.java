/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra.lambda;

import de.saar.basic.Pair;
import de.saar.basic.tree.Tree;
import de.saar.basic.tree.TreeVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author koller
 */
public class LambdaTerm {

    /**
     * @return the tree
     */
    public Tree<Pair<Type, String>> getTree() {
        return tree;
    }

    public static enum Type {

        CONSTANT, VARIABLE, LAMBDA, APPLY, EXISTS, CONJ, ARGMAX, ARGMIN
    };
    private Type type;
    private List<LambdaTerm> sub;
    private String x = null;
    private Tree<Pair<Type, String>> tree;

    private LambdaTerm(Type type) {
        this.type = type;
        this.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(type, "");
        this.tree.addNode(pair, null);
    }

    // initialise LambdaTerm with a given Tree
    private LambdaTerm(Tree<Pair<Type, String>> tree) {
        this.type = tree.getLabel(tree.getRoot()).left;
        this.tree = tree;
        this.x = tree.getLabel(tree.getRoot()).right;
    }

    public static LambdaTerm constant(String x) {
        LambdaTerm ret = new LambdaTerm(Type.CONSTANT);
        ret.x = x;
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.CONSTANT, x);
        ret.getTree().addNode(pair, null);
        return ret;
    }

    public static LambdaTerm variable(String x) {
        LambdaTerm ret = new LambdaTerm(Type.VARIABLE);
        ret.x = x;
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.VARIABLE, x);
        ret.getTree().addNode(pair, null);
        return ret;
    }

    public static LambdaTerm lambda(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.LAMBDA);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.LAMBDA, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub.getTree(), ret.getTree().getRoot());
        return ret;
    }

    public static LambdaTerm apply(LambdaTerm functor, List<LambdaTerm> arguments) {
        LambdaTerm ret = new LambdaTerm(Type.APPLY);
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(functor);
        ret.sub.addAll(arguments);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.APPLY, "");
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( functor.getTree(), ret.getTree().getRoot());
        for (LambdaTerm argument : arguments) {
            ret.getTree().addSubTree( argument.getTree(), ret.getTree().getRoot());
        }
        ret.x = "";
        return ret;
    }

    public static LambdaTerm apply(LambdaTerm functor, LambdaTerm... arguments) {
        return apply(functor, Arrays.asList(arguments));
    }

    public static LambdaTerm exists(String x, LambdaTerm sub) {
        LambdaTerm ret = new LambdaTerm(Type.EXISTS);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.EXISTS, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub.getTree(), ret.getTree().getRoot());
        return ret;
    }

    public static LambdaTerm conj(List<LambdaTerm> subs) {
        LambdaTerm ret = new LambdaTerm(Type.CONJ);
        ret.sub = new ArrayList<LambdaTerm>(subs);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        ret.x = "";
        Pair<Type, String> pair = new Pair<Type, String>(Type.APPLY, null);
        ret.getTree().addNode(pair, null);
        for (LambdaTerm argument : subs) {
            ret.getTree().addSubTree( argument.getTree(), ret.getTree().getRoot());
        }
        return ret;
    }

    public static LambdaTerm conj(LambdaTerm... subs) {
        return conj(Arrays.asList(subs));
    }

    public static LambdaTerm argmax(String x, LambdaTerm sub1, LambdaTerm sub2) {
        LambdaTerm ret = new LambdaTerm(Type.ARGMAX);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub1);
        ret.sub.add(sub2);
        // merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.ARGMAX, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub1.getTree(), ret.getTree().getRoot());
        ret.getTree().addSubTree( sub2.getTree(), ret.getTree().getRoot());
        return ret;
    }

    public static LambdaTerm argmin(String x, LambdaTerm sub1, LambdaTerm sub2) {
        LambdaTerm ret = new LambdaTerm(Type.ARGMIN);
        ret.x = x;
        ret.sub = new ArrayList<LambdaTerm>();
        ret.sub.add(sub1);
        ret.sub.add(sub2);// merge trees
        ret.tree = new Tree<Pair<Type, String>>();
        Pair<Type, String> pair = new Pair<Type, String>(Type.ARGMIN, x);
        ret.getTree().addNode(pair, null);
        ret.getTree().addSubTree( sub1.getTree(), ret.getTree().getRoot());
        ret.getTree().addSubTree( sub2.getTree(), ret.getTree().getRoot());
        return ret;
    }

     // given a list of nodes which shall be substituted
    // substitute every node with the content given
    private Tree<Pair<Type, String>> substitute(final String varName, final Tree<Pair<Type, String>> content, final Tree<Pair<Type, String>> treeToWorkOn) {
        //System.out.println("Ersetze "+nodes+" durch "+content);

        Tree<Pair<Type, String>> ret = new Tree<Pair<Type, String>>();
        TreeVisitor<String, Tree<Pair<Type, String>>> tv = new TreeVisitor<String, Tree<Pair<Type, String>>>() {

            @Override
            public String getRootValue() {
                return treeToWorkOn.getRoot();
            }

            @Override
            public Tree<Pair<Type, String>> combine(String node, List<Tree<Pair<Type, String>>> childValues) {
                Tree<Pair<Type, String>> ret = new Tree<Pair<Type, String>>();
                // replace node
                if (treeToWorkOn.getLabel(node).right.equals(varName) && treeToWorkOn.getLabel(node).left == Type.VARIABLE ) {
                    ret = content;
                } else {
                    // delete upmost node (lamda)
                    // should not be more than 1 child
                    if (node.equals(treeToWorkOn.getRoot())) {
                        //    System.out.println("Wuzel löschen");
                        ret = childValues.get(0);
                    } // do not replace
                    else {
                        Pair<Type, String> label = treeToWorkOn.getLabel(node);
                        ret.addNode(label, ret.getRoot());
                        for (Tree<Pair<Type, String>> child : childValues) {
                            ret.addSubTree(child, "n1");
                        }
                    }
                }
                return ret;
            }
        };
        ret = treeToWorkOn.dfs(tv);
        return ret;
    }

    // applies beta-reduction to the LamdaTerm once
    private LambdaTerm beta() {

        // beginning tree visitor
        TreeVisitor<String, Tree<Pair<Type, String>>> tv = new TreeVisitor<String, Tree<Pair<Type, String>>>() {

            @Override
            public String getRootValue() {
                return getTree().getRoot();
            }

            @Override
            public Tree<Pair<Type, String>> combine(String parent, List<Tree<Pair<Type, String>>> childValues) {
                Pair<Type, String> parentLabel = getTree().getLabel(parent);
                Tree<Pair<Type, String>> ret = new Tree<Pair<Type, String>>();

                // if we have an APPLY node
                if (parentLabel.left == Type.APPLY) {
                    // if there is a variable to fill
                    Tree<Pair<Type, String>> functor = childValues.get(0);
                    Pair<Type, String> label = functor.getLabel(functor.getRoot());
                    if (label.left == Type.LAMBDA
                            || label.left == Type.ARGMAX
                            || label.left == Type.ARGMIN
                            || label.left == Type.EXISTS) {

                        // if there is more than one child, use the first argument
                        // and create a new apply node
                        if (childValues.size() > 2) {
                            ret = new Tree<Pair<Type, String>>();
                            Pair<Type, String> pair = new Pair<Type, String>(Type.APPLY, "");
                            ret.addNode(pair, null);
                            ret.addSubTree(substitute(label.right, childValues.get(1), functor), ret.getRoot());
                            for (Tree<Pair<Type, String>> argument : childValues.subList(2,childValues.size())) {
                                ret.addSubTree(argument, ret.getRoot());
                            }

                        } else {
                            // replace variables with first arguments
                            ret = substitute(label.right, childValues.get(1), functor);
                        }

                    } // else: no variable to fill - just make new Tree
                    else {

                        Pair<Type, String> p = new Pair<Type, String>(Type.APPLY, "");
                        // check hier
                        ret.addNode(p, ret.getRoot());

                        for (Tree<Pair<Type, String>> child : childValues) {
                            ret.addSubTree(child, ret.getRoot());
                        }
                    }
                } // else: node is not an apply node - just pass up as Tree
                else {
                    ret = new Tree<Pair<Type, String>>();
                    ret.addNode(parentLabel, null);
                    // check hier
                    for (Tree<Pair<Type, String>> child : childValues) {
                        ret.addSubTree(child, ret.getRoot());
                    }
                }
                return ret;
            }
        };
        // end tree visitor
        LambdaTerm ret = new LambdaTerm(getTree().dfs(tv));

        return ret;
    }

    // applies beta-rerduction to a LambdaTerm until no more is possible
    public LambdaTerm reduce() {
        LambdaTerm old = this;

        Boolean t = true;
        while (t == true) {
            LambdaTerm temp = old.beta();
            if (temp.getTree().equals(old.getTree())) {

                t = false;
            }
            old = temp;
        }

        return old;

    }

    public Pair<LambdaTerm, LambdaTerm> split(String top, String bottom) {


        return null;
    }


    // TODO - Rewrite to make subs obsolete
    @Override
    public String toString() {
        return type + (x == null ? "" : ("." + x)) + (sub == null ? "" : ("." + sub.toString()));
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LambdaTerm other = (LambdaTerm) obj;
        if (this.getTree().equals(other.getTree())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 59 * hash + (this.sub != null ? this.sub.hashCode() : 0);
        hash = 59 * hash + (this.x != null ? this.x.hashCode() : 0);
        return hash;
    }
}
