/*
 * $Id$
 *
 * Copyright (c) 2008 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.texparser;

import static org.junit.Assert.*;
import static net.sourceforge.texlipse.texparser.LatexParserUtils.*;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.junit.Test;

public class LatexParserUtilsTest {

    @Test
    public void testisEscaped() {
        assertFalse(isEscaped("%", 0));
        assertFalse(isEscaped("  %", 2));
        assertTrue(isEscaped("\\%", 1));
        assertTrue(isEscaped(" \\%", 2));
        assertFalse(isEscaped("\\\\%", 2));
        assertFalse(isEscaped(" \\\\%", 3));
        assertTrue(isEscaped("\\\\\\%", 3));
        assertTrue(isEscaped(" \\\\\\%", 4));
        
        assertFalse(isEscaped("test \\begin", 5));
        assertTrue(isEscaped("test \\\\begin", 6));
    }

    @Test
    public void testgetStartofLineStringInt() {
        String testString1 = "test";
        assertTrue(getStartofLine(testString1, 2) == 0);
        
        String testString2 = "prev \rtest";
        assertTrue(getStartofLine(testString2, 8) == 6);

        String testString3 = "prev \ntest";
        assertTrue(getStartofLine(testString3, 8) == 6);

        String testString4 = "prev \r\ntest";
        assertTrue(getStartofLine(testString4, 8) == 7);
    }

    @Test
    public void testIsInsideCommentStringInt() {
        String testString1 = "% Comment %";
        assertFalse(isInsideComment(testString1, 0));
        for (int i=1; i<testString1.length(); i++) {
            assertTrue(isInsideComment(testString1, i));
        }

        String testString2 = "No % Comment ";
        for (int i=0; i < 4; i++) {
            assertFalse(isInsideComment(testString2, 1));
        }
        for (int i=4; i<testString2.length(); i++) {
            assertTrue(isInsideComment(testString2, i));
        }

        String testString3 = "No %\n Comment ";
        for (int i=5; i<testString3.length(); i++) {
            assertFalse(isInsideComment(testString3, i));
        }

        String testString4 = "\\% No Comment";
        for (int i=0; i < 4; i++) {
            assertFalse(isInsideComment(testString4, i));
        }

        String testString5 = "\\%% Comment";
        for (int i=3; i < 6; i++) {
            assertTrue(isInsideComment(testString5, i));
        }

        String testString6 = "\\\\% Comment";
        for (int i=3; i < 5; i++) {
            assertTrue(isInsideComment(testString6, i));
        }
    }

    @Test
    public void testFindCommandStringStringInt() {
        String testString1 = "\\test";
        assertEquals(0, findCommand(testString1, "\\test", 0));
        assertEquals(-1, findCommand(testString1, "\\test", 1));

        String testString2 = "\\testt";
        assertEquals(-1, findCommand(testString2, "\\test", 0));

        String testString3 = "\\test1";
        assertEquals(0, findCommand(testString3, "\\test", 0));

        String testString4 = "\\test{arg1}";
        assertEquals(0, findCommand(testString4, "\\test", 0));

        String testString5 = "% \\test";
        assertEquals(-1, findCommand(testString5, "\\test", 0));
    }

/*    @Test
    public void testFindLastCommandStringStringInt() {
        fail("Not yet implemented"); // TODO
    }*/

    @Test
    public void testFindPeerCharStringIntIntCharChar() {
        String testString1 = "{aa}";
        assertEquals(3, findPeerChar(testString1, 0, LEFT, '{', '}'));
        assertEquals(0, findPeerChar(testString1, 3, RIGHT, '}', '{'));

        String testString2 = "{{}}";
        assertEquals(3, findPeerChar(testString2, 0, LEFT, '{', '}'));
        assertEquals(2, findPeerChar(testString2, 1, LEFT, '{', '}'));
        assertEquals(0, findPeerChar(testString2, 3, RIGHT, '}', '{'));
        assertEquals(1, findPeerChar(testString2, 2, RIGHT, '}', '{'));
        
        String testString3 = "{\\}";
        assertEquals(-1, findPeerChar(testString3, 0, LEFT, '{', '}'));

        String testString4 = "{%}";
        assertEquals(-1, findPeerChar(testString4, 0, LEFT, '{', '}'));
    }

    @Test
    public void testGetCommandArgumentStringInt() {
        String testString1 = "\\test";
        assertNull(getCommandArgument(testString1, 1));

        String testString2 = "\\test{arg}";
        IRegion r1 = new Region(6, 3);
        assertEquals(r1, getCommandArgument(testString2, 1));

        String testString3 = "\\test  {arg}";
        IRegion r2 = new Region(8, 3);
        assertEquals(r2, getCommandArgument(testString3, 1));

        String testString4 = "\\test  a{arg}";
        assertNull(getCommandArgument(testString4, 1));
    }
    
    @Test
    public void testGetCommand() {
        String testString = "\\test{arg}";
        IRegion r = new Region(0, 5);
        assertEquals(r, getCommand(testString, 0));
        assertEquals(r, getCommand(testString, 5));
        assertEquals(r, getCommand(testString, 6));
        assertEquals(r, getCommand(testString, 102));

        testString = " \\test{arg} ";
        IRegion r2 = new Region(1, 5);
        assertNull(getCommand(testString, 0));
        assertEquals(r2, getCommand(testString, 5));
        assertEquals(r2, getCommand(testString, 6));
        assertNull(getCommand(testString, 102));

        testString = "\\test  \r   {arg}";
        assertEquals(r, getCommand(testString, 0));
        assertEquals(r, getCommand(testString, 13));

        testString = "\\test  u   {arg}";
        assertNull(getCommand(testString, 12));
        
        testString = "\\test{arg1}{arg2}";
        assertNull(getCommand(testString, 12));
        
        testString = "\\test a";
        assertNull(getCommand(testString, 6));
        
        testString = "% \\test";
        assertNull(getCommand(testString, 4));

        testString = "\\\\test";
        assertNull(getCommand(testString, 4));

        testString = "test";
        assertNull(getCommand(testString, 3));

        testString = "\\\\test{arg}";
        assertNull(getCommand(testString, 8));
        assertNull(getCommand("{test}", 3));
    }

    @Test
    public void testFindMatchingEndEnvironment() {
        String testString1 = " \\begin{a}\\end{a}";
        IRegion r1 = new Region(10, 7);
        assertNull(findMatchingEndEnvironment(testString1, "a", 0));
        for (int i=1; i < 10; i++) {
            assertEquals(r1, findMatchingEndEnvironment(testString1, "a", i));
        }
        for (int i=10; i < testString1.length(); i++) {
            assertNull(findMatchingEndEnvironment(testString1, "a", i));
        }

        String testString2 = "\\begin{a}\\begin{a}\\end{a}\\end{a}";
        IRegion r2 = new Region(25, 7);
        assertEquals(r2, findMatchingEndEnvironment(testString2, "a", 2));

        String testString3 = "\\begin{a}\\begin{a}\\end{a}%\\end{a}";
        assertNull(findMatchingEndEnvironment(testString3, "a", 0));

        String testString4 = "\\begin{a}\\end{b}{a}";
        assertNull(findMatchingEndEnvironment(testString4, "a", 0));
    }
}
