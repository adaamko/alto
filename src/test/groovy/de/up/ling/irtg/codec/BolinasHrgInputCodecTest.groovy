/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec

import org.junit.*
import java.util.*
import java.io.*
import de.up.ling.irtg.*
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.*
import de.up.ling.irtg.automata.TreeAutomaton
import static org.junit.Assert.*
import de.saar.chorus.term.parser.*;
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.algebra.Algebra
import de.up.ling.irtg.algebra.graph.GraphAlgebra
import de.up.ling.irtg.hom.*;
import static de.up.ling.irtg.util.TestingTools.*;


/**
 *
 * @author koller
 */
class BolinasHrgInputCodecTest {
    @Test
    public void testHrg1() {
        String hrg = '''\n\
START -> (. :N_0_root_1$ ); 0.5
N_0_root_1 -> (. :N_0_0$  :ARG0 (. :N_1_1$ ) :N_2$ (.sheep ) ); 0.002
N_0_0 -> (."need-01" ); 0.002
N_1_1 -> (.i ); 0.032
N_2 -> (. :ARG1 .*0 ); 0.112
        ''';
        
        assertAmrInHrgLanguage(hrg, I_NEED_SHEEP)
    }
    
    @Test
    public void testHrg2() {
        String hrg = '''START -> (. :N_0_root_1$ ); 0.5
N_0_root_1 -> (. :N_0_0$  :ARG0 (. :N_1_1$ ) :N_2$ (.sheep ) ); 0.002
N_0_0 -> (."need-01" ); 0.002
N_1_1 -> (.i ); 0.032
N_2 -> (. :ARG1 .*1 ); 0.112
        ''';
        
        assertAmrInHrgLanguage(hrg, I_NEED_SHEEP)
    }
    
    @Test
    public void testHrg3() {
        String hrg = '''START -> (. :N_0_root_1$ ); 0.5
N_0_root_1 -> (. :N_0_0$  :ARG0 (. :N_1_1$ ) :N_2$ (.sheep ) ); 0.002
N_0_0 -> (."need-01" ); 0.002
N_1_1 -> (.i ); 0.032
N_2 -> (. :ARG1 .*2 ); 0.112
        ''';
        
        assertAmrInHrgLanguage(hrg, I_NEED_SHEEP)
    }

    @Test
    public void testWideGrammar() {
        InterpretedTreeAutomaton irtg = phrg(WIDE_GRAMMAR);
        
        assertEquals(irtg.getAutomaton().getFinalStates().size(),1);  
        assertEquals(irtg.getAutomaton().countTrees(),1);
    } 
    
    //@Test
    public void testCGrammarLanguage() {
        assertAmrInHrgLanguage(CHRISTOPH_C_GRAMMAR, SMART_BOY_GIRL)
    }
    
    
    
    
    /************** utility functions ***************/
    
    
    /**
     * Test String taken from the  
     * public Release 1.4 of the AMR Bank. (1,562 sentences from The Little
     * Prince; November 14, 2014) which were generated by
     *  -The Linguistic Data Consortium SDL
     *  -The University of Colorado's Center for Computational Language and Education Research (CLEAR)
     *  -The University of Southern California's Information Sciences Institute (ISI)
     *   and Computational Linguistics at USC. 
     */
    private static final String I_NEED_SHEEP = """(n / need-01
  :ARG0 (i / i)
  :ARG1 (s / sheep))""";
    
    private static final String SMART_BOY_GIRL = """
       (a / want\n\
        :arg0 (b / boy :mod (c / smart))
	:arg1 (d / believe\n\
                :arg0 (e / girl)\n\
                :arg1 (f / want	\n\
                        :arg0 (b)\n\
                        :arg1 (b) )))
""";
    
    private void assertAmrInHrgLanguage(String hrg, String amr) {
        InterpretedTreeAutomaton irtg = phrg(hrg);        
        TreeAutomaton chart = irtg.parse(["Graph":amr])        
        assert ! chart.isEmpty();     
    }
    
    private InterpretedTreeAutomaton phrg(String hrg) {
        BolinasHrgInputCodec ic = new BolinasHrgInputCodec();
        return ic.read(hrg);
    }
    
    private static final WIDE_GRAMMAR = '''#COMMENT
T -> (. :want :arg0 (x. :E$) :arg1 (. :T$ x.));
T -> (a. :believe :arg0 (x. :girl) :arg1 (b. :T$ x. c.*));
T -> (. :want :arg0 .*1 :arg1 .*2 );
E -> (. :boy);''';
}