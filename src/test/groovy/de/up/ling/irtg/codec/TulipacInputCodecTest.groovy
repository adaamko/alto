/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec


import org.junit.Test
import java.util.*
import java.util.function.Function
import org.junit.BeforeClass
import java.io.*
import com.google.common.collect.Iterators
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.binarization.BinarizingAlgebraSeed
import de.up.ling.irtg.binarization.BinaryRuleFactory
import de.up.ling.irtg.binarization.BkvBinarizer
import de.up.ling.irtg.binarization.IdentitySeed
import de.up.ling.irtg.binarization.RegularSeed
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.irtg.Interpretation
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.graph.SGraph
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;
import com.google.common.collect.ImmutableMap
import de.up.ling.irtg.binarization.*

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author koller
 */
class TulipacInputCodecTest {
    private static InterpretedTreeAutomaton irtg;
    
    
    @BeforeClass
    public static void setupClass() {
        irtg = new TulipacInputCodec().read(SHIEBER)
    }
    
    @Test
    public void testShieber() {
        TreeAutomaton chart = irtg.parse(["string": "mer es huus aastriiche"])
        TreeAutomaton filtered = irtg.getInterpretation("ft").filterNull(chart)
        
        assertThat(filtered.viterbi(), is(pt("vinf_tv-aastriiche_objcase__acc_(np_pron-mer_case__nom_(*NOP*_pron_A,*NOP*_np_A),np_n-huus__(*NOP*_n_A,adj_det-es_case__acc_(*NOP*_det_A,*NOP*_np_A)),*NOP*_S_A,*NOP*_v_A)")))
        // NB this also checks that @NA on root of vinf_tv was processed correctly,
        // otherwise there would be an extra *NOP*_S_A at the end
    }
    
    @Test
    public void testShieberCrossSerial() {
        TreeAutomaton chart = irtg.parse(["string": "mer em hans es huus hälfed aastriiche"])
        TreeAutomaton filtered = irtg.getInterpretation("ft").filterNull(chart)
        
        assertThat(filtered.viterbi(), is(pt("vinf_tv-aastriiche_objcase__acc_(np_pron-mer_case__nom_(*NOP*_pron_A,*NOP*_np_A),np_n-huus__(*NOP*_n_A,adj_det-es_case__acc_(*NOP*_det_A,*NOP*_np_A)),'vinf_tv_aux-hälfed_objcase__dat_'(np_n-hans__(*NOP*_n_A,adj_det-em_case__dat_(*NOP*_det_A,*NOP*_np_A)),*NOP*_S_A,*NOP*_v_A),*NOP*_v_A)")))
    }
    
    @Test
    public void testShieberFtClash() {
        //        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(SHIEBER)
        TreeAutomaton chart = irtg.parse(["string": "mer em hans aastriiche"])
        TreeAutomaton filtered = irtg.getInterpretation("ft").filterNull(chart)
        
        assertThat(chart.viterbi(), notNullValue())
        assertThat(filtered.viterbi(), nullValue())
    }
    
    @Test
    public void testShieberBinarization() {
        InterpretedTreeAutomaton binarized = binarize(irtg)
        
        TreeAutomaton chart = binarized.parse(["string": "mer em hans es huus hälfed aastriiche"])
        TreeAutomaton filtered = binarized.getInterpretation("ft").filterNull(chart)
        
        assertThat(filtered.viterbi(), not(nullValue()))
    }
    
    @Test
    public void testTreeHom() {
        // checks that @NA is processed correctly
        // prevents bug where @NA produced S_0 instead of S_3 label
        assertThat(irtg.getInterpretation("tree").getHomomorphism().get("vinf_tv-hälfed_objcase__dat_"), 
            is(pt("S_3(?1,'@'(?3,S_1(?2)),'@'(?4,v_1('hälfed')))")))
    }
    
    @Test
    public void testLindemannNoAgreement() {
        InterpretedTreeAutomaton lirtg = new TulipacInputCodec().read(LINDEMANN)
        TreeAutomaton chart = lirtg.parse(["string": "die Hund"])
        
        Tree dt = chart.viterbi()
        assertThat(dt, notNullValue())
        
        Interpretation ft = lirtg.getInterpretation("ft")
        Tree t = ft.getHomomorphism().apply(dt)
        assertThat(t, notNullValue())        
        assertThat(ft.getAlgebra().evaluate(t), nullValue())
    }

    @Test
    public void testLindemannAgreement() {
        InterpretedTreeAutomaton lirtg = new TulipacInputCodec().read(LINDEMANN)
        TreeAutomaton chart = lirtg.parse(["string": "der Hund"])
        
        Tree dt = chart.viterbi()
        assertThat(dt, notNullValue())
        
        Interpretation ft = lirtg.getInterpretation("ft")
        Tree t = ft.getHomomorphism().apply(dt)
        assertThat(t, notNullValue())        
        assertThat(ft.getAlgebra().evaluate(t), notNullValue())
    }

    
    private static InterpretedTreeAutomaton binarize(final InterpretedTreeAutomaton irtg) throws Exception {
        Map<String, Algebra> newAlgebras = ImmutableMap.of("string", new TagStringAlgebra(),
                                                           "tree", new BinarizingTagTreeAlgebra(),
                                                           "ft", new FeatureStructureAlgebra());

        Map<String, RegularSeed> seeds = ImmutableMap.of(
                "string", new IdentitySeed(irtg.getInterpretation("string").getAlgebra(), newAlgebras.get("string")),
                "ft", new IdentitySeed(irtg.getInterpretation("ft").getAlgebra(), newAlgebras.get("ft")),
                "tree", new BinarizingAlgebraSeed(irtg.getInterpretation("tree").getAlgebra(), newAlgebras.get("tree")));

        Function<InterpretedTreeAutomaton, BinaryRuleFactory> rff = GensymBinaryRuleFactory.createFactoryFactory();
        BkvBinarizer binarizer = new BkvBinarizer(seeds, rff);

        InterpretedTreeAutomaton binarized = binarizer.binarize(irtg, newAlgebras, null);

        return binarized;
    }

    @Test(expected = CodecParseException.class)
    public void testUndeclaredTree() {
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(UNDECLARED_TREE);
    }

    @Test(expected = CodecParseException.class)
    public void testUndeclaredFamily() {
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(UNDECLARED_FAMILY);
    }

    @Test(expected = CodecParseException.class)
    public void testFamilyWithUndeclaredTree() {
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(FAMILY_WITH_UNDECLARED_TREE);
    }

    @Test(expected = CodecParseException.class)
    public void testUndeclaredFamilyLemma() {
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(UNDECLARED_FAMILY_FOR_LEMMA);
    }

    @Test(expected = CodecParseException.class)
    public void testFamilyWithUndeclaredForLemma() {
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(FAMILY_WITH_UNDECLARED_TREE_FOR_LEMMA);
    }

    @Test
    public void testWrongOrder() {
        // Words may be defined before trees and families in the grammar file
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(WRONG_ORDER);
    }

    // Reproduce issue #44: Extra closing brackets should cause an exception.
    @Test(expected = CodecParseException.class)
    public void testExtraClosingBrackets() {
        InterpretedTreeAutomaton irtg = new TulipacInputCodec().read(EXTRA_CLOSING_BRACKETS);
    }


    
    private static final String LINDEMANN = """
tree A:
S {
  Det! [gen=?g, case=?c]
  N+ [gen=?g, case=?c]
}

tree det:
 Det+

word 'Hund' : A[gen=masc, case=nom]
word 'der' : det[gen=masc, case=nom]
word 'die' : det[gen=fem, case=nom]


""";
    
    private static final String SHIEBER = """\n\

family vinf_tv: { vinf_tv, vinf_tv_aux }

tree vinf_tv:
     S @NA {
       np! [case=nom][]
       S {
         np! [case=?o] []
       }
       v+ [objcase=?o] []
     }


tree vinf_tv_aux:
     S @NA {
     	S {
	  S @NA {
	    np! [case=?o] []
	    S*
          }
	}
	v+ [objcase=?o][]
     }




family np_n: { np_n }




tree np_n:
     np [] [case=?c] {
       n+ [case=?c] []
     }

tree adj_det:
     np [] [case=?c] {
       det+ [case=?c] []
       np* [case=?c] []
     }

tree np_pron:
     np[][case=?c] {
       pron+ [case=?c] []
     }




// LEXICON

word 'mer': np_pron[case=nom]

word 'em': adj_det[case=dat]
word 'es': adj_det[case=acc]
word 'd': adj_det[case=acc]
word 'de': adj_det[case=acc]

word 'hans': np_n
word 'huus': np_n
word 'chind': np_n

word 'aastriiche': <vinf_tv>[objcase=acc]

lemma 'laa': <vinf_tv>[objcase=acc] {
  word "lönd"
  word "laa"
}

lemma 'hälfe': <vinf_tv>[objcase=dat] {
  word 'hälfed'
  word 'hälfe'
}





    """;

    private static final String UNDECLARED_TREE = """
    word 'mer': np_pron
""";

    private static final String UNDECLARED_FAMILY = """
    word 'mer': <unk_family>
""";

    private static final String FAMILY_WITH_UNDECLARED_TREE = """
    family my_family: {tree_a, tree_b}
    word 'mer': <my_family>
""";

    private static final String UNDECLARED_FAMILY_FOR_LEMMA = """
    lemma 'mer': <unk_family> {
       word 'x'
    }
""";

    private static final String FAMILY_WITH_UNDECLARED_TREE_FOR_LEMMA = """
    family my_family: {tree_a, tree_b}
    lemma 'mer': <my_family> {
      word 'x'
    }
""";

    // declare first word, then tree
    private static final String WRONG_ORDER = """
word 'mer': np_pron[case=nom]

tree np_pron:
     np[][case=?c] {
       pron+ [case=?c] []
     }


""";


    private static final String EXTRA_CLOSING_BRACKETS = """
family alphaintrans: { intrans1, intrans2, intrans3 }

tree intrans1:
  S {
    NP![case=nom] []
    V+ 
    }

tree intrans2:
  S {
    V+
    NP![case=nom] [] 
    }

tree intrans3:
  S {
    D! [] []
    NP![case=nom] []
    V+ 
    }
} // <-- this closing bracket is too much

word 'schl"ä"ft': <alphaintrans> 
word 'ruht': <alphaintrans> 
""";
}

