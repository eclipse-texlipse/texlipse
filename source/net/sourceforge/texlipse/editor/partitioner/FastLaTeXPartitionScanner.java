package net.sourceforge.texlipse.editor.partitioner;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;


/**
 * This scanner recognizes math, verbatim and comments.
 */
public class FastLaTeXPartitionScanner implements IPartitionTokenScanner {

    public final static String TEX_DEFAULT = "__tex_default";
    public final static String TEX_COMMENT = "__tex_commentPartition";
    public static final String TEX_MATH = "__tex_mathPartition"; 
    public static final String TEX_CURLY_BRACKETS = "__tex_curlyBracketPartition";
    public static final String TEX_SQUARE_BRACKETS = "__tex_squareBracketPartition";
    public static final String TEX_VERBATIM = "__tex_VerbatimPartition";
    public static final String TEX_TIKZPIC = "__tex_TikzPartition";

    public static final String[] TEX_PARTITION_TYPES = new String[] {
        IDocument.DEFAULT_CONTENT_TYPE,
        TEX_COMMENT,
        TEX_MATH,
        TEX_CURLY_BRACKETS,
        TEX_SQUARE_BRACKETS,
        TEX_VERBATIM,
        TEX_TIKZPIC};

    // states
    private static final int TEX = 0;
    private static final int COMMENT = 1;
    private static final int MATH = 2;
    private static final int VERBATIM = 3;
    private static final int ARGS = 4;
    private static final int OPT_ARGS = 5;
    private static final int TIKZPIC = 6;

    private static final String BEGIN = "begin";
    private static final String END = "end";
    private static final String VERB = "verb";
    private static final String LSTINLINE = "lstinline";
    private static final String TIKZCOMMAND = "tikz";
    private static final String TIKZPICTURE_BEGIN = "tikzpicture";
    private static final String TIKZPICTURE_END = "endtikzpicture";

    private static final String[] MATHRULES = {"equation", "eqnarray", "align", "alignat", "flalign", "multline", "gather"};
    private static final String[] MATHRULESSTAR = {"equation*", "eqnarray*", "align*", "alignat*", "flalign*", "multline*", "gather*"};
    private static final String[] MATHRULESNOSTAR = {"math", "displaymath"};
    private static final String[] COMMENTRULES = {"comment"};
    private static final String[] VERBATIMRULES = {"verbatim", "Verbatim", "lstlisting"};
    private static final String[] TIKZRULES = {"tikzpicture"};

    /** The scanner. */
    private final BufferedDocumentScanner fScanner= new BufferedDocumentScanner(1000);    // faster implementation

    private final IToken[] fTokens= new IToken[] {
            new Token(null),
            new Token(TEX_COMMENT),
            new Token(TEX_MATH),
            new Token(TEX_VERBATIM),
            new Token(TEX_CURLY_BRACKETS),
            new Token(TEX_SQUARE_BRACKETS),
            new Token(TEX_TIKZPIC)
        };
    private int fTokenOffset;
    private int fTokenLength;
    private String currContentType;

    public void setPartialRange(IDocument document, int offset, int length, String contentType, int partitionOffset) {
        //Set start of range to partitionOffset if contentType is not default
        if(!IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) && partitionOffset != -1 && partitionOffset < offset){
            fScanner.setRange(document, partitionOffset, length+(offset-partitionOffset));
            fTokenOffset= partitionOffset;
            fTokenLength= 0;
            currContentType = null;
            
        }else{
            fScanner.setRange(document, offset, length);
            fTokenOffset= offset;
            fTokenLength= 0;
            currContentType = contentType;
        }
    }

    public void setRange(IDocument document, int offset, int length) {
        currContentType = null;
        fScanner.setRange(document, offset, length);
        fTokenOffset= offset;
        fTokenLength= 0;
    }


    public int getTokenOffset() {
        return fTokenOffset;
    }

    public int getTokenLength() {
        return fTokenLength;
    }

    /*
     * @see org.eclipse.jface.text.rules.ITokenScanner#nextToken()
     */
    public IToken nextToken() {
        fTokenOffset += fTokenLength;
        fTokenLength= 0;
        
        int ch= fScanner.read();
        if(ch == ICharacterScanner.EOF){
            fTokenLength++;
            return Token.EOF;
        }

        if(currContentType != null){
            //Ignore this case
        }


        // characters
        switch (ch) {
        case '\\':
            int c1 = fScanner.read();
            if (c1 != 'b' && c1 != '[' && c1 != '('
                    && c1 != 'v' && c1 != 'l' && c1 != 't') {
                fTokenLength+=2;
                return fTokens[TEX];
            }
            if (c1 == '(' || c1 == '[') {
                int offsetEnd = fTokenOffset;
                ch = fScanner.read();
                offsetEnd+=2;
                while (true){
                    ch = fScanner.read();
                    offsetEnd++;
                    if (ch == '\\'){
                        int ch2 = fScanner.read();
                        offsetEnd++;
                        if ((ch2 == ']' && c1 == '[') || (ch2 == ')' && c1 == '(')) {
                            offsetEnd++;
                            fTokenLength = offsetEnd-fTokenOffset;
                            return fTokens[MATH];
                        }
                    }
                    else if (ch == '%') {
                        offsetEnd += ignoreComment();
                    }
                    else if (ch == '$') {
                        //Something wrong in the code
                        //Tag everything except $ as MATH and stop
                        fScanner.unread();
                        fTokenLength = offsetEnd-fTokenOffset;
                        return fTokens[MATH];
                    }
                    else if (ch == ICharacterScanner.EOF){
                        fTokenLength = offsetEnd-fTokenOffset-1;
                        return fTokens[MATH];
                    }
                }
            }
            else if (c1 == 'b') {
                return checkForEnv();
            }
            else if (c1 == 'v' || c1 == 'l') {
                return checkForVerb();
            }
            else if (c1 == 't') {
                return checkForTikz();
            }
            else {
                fTokenLength+=2;
                return fTokens[TEX];                                  
            }
        case '$':
            int offsetEnd = fTokenOffset;
            c1 = fScanner.read();
            offsetEnd+=2;
            while (true) {
                ch = fScanner.read();
                offsetEnd++;
                if (ch == '\\'){
                    ch = fScanner.read();
                    offsetEnd++;
                    if (ch == '[' || ch == ']' || ch == '(' || ch == ')'){
                        //Something is wrong here
                        //Tag everything except \( as MATH and stop
                        fScanner.unread();
                        fScanner.unread();
                        offsetEnd -= 2;
                        fTokenLength = offsetEnd-fTokenOffset;
                        return fTokens[MATH];                         
                    }
                }
                else if (ch == '%') {
                    offsetEnd += ignoreComment();
                }
                else if (ch == '$' && c1 != '$') {
                    fTokenLength = offsetEnd-fTokenOffset;
                    return fTokens[MATH];
                }
                else if (ch == '$') {
                    c1 = ' ';
                }
                else if (ch == ICharacterScanner.EOF) {
                    fTokenLength = offsetEnd-fTokenOffset-1;
                    return fTokens[MATH];
                }
            }
        case '%':
            offsetEnd = fTokenOffset;
            offsetEnd++;
            while (true) {
                ch = fScanner.read();
                offsetEnd++;
                if (ch == '\r' || ch == '\n') {
                    fScanner.unread();
                    offsetEnd--;
                    fTokenLength = offsetEnd-fTokenOffset;
                    return fTokens[COMMENT];
                }
                else if (ch == ICharacterScanner.EOF) {
                    fTokenLength = offsetEnd-fTokenOffset-1;
                    return fTokens[COMMENT];
                }
            }
        case '{':
            return scanBracket('{', '}', ARGS, fTokenOffset + 1);
        case '[':
            return scanBracket('[', ']', OPT_ARGS, fTokenOffset + 1);
        default:
            offsetEnd = fTokenOffset+1;
            while (ch != '$' && ch != '\\' && ch != '%' && ch != '{'
                    && ch != '[' && ch != ICharacterScanner.EOF) {
                ch = fScanner.read();
                offsetEnd++;                 
            }
            if (ch != ICharacterScanner.EOF) fScanner.unread();
            offsetEnd--;
            fTokenLength=offsetEnd-fTokenOffset;
            return fTokens[TEX];
        }
    }

    private int ignoreComment() {
        int ch = fScanner.read();
        int r=1;
        while (ch != '\r' && ch != '\n' && ch != ICharacterScanner.EOF) {
            ch = fScanner.read();
            r++;
        }
        return r;
    }

    private int checkForCommand(String command, int start) {
        for (int i=start; i<command.length(); i++) {
            int ch = fScanner.read();
            if (command.charAt(i) != ch) {
                unReadScanner(i - start + 1);
                return 0;
            }
        }
        return command.length() - start;
    }

    private IToken checkForVerb() {
        int o = checkForCommand(VERB, 1);
        if (o == 0) {
            o = checkForCommand(LSTINLINE, 1);
            if (o == 0) {
                fTokenLength += 2;
                return fTokens[TEX];
            }
        }
        int offsetEnd = fTokenOffset;
        offsetEnd += o + 2;
        //verbch is the termination character
        int verbch = fScanner.read();
        offsetEnd++;
        if (Character.isLetter(verbch)) {
            fTokenLength = offsetEnd-fTokenOffset;
            return fTokens[TEX];
        }
        int ch = fScanner.read();
        offsetEnd++;
        while (ch != verbch && ch != ICharacterScanner.EOF && ch != '\r' && ch != '\n') {
            ch = fScanner.read();
            offsetEnd++;
        }
        if (ch != verbch) offsetEnd--;
        fTokenLength = offsetEnd-fTokenOffset;
        return fTokens[VERBATIM];
    }

    private IToken checkForTikz() {
        boolean single = true;
        int offsetEnd = fTokenOffset + 2;
        int o = checkForCommand(TIKZCOMMAND, 1);
        if (o > 0) {
            offsetEnd += o;
            int ob = checkForCommand(TIKZPICTURE_BEGIN, 4);
            if (ob > 0) {
                single = false;
                offsetEnd += ob;
            }
        }
        else {
            fTokenLength += 2;
            return fTokens[TEX];
        }
        int ch = fScanner.read();
        offsetEnd++;
        // Skip optional arguments at beginning of environment
        if (ch == '[') {
            boolean skip = true;
            while (skip) {
                ch = fScanner.read();
                offsetEnd++;
                if (ch == '\\') {
                    ch = fScanner.read();
                    offsetEnd++;
                }
                else if (ch == ']') {
                    ch = fScanner.read();
                    offsetEnd++;
                    skip = false;
                }
                else if (ch == '%') {
                    offsetEnd += ignoreComment();
                }
                else if (ch == ICharacterScanner.EOF) {
                    // Something got screwed up when setting optional arguments
                    // for the environment - mark everything as plain latex
                    fTokenLength = offsetEnd - fTokenOffset - 1;
                    return fTokens[TEX];
                }
            }
        }
        while (true) {
            ch = fScanner.read();
            offsetEnd++;
            if (single && ch == ';') {
                fTokenLength = offsetEnd - fTokenOffset;
                return fTokens[TIKZPIC];
            }
            else if (ch == '%') {
                offsetEnd += ignoreComment();
            }
            else if (ch == '\\') {
                ch = fScanner.read();
                offsetEnd++;
                if (!single && ch == 'e') {
                    o = checkForCommand(TIKZPICTURE_END, 1);
                    if (o > 0) {
                        fTokenLength = offsetEnd + o - fTokenOffset;
                        return fTokens[TIKZPIC];
                    }
                }
            }
            else if (ch == ICharacterScanner.EOF) {
                fTokenLength = offsetEnd - fTokenOffset - 1;
                return fTokens[TIKZPIC];
            }
        }
    }

    private IToken checkForEnv() {
        int o = checkForCommand(BEGIN, 1);
        if (o == 0) {
            fTokenLength += 2;
            return fTokens[TEX];
        }
        int offsetEnd = fTokenOffset;
        offsetEnd += 6;
        int ch = fScanner.read();
        offsetEnd++;
        while (Character.isWhitespace(ch)) {
            ch = fScanner.read();
            offsetEnd++;
        }
        if (ch != '{'){
            unReadScanner(offsetEnd - fTokenOffset - 2);
            fTokenLength += 2;
            return fTokens[TEX];
        }
        final StringBuilder b = new StringBuilder();
        ch = fScanner.read();
        offsetEnd++;
        while (ch != '}' && ch != ICharacterScanner.EOF && ch != '{' && ch != '\\'){
            b.append((char)ch);
            ch = fScanner.read();
            offsetEnd++;
        }
        String envName = b.toString();
        if (getEnvIndex(envName) != TEX) {
            return checkForEndEnv(envName, offsetEnd);
        }
        else {
            unReadScanner(offsetEnd - fTokenOffset - 2);
            fTokenLength += 2;
            return fTokens[TEX];
        }
    }

    private IToken checkForEndEnv(String name, int offsetEnd) {
        while (true) {
            int ch = fScanner.read();
            offsetEnd++;
            if (ch == '%') {
                offsetEnd += ignoreComment();
                ch = fScanner.read();
                offsetEnd++;
            }
            if (ch == '\\') {
                boolean isEnv = true;
                for (int i=0; i<END.length(); i++) {
                    ch = fScanner.read();
                    offsetEnd++;
                    if (END.charAt(i) != ch) {
                        isEnv = false;
                        break;
                    }
                }
                if (!isEnv) continue;
                ch = fScanner.read();
                offsetEnd++;
                while (Character.isWhitespace(ch)) {
                    ch = fScanner.read();
                    offsetEnd++;
                }
                if (ch != '{') continue;
                StringBuilder b = new StringBuilder();
                ch = fScanner.read();
                offsetEnd++;
                while (ch != '}' && ch != ICharacterScanner.EOF && ch != '{' && ch != '\\'){
                    b.append((char)ch);
                    ch = fScanner.read();
                    offsetEnd++;
                }
                String envName = b.toString();
                if (envName.equals(name)) {
                    fTokenLength = offsetEnd-fTokenOffset;
                    return fTokens[getEnvIndex(envName)];
                }
            }
            else if (ch == ICharacterScanner.EOF) {
                fTokenLength = offsetEnd-fTokenOffset-1;
                return fTokens[getEnvIndex(name)];
            }
        }
    }

    private IToken scanBracket(int openChar, int closeChar, int type, int currentOffset) {
        int ch;
        int offsetEnd = currentOffset;
        int stack = 0;
        while (true) {
            ch = fScanner.read();
            offsetEnd++;
            if (ch == closeChar) {
                stack--;
                if (stack < 0) {
                    fTokenLength = offsetEnd - fTokenOffset;
                    return fTokens[type];
                }
            }
            else if (ch == openChar) {
                stack++;
            }
            else if (ch == '%') {
                offsetEnd += ignoreComment();
            }
            else if (ch == '\\') {
                ch = fScanner.read();
                offsetEnd++;
            }
            else if (ch == ICharacterScanner.EOF) {
                fTokenLength = offsetEnd - fTokenOffset - 1;
                return fTokens[type];
            }
        }
    }
    
    
    private boolean unReadScanner(int readChar) {
        for (int j = 0; j < readChar; j++)
            fScanner.unread();
        return false;
    }

    private static final boolean matchesAny(final String envName, final String[] rules) {
        for (String st : rules) {
            if (st.equals(envName)) return true;
        }

        return false;
    }

    private static final int getEnvIndex(final String envName) {
        if (isMathEnv(envName)) return MATH;
        else if (isVerbatimEnv(envName)) return VERBATIM;
        else if (isCommentEnv(envName)) return COMMENT;
        else if (isTikzEnv(envName)) return TIKZPIC;
        else return TEX;
    }

    /**
     * 
     * @param envName Name of the environment
     * @return true, if the given name denotes a math environment
     */
    public static final boolean isMathEnv(String envName) {
        return matchesAny(envName, MATHRULES)
                || matchesAny(envName, MATHRULESSTAR)
                || matchesAny(envName, MATHRULESNOSTAR);
    }

    public static final boolean isVerbatimEnv(String envName) {
        return matchesAny(envName, VERBATIMRULES);
    }

    public static final boolean isCommentEnv(String envName) {
        return matchesAny(envName, COMMENTRULES);
    }

    public static final boolean isTikzEnv(String envName) {
        return matchesAny(envName, TIKZRULES);
    }

}

