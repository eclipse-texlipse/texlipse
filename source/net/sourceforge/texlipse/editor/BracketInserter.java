/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.partitioner.FastLaTeXPartitionScanner;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

/**
 * This class is used for automatic bracket (and dollar sign) closing
 * and for replacement of quotation marks. Most parts are copied from
 * the Eclipse JDT. 
 * 
 * @author Boris von Loesch
 * @author Oskar Ojala
 */

public class BracketInserter implements VerifyKeyListener, ILinkedModeListener {
    
    private class ExitPolicy implements IExitPolicy {
        
        final char fExitCharacter;
        final char fEscapeCharacter;
        final Stack<BracketLevel> fStack;
        final int fSize;
        final ISourceViewer sourceViewer;
        
        public ExitPolicy(char exitCharacter, char escapeCharacter, Stack<BracketLevel> stack, ISourceViewer viewer) {
            fExitCharacter= exitCharacter;
            fEscapeCharacter= escapeCharacter;
            fStack= stack;
            fSize= fStack.size();
            sourceViewer = viewer;
        }
        
        /*
         * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
         */
        public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
            
            if (fSize == fStack.size() && !isMasked(offset)) {
                if (event.character == fExitCharacter) {
                    BracketLevel level= (BracketLevel) fStack.peek();
                    if (level.fFirstPosition.offset > offset || level.fSecondPosition.offset < offset)
                        return null;
                    if (level.fSecondPosition.offset == offset && length == 0)
                        // don't enter the character if if its the closing peer
                        return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
                }
                // when entering an anonymous class between the parenthesis', we don't want
                // to jump after the closing parenthesis when return is pressed
                if (event.character == SWT.CR && offset > 0) {
                    IDocument document= sourceViewer.getDocument();
                    try {
                        if (document.getChar(offset - 1) == '{')
                            return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
                    } catch (BadLocationException e) {
                    }
                }
            }
            return null;
        }
        
        private boolean isMasked(int offset) {
            IDocument document= sourceViewer.getDocument();
            try {
                return fEscapeCharacter == document.getChar(offset - 1);
            } catch (BadLocationException e) {
            }
            return false;
        }
    }
    
    private static class BracketLevel {
        int fOffset;
        int fLength;
        LinkedModeUI fUI;
        Position fFirstPosition;
        Position fSecondPosition;
    }
    
    /**
     * Position updater that takes any changes at the borders of a position to
     * not belong to the position.
     *
     * @since 3.0
     */
    private static class ExclusivePositionUpdater implements IPositionUpdater {
        
        /** The position category. */
        private final String fCategory;
        
        /**
         * Creates a new updater for the given <code>category</code>.
         *
         * @param category the new category.
         */
        public ExclusivePositionUpdater(String category) {
            fCategory= category;
        }
        
        /*
         * @see org.eclipse.jface.text.IPositionUpdater#update(org.eclipse.jface.text.DocumentEvent)
         */
        public void update(DocumentEvent event) {
            
            int eventOffset= event.getOffset();
            int eventOldLength= event.getLength();
            int eventNewLength= event.getText() == null ? 0 : event.getText().length();
            int deltaLength= eventNewLength - eventOldLength;
            
            try {
                Position[] positions= event.getDocument().getPositions(fCategory);
                
                for (int i= 0; i != positions.length; i++) {
                    
                    Position position= positions[i];
                    
                    if (position.isDeleted())
                        continue;
                    
                    int offset= position.getOffset();
                    int length= position.getLength();
                    int end= offset + length;
                    
                    if (offset >= eventOffset + eventOldLength)
                        // position comes
                        // after change - shift
                        position.setOffset(offset + deltaLength);
                    else if (end <= eventOffset) {
                        // position comes way before change -
                        // leave alone
                    } else if (offset <= eventOffset && end >= eventOffset + eventOldLength) {
                        // event completely internal to the position - adjust length
                        position.setLength(length + deltaLength);
                    } else if (offset < eventOffset) {
                        // event extends over end of position - adjust length
                        int newEnd= eventOffset;
                        position.setLength(newEnd - offset);
                    } else if (end > eventOffset + eventOldLength) {
                        // event extends from before position into it - adjust offset
                        // and length
                        // offset becomes end of event, length adjusted accordingly
                        int newOffset= eventOffset + eventNewLength;
                        position.setOffset(newOffset);
                        position.setLength(end - newOffset);
                    } else {
                        // event consumes the position - delete it
                        position.delete();
                    }
                }
            } catch (BadPositionCategoryException e) {
                // ignore and return
            }
        }
        
        /**
         * Returns the position category.
         *
         * @return the position category
         */
        public String getCategory() {
            return fCategory;
        }
        
    }
    
    private final String CATEGORY= toString();
    private IPositionUpdater fUpdater= new ExclusivePositionUpdater(CATEGORY);
    private Stack<BracketLevel> fBracketLevelStack= new Stack<BracketLevel>();
    private final ISourceViewer sourceViewer;
    private final IEditorPart editor;
    private static HashMap<String, String> quotes;
    private static final Pattern SIMPLE_COMMAND_PATTERN = Pattern.compile("\\\\.\\{\\\\?\\w\\}");

    public BracketInserter(ISourceViewer viewer, IEditorPart editor) {
        this.sourceViewer = viewer;
        this.editor = editor;
        if (quotes == null) {
            quotes = new HashMap<String, String>();
            quotes.put("eno", "``");
            quotes.put("enc", "''");
            quotes.put("fio", "''");
            quotes.put("fic", "''");
            quotes.put("fro", "\"<");
            quotes.put("frc", "\">");
            quotes.put("deo", "\"`");
            quotes.put("dec", "\"'");
        }
    }
    
    /**
     * Tests if the given character is a bracket or paren
     * 
     * @param c Chracter to test
     * @return True if <code>c</code> is a bracket or paren, false otherwise
     */
    private static boolean isClosingBracket(char c){
        if (c == ')' || c == '}' || c == ']' || c == '$')
            return true;
        return false;
    }
    
    /**
     * Returns the opposing paren, e.g. returns ')' for '(' 
     * 
     * @param character A bracket or paren
     * @return The opposing bracket or paren
     */
    private static char getPeerCharacter(char character) {
        switch (character) {
        case '(':
            return ')';
        case ')':
            return '(';
        case '{':
            return '}';
        case '}':
            return '{';
        case '[':
            return ']';
        case '$':
            return '$';
        case ']':
            return '[';
        default:
            return 0;
        }
    }
    
    /**
     * Returns true if <i>next</i> is a character that could stand
     * behind a closing quotation mark instead of a white space
     * @param next
     * @return
     */
    private boolean isLikePunctuationMark(char next) {
        switch (next) {
        case '.':
        case ',':
        case '!':
        case '?':
        case ';':
        case ':':
        case '-':
        case ')':
        case ']':
        case '}':
        case '=':
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns true if <i>next</i> is a character that could stand
     * before an opening quotation mark instead of a white space
     * @param next
     * @return
     */
    private boolean isLikeOpeningBrace(char next) {
        switch (next) {
        case '(':
        case '[':
        case '{':
        case '~':
        case '=':
        case ':':
            return true;
        default:
            return false;
        }
    }

    /**
     * Gets the quote wanted for the current language
     * 
     * @param opening
     *            True if the opening quote is wanted, false if the closing
     *            quote is wanted
     * @return String containing the quotes as TeX code
     */
    private String getQuotes (boolean opening){
        String replacement;
        IProject project = ((FileEditorInput)editor.getEditorInput()).getFile().getProject();
        String lang = TexlipseProperties.getProjectProperty(project, TexlipseProperties.LANGUAGE_PROPERTY);		
        String postfix = opening ? "o" : "c";
        replacement = quotes.get(lang + postfix);
        return (replacement != null ? replacement : quotes.get("en" + postfix));
    }
    
    /*
     * @see org.eclipse.swt.custom.VerifyKeyListener#verifyKey(org.eclipse.swt.events.VerifyEvent)
     */
    public void verifyKey(VerifyEvent event) {
        // TODO separate math mode from normal typing?
        // early pruning to slow down normal typing as little as possible
        if (!event.doit)
            return;
        switch (event.character) {
        case '(':
        case '{':
        case '[':
        case '$':
        case '"':
        case '.':
        case '\b':
            break;
        default:
            return;
        }
        IDocument document = sourceViewer.getDocument();
        final Point selection = sourceViewer.getSelectedRange();
        final int offset = selection.x;
        final int length = selection.y;
        
        final char character = event.character;
        try {
            if (document instanceof IDocumentExtension3) {
                try {
                    String contentType = ((IDocumentExtension3) document).getContentType(
                            TexEditor.TEX_PARTITIONING, offset, false);
                    if (FastLaTeXPartitionScanner.TEX_VERBATIM.equals(contentType)) {
                        //No features inside verbatim environments
                        return;
                    }
                } catch (BadPartitioningException e) {
                    TexlipsePlugin.log("Bad partitioning", e);
                }
            }
            char next = ' ';
            char last = ' ';
            try {
                if (offset > 0) 
                    last = document.getChar(offset-1);
                next = document.getChar(offset);
            } catch (BadLocationException e) {
                //Could happen if this is the beginning or end of a document
            }
            if (last == '\\') 
                return;
            if (character == '"'){
                //Replace quotation marks
                if (!TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.SMART_QUOTES))
                    return;
                
                String mark;
                if (Character.isWhitespace(last) || isLikeOpeningBrace(last)) {
                    mark = getQuotes(true);
                } else if (Character.isWhitespace(next) || isLikePunctuationMark(next)) {
                    mark = getQuotes(false);
                } else {
                    return;
                }
                document.replace(offset, length, mark);
                sourceViewer.setSelectedRange(offset+mark.length(), 0);
                event.doit = false;
                return;
            }
            
            // Smart backspace
            
            if (character == '\b') {
                if (!TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.SMART_BACKSPACE)) {
                    return;
                }
                if (last == '}' && offset > 4) { // \={o} or \'{\i}
                    int distance;
                    if (document.getChar(offset-5) == '\\') {
                        distance = 5;
                    } else if (offset > 5 && document.getChar(offset-6) == '\\') {
                        distance = 6;
                    } else {
                        return;
                    }
                    String deletion = document.get(offset - distance, distance);
                    Matcher m = SIMPLE_COMMAND_PATTERN.matcher(deletion);
                    if (m.matches()) {
                        document.replace(offset - distance, distance, "");
                        event.doit = false;
                    }
                } else if (Character.isLetter(last)) {
                    // FIXME can't handle unicode
                    // \'a
                    if (offset > 2 && document.getChar(offset-3) == '\\') {
                        // "\\\\\\W\\w"
                        if (!Character.isLetter(document.getChar(offset-2))) {
                        document.replace(offset - 3, 3, "");
                        event.doit = false;
                    }
                }
                }
                return;
            }
            
            // Smart \ldots
            
            if (character == '.') {
                if (!TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.SMART_LDOTS)) {
                    return;
                }
                if (last == '.' && document.getChar(offset-2) == '.') {
                    String replacement = "\\ldots";
                    document.replace(offset-2, length+2, replacement);
                    sourceViewer.setSelectedRange(offset + replacement.length() - 2, 0);
                    event.doit = false;
                }
                return;
            }
            
            // Smart parens
            
            if (!TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.SMART_PARENS))
                return;
            
            if (Character.isWhitespace(next) || isClosingBracket(next)){
                //For a dollar sign we need a whitespace before and after the letter
                if (character == '$' && !Character.isWhitespace(last))
                    return;
            } else {
                return;
            }
            
            boolean left = false; 
            if (last == 't'){
                //Maybe we have \left then we will also append \right
                try{
                    String prev = document.get(offset - 6, 6);
                    if (prev.charAt(0) != '\\' && "\\left".equals(prev.substring(1))) 
                        left = true; 
                }
                catch (BadLocationException e) {
                    //Could happen, no worry
                }
            }
            final char closingCharacter= getPeerCharacter(character);
            final StringBuffer buffer= new StringBuffer();
            buffer.append(character);
            if (left) buffer.append("\\right");
            buffer.append(closingCharacter);
            
            document.replace(offset, length, buffer.toString());
            
            // The code below does the fancy "templateish" enter-to-exit-braces
            BracketLevel level= new BracketLevel();
            fBracketLevelStack.push(level);
            
            LinkedPositionGroup group= new LinkedPositionGroup();
            group.addPosition(new LinkedPosition(document, offset + 1, 0, LinkedPositionGroup.NO_STOP));
            
            LinkedModeModel model= new LinkedModeModel();
            model.addLinkingListener(this);
            model.addGroup(group);
            model.forceInstall();
            
            level.fOffset= offset;
            level.fLength= 2;
            if (left) level.fLength += 6;
            
            // set up position tracking for our magic peers
            if (fBracketLevelStack.size() == 1) {
                document.addPositionCategory(CATEGORY);
                document.addPositionUpdater(fUpdater);
            }
            level.fFirstPosition= new Position(offset, 1);
            level.fSecondPosition= new Position(offset + 1, level.fLength - 1);
            document.addPosition(CATEGORY, level.fFirstPosition);
            document.addPosition(CATEGORY, level.fSecondPosition);
            
            level.fUI= new EditorLinkedModeUI(model, sourceViewer);
            level.fUI.setSimpleMode(true);
            level.fUI.setExitPolicy(new ExitPolicy(closingCharacter, (char)0, 
                    fBracketLevelStack, sourceViewer));
            level.fUI.setExitPosition(sourceViewer, offset + level.fLength, 0, Integer.MAX_VALUE);
            level.fUI.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
            level.fUI.enter();
            
            
            IRegion newSelection= level.fUI.getSelectedRegion();
            sourceViewer.setSelectedRange(newSelection.getOffset(), newSelection.getLength());
            
            event.doit= false;
            
        } catch (BadLocationException e) {
        } catch (BadPositionCategoryException e) {
        }
    }
    
    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    public void left(LinkedModeModel environment, int flags) {
        
        final BracketLevel level= fBracketLevelStack.pop();
        
        if (flags != ILinkedModeListener.EXTERNAL_MODIFICATION)
            return;
        
        // remove brackets
        final IDocument document= sourceViewer.getDocument();
        if (document instanceof IDocumentExtension) {
            IDocumentExtension extension= (IDocumentExtension) document;
            extension.registerPostNotificationReplace(null, new IDocumentExtension.IReplace() {
                
                public void perform(IDocument d, IDocumentListener owner) {
                    if ((level.fFirstPosition.isDeleted || level.fFirstPosition.length == 0)
                            && !level.fSecondPosition.isDeleted
                            && level.fSecondPosition.offset == level.fFirstPosition.offset) {
                        try {
                            document.replace(level.fSecondPosition.offset,
                                    level.fSecondPosition.length,
                                    null);
                        } catch (BadLocationException e) {
                            //JavaPlugin.log(e);
                        }
                    }
                    
                    if (fBracketLevelStack.size() == 0) {
                        document.removePositionUpdater(fUpdater);
                        try {
                            document.removePositionCategory(CATEGORY);
                        } catch (BadPositionCategoryException e) {
                            //JavaPlugin.log(e);
                        }
                    }
                }
            });
        }
    }
    
    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#suspend(org.eclipse.jface.text.link.LinkedModeModel)
     */
    public void suspend(LinkedModeModel environment) {
    }
    
    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#resume(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    public void resume(LinkedModeModel environment, int flags) {
    }
}