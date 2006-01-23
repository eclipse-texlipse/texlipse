/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Contains the LaTeX commands that can be completed.
 * 
 * @author Oskar Ojala
 */
public class TexCommandContainer {

    /**
     * Standard LaTeX commands.
     */
    public static final CommandEntry[] builtIn = {
            new CommandEntry("abstract", "\\abstract{}\n\nPrepares an abstract page.", 1),
            new CommandEntry("addcontentsline", "\\addcontentsline{}{}{}\n\nAdds a chapter title to the contents page.", 3),
            new CommandEntry("alph", "\\alph{counter}\n\nCauses the current value of counter to be printed in alphabetic lowercase characters, i.e., a, b, c... The \\Alph command causes upper case alphabetic characters, i.e., A, B, C...", 1),
            new CommandEntry("Alph", "\\Alph{counter}\n\nCauses the current value of counter to be printed in alphabetic uppercase characters, i.e., A, B, C... The \\alph command causes upper case alphabetic characters, i.e., a, b, c...", 1),
            new CommandEntry("appendix", "\\appendix\n\nChanges the way sectional units are numbered. The command generates no text and does not affect the numbering of parts.", 0),
            new CommandEntry("arabic", "\\arabic{counter}\n\nCauses the current value of counter to be printed in arabic numbers, i.e., 3.", 1),
            new CommandEntry("author", "\\author{name}\n\nDefines the author, used for output if \\maketitle is used.", 1),
            new CommandEntry("backslash", "\\backslash\n\nOutputs a backslash in math mode. (Enclose by two dollar symbols.)", 0),
            new CommandEntry("bf", "{\\bf text}\n\nOutputs the text in bold font.", 0),
            //new CommandEntry("bffamily", "{\\bffamily text}\n\nOutputs the text in bold font.", 0),
            new CommandEntry("bibitem", "\\bibitem{cite key}{item}\n\n\\bibitem requires two arguments for parsing. LaTeX syntax allows writing this as if it were two arguments, even though it is only one. This command is used within a \\thebibliography environment.", 2),
            new CommandEntry("bibliography", "\\bibliography{filenam}\n\nIncludes the bibliography at this point. The argument should be the name of the BibTeX file to use without the .bib -extension. Multiple files can be used by separating them with commas.", 1),
            new CommandEntry("bibliographystyle", "\\bibliographystyle{}\n\nDefines the style file to use for the bibliography generation with BibTeX", 1),
            new CommandEntry("bigskip", "\\bigskip\n\nEquivalent to \\vspace{\\bigskipamount} where \\bigskipamount is determined by the document style.", 0),
            new CommandEntry("caption", "\\caption{}\n\nUsed within a \\figure or \\table environment to specify a caption. May be followed by a \\label command.", 1),
            new CommandEntry("cdots", "\\cdots\n\nOutputs 3 dots.", 0),
            new CommandEntry("center", "\\center{text}\n\nCenters a block of text.", 1),
            new CommandEntry("centering", "\\centering\n\nThis declaration can be used inside an environment such as quote or in a parbox. The text of a figure or table can be centered on the page by putting a \\centering command at the beginning of the figure or table environment.", 0),
            new CommandEntry("centerline", "\\centerline{text}\n\nCenters a text line.", 1),
            new CommandEntry("chapter", "\\chapter{}\n\nOutputs a chapter heading. \\chapter* does not add a contents entry.", 1),
            new CommandEntry("cite", "\\cite{reference key}\n\nCites a reference. The reference key must be defined in a .bib file.", 1),
            new CommandEntry("comment", "\\comment{argument}\n\nAllows large comments in LaTeX files. The argument is ignored totally.", 1),
            new CommandEntry("date", "\\date{}\n\nSets the date of the document. Output by \\maketitle", 1),
            new CommandEntry("description", "\\description{}\n\nA list environment. Each \\item of the list should be followed by square-bracketed text, which will be highlighted.", 1),
            new CommandEntry("document", "\\document{}\n\nThis environment is used to enclose the body of a document.", 1),
            new CommandEntry("documentstyle", "\\documentstyle{style}\n\nSpecifies the main style of the document (report, article, ?). Use \\chapter in reports and \\section in articles.", 1),
            new CommandEntry("em", "{\\em text}\n\nEmphasizes (italic) text.", 0),
            new CommandEntry("emph", "\\emph{text}\n\nEmphasizes (italic) text.", 1),
            new CommandEntry("ensuremath", "\\ensuremath{text}\n\nThe argument of the \\ensuremath command is always set in math mode, regardless of the current mode. Note that math mode is not specifically invoked in the argument of the \\ensuremath command.\nLaTeX2e only.", 1),
            new CommandEntry("enumerate", "\\enumerate{}\n\nSets numbers to the \\items used in the list environment in question.", 1),
            //new CommandEntry("fffamily", "{\\fffamily argument}\n\nFormats the argument to teletype font.", 0),
            new CommandEntry("figure", "\\figure{}\n\nAn environment for figures. Allows interpretation of embedded caption commands as figures.", 1),
            new CommandEntry("flushleft", "\\flushleft{text}\n\nFlushes the text to the left margin.", 1),
            new CommandEntry("flushright", "\\flushright{text}\n\nFlushes the text to the right margin.", 1),
            new CommandEntry("fnsymbol", "\\fnsymbol{counter}\n\nCauses the current value of counter to be printed in a specific sequence of nine symbols that can be used for numbering footnotes. The sequence is asterisk, dagger, double dagger, section mark, paragraph mark, double vertical lines, double asterisks, double daggers, double double daggers.", 1),
            new CommandEntry("footnote", "\\footnote{}\n\nCreates a footnote to the end of the section.", 1),
            new CommandEntry("footnotemark", "\\footnotemark[number]\n\nPuts the footnote number in the text. It is used in conjunction with \\footnotetext to produce footnotes where \\footnote cannot be used.", 0),
            new CommandEntry("footnotetext", "\\footnotetext[number]{text}\n\nUsed in conjunction with the \\footnotemark command and places the text in the argument at the bottom of the page. This command can come anywhere after the \\footnotemark command.", 1),
            new CommandEntry("hline", "\\hline\n\nDraws a horizontal line below the current row within a \\tabular environment.", 0),
            new CommandEntry("hrule", "\\hrule\n\nDraws a horizontal line below the current paragraph.", 0),
            new CommandEntry("huge", "{\\huge text}\n\nOutputs the text in huge font.", 0),
            new CommandEntry("HUGE", "{\\HUGE text}\n\nOutputs the text in even more huge font than \\Huge", 0),
            new CommandEntry("Huge", "{\\Huge text}\n\nOutputs the text in even more huge font than \\huge", 0),
            new CommandEntry("hyphenation", "\\hyphenation{words}\n\nDeclares allowed hyphenation points, where words is a list of words, separated by spaces, in which each hyphenation point is indicated by a - character. For example, \\hyphenation{fortran,er-go-no-mic} indicates that 'fortran' cannot be hyphenated and indicates allowed hyphenation points for 'ergonomic'.\nThe argument to this command should contain words composed only of normal letters. To suggest hyphenation points for strings containing nonletters or accented letters, use the \\- command in the input text.\nThis command is normally given in the preamble.", 1),
            new CommandEntry("ldots", "\\ldots\n\nOutputs 3 dots.", 0),
            new CommandEntry("include", "\\include{filename}\n\nIncludes the specified file.", 1),
            new CommandEntry("index", "\\index{keyword}\n\nAdds a keyword to the keyword list of the current topic.", 1),
            new CommandEntry("input", "\\input{filename}\n\nIncludes the specified file.", 1),
            new CommandEntry("insertatlevel", "\\insertatlevel{level}{text}\n\nInserts the text into the given level of the document.", 2),
            new CommandEntry("it", "{\\it text}\n\nOutputs the text following it in italic.", 0),
            new CommandEntry("item", "\\item\n\nMarks an item of a list. Each \\item of the list may be followed by square-bracketed text, which will be highlighted.", 0),
            new CommandEntry("itemize", "\\itemize{}\n\nIndents each \\item of the list in question with a bullet.", 1),
            new CommandEntry("itemsep", "\\itemsep\n\nSpecifies the separator between list items.", 0),
            new CommandEntry("itshape", "\\itshape{text}\n\nOutputs the text in italic.", 1),
            new CommandEntry("label", "\\label{label}\n\nLabels the chapter, section, subsection or figure caption with the given label.", 1),
            new CommandEntry("large", "{\\large text}\n\nOutputs the text in large.", 0),
            new CommandEntry("Large", "{\\Large text}\n\nOutputs the text in even more large than \\large.", 0),
            new CommandEntry("LARGE", "{\\LARGE text}\n\nOutputs the text in even more large than \\Large.", 0),
            new CommandEntry("LaTeX", "\\LaTeX\n\nOutputs the LaTeX text in specific format.", 0),
            new CommandEntry("maketitle", "\\maketitle\n\nCreates the article or report title by combining the \\title, \\author and optionally \\date.", 0),
            new CommandEntry("marginpar", "\\marginpar{note}\n\nInserts a marginal note.", 1),
            new CommandEntry("marginpareven", "\\marginpareven{note}\n\nInserts a marginal note on even pages.", 1),
            new CommandEntry("marginparodd", "\\marginparodd{note}\n\nInserts a marginal note on odd pages.", 1),
            new CommandEntry("marginparwidth", "\\marginparwidth{}\n\nSpecifies the margin width to be used.", 1),
            new CommandEntry("mdseries", "\\mdseries{}\n\nStarts using a medium-weight font.", 1),
            new CommandEntry("multicolumn", "\\multicolumn{}{}{}\n\nUsed in \\tabular environment to denote a multicolumn cell.", 3),
            new CommandEntry("newcommand", "\newcommand\n\nDefines a new command. An optional argument between the two mandatory ones defines the number of arguments of the command. Arguments can be referred in the definition as #1, #2 etc.", 2),
            new CommandEntry("newpage", "\newpage\n\nInserts a page break.", 0),
            new CommandEntry("nocite", "\nocite{reference}\n\nUses this reference in the bibliography, but not in the text.", 1),
            new CommandEntry("noindent", "\noindent\n\nSets the paragraph indentation to zero.", 0),
            new CommandEntry("normalsize", "\normalsize{}\n\nSets the font size back to normal.", 1),
            new CommandEntry("onecolumn", "\\onecolumn\n\nSets the number of columns to one.", 0),
            new CommandEntry("overbrace", "\\overbrace{equation}\n\nGenerates a brace over equation. To \"label\" the overbrace, use a superscript.\nMath mode only.", 1),
            new CommandEntry("pagenumbering", "\\pagenumbering{argument}\n\nalph is [a,b,?], Alph is [A,B,?], arabic is [1,2,?], roman is [i,ii,?], Roman is [I,II,?]", 1),
            new CommandEntry("pageref", "\\pageref{label}\n\nCreates a page reference to the given label.", 1),
            new CommandEntry("pagestyle", "\\pagestyle{option}\n\nChanges the style from the current page on throughout the remainder of your document. Valid options are plain, empty, headings and myheadings.", 1),
            new CommandEntry("par", "\\par\n\nEnds paragraph.", 0),
            new CommandEntry("part", "\\part{title}\n\nStarts a new part of a book.", 1),
            new CommandEntry("paragraph", "\\paragraph\n\nStarts a new paragraph with the specified heading.\nEssentially a lower level sectioning command than subsubsection.", 0),
            new CommandEntry("parindent", "\\parindent{indentation}\n\nIndents the first line of the next paragraphs by the specified amount.", 1),
            new CommandEntry("parskip", "\\parskip{}\n\nChanges the spacing between paragraphs.", 1),
            new CommandEntry("printindex", "\\printindex\n\nInserts an index.", 0),
            new CommandEntry("protect", "\\protect\n\nA fragile command that appears in a moving argument must be preceded by a \\protect command. The \\protect applies only to the immediately following command; if arguments of this command also contain fragile commands, the latter must be protected with their own \\protect.", 0),
            new CommandEntry("providecommand", "\\providecommand\n\nDefines a new command if the command has not been defined previously. An optional argument between the two mandatory ones defines the number of arguments of the command. Arguments can be referred in the definition as #1, #2 etc.", 2),
            new CommandEntry("put", "\\put(x-coord,y-coord){object}\n\nPlaces the object specified by the mandatory argument at the given coordinates. The coordinates are in units of \\unitlength. Some of the object items you might place are \\line, \\vector, \\circle and \\oval.", 1),
            new CommandEntry("quotation", "\\quotation{quotation}\n\nIndents a long quotation.", 1),
            new CommandEntry("quote", "\\quote{quotation}\n\nIndents a short quotation.", 1),
            new CommandEntry("ref", "\\ref{}\n\nRefers to a \\label and causes the number of that section or figure to be printed.", 1),
            new CommandEntry("renewcommand", "\\renewcommand\n\nRedefines a command. An optional argument between the two mandatory ones defines the number of arguments of the command. Arguments can be referred in the definition as #1, #2 etc.", 2),
            new CommandEntry("rm", "{\\rm argument}\n\nFormats the argument to a plain roman font.", 0),
            new CommandEntry("rmfamily", "{\\rmfamily argument}\n\nFormats the argument to a plain roman font.", 0),
            new CommandEntry("roman", "\\roman{counter}\n\nCauses the current value of counter to be printed in lower case roman numerals, i.e., i, ii, iii, ... The \\Roman command causes upper case roman numerals, i.e., I, II, III...", 1),
            new CommandEntry("Roman", "\\Roman{counter}\n\nCauses the current value of counter to be printed in upper case roman numerals, i.e., I, II, III, ... The \\roman command causes upper case roman numerals, i.e., i, ii, iii...", 1),
            new CommandEntry("sc", "{\\sc text}\n\nUses small capitals for the output print.", 0),
            new CommandEntry("scshape", "{\\scshape srgument}\n\nUses small capitals for the output print.", 0),
            new CommandEntry("section", "\\section{heading}\n\nStarts a new section with the given heading.", 1),
            new CommandEntry("sf", "{\\sf argument}\n\nFormats the argument to sans-serif font.", 0),
            new CommandEntry("sffamily", "{\\sffamily argument}\n\nFormats the argument to sans-serif font.", 0),
            new CommandEntry("shortcite", "\\shortcite{reference key}\n\nCites a reference. The reference key must be defined in a .bib file.", 1),
            new CommandEntry("sl", "{\\sl text}\n\nOutputs slanted text.", 0),
            new CommandEntry("slshape", "{\\slshape text}\n\nOutputs slanted text.", 0),
            new CommandEntry("small", "{\\small argument}\n\nOutputs the argument in a small font.", 0),
            new CommandEntry("special", "\\special{argument}\n\nThe argument is printed on the output file as it is without processing.", 1),
            new CommandEntry("ss", "\\ss\n\nOutputs the German sharp double S.", 0),
            new CommandEntry("subparagraph", "\\subparagraph{heading}\n\nStarts a new subparagraph with the given heading.", 1),
            new CommandEntry("subsection", "\\subsection{heading}\n\nStarts a new subsection with the given heading.", 1),
            new CommandEntry("subsubsection", "\\subsubsection{heading}\n\nStarts a new subsubsection with the given heading.", 1),
            new CommandEntry("tabbing", "\\tabbing{}\n\nInitializes tabbing environment.", 1),
            new CommandEntry("table", "\\table{}\n\nInitializes an environment for tables.", 1),
            new CommandEntry("tableofcontents", "\\tableofcontents\n\nInserts a table of contents to the current location.", 0),
            new CommandEntry("tabular", "\\tabular{}{}\n\nTabular environment: The first argument specifies the column formatting. a pipe symbol (|) denotes a vertical border, one of l, r, c signifies a normal column of default width, and p followed by a dimension specifies a column of given width.", 2),
            new CommandEntry("TeX", "\\TeX\n\nOutputs the TeX text in specific format.", 0),
            new CommandEntry("textbackslash", "\\textbackslash\n\nOutputs a backslash", 0),
            new CommandEntry("textbf", "\\textbf{text}\n\nOutputs the text in bold font.", 1),
            new CommandEntry("textit", "\\textit{text}\n\nOutputs the text in italic.", 1),
            new CommandEntry("textrm", "\\textrm{argument}\n\nFormats the argument to a plain roman font.", 1),
            new CommandEntry("textsc", "\\textsc{text}\n\nUses small capitals for the output print.", 1),
            new CommandEntry("textsf", "\\textsf{argument}\n\nFormats the argument to sans-serif font.", 1),
            new CommandEntry("textsl", "\\textsl{text}\n\nOutputs slanted text.", 1),
            new CommandEntry("texttt", "\\texttt{argument}\n\nFormats the argument to teletype font.", 1),
            new CommandEntry("textwidth", "\\textwidth<width>\n\nSets the text width.", 0),
            new CommandEntry("thebibliography", "\\thebibliography{}\n\nAn environment for specifying the bibliography as a series of \\bibitem commands.", 1),
            new CommandEntry("thispagestyle", "\\thispagestyle{option}\n\nWorks in the same manner as the \\pagestyle command except that it changes the style for the current page only.", 1),
            new CommandEntry("tiny", "{\\tiny argument}\n\nFormats the argument to very small font.", 0),
            new CommandEntry("title", "\\title{title}\n\nSets the title to be used with the \\maketitle command.", 1),
            new CommandEntry("today", "\\today\n\nPrints today's date.", 0),
            new CommandEntry("tt", "{\\tt argument}\n\nFormats the argument to teletype font.", 0),
            new CommandEntry("twocolumn", "\\twocolumn\n\nStarts a new page and produces two-column output.", 0),
            new CommandEntry("underbrace", "\\underbrace{equation}\n\nGenerates a brace below equation. To \"label\" the underbrace, use a subscript.\nMath mode only.", 1),
            new CommandEntry("underline", "\\underline{argument}\n\nUnderlines the given argument.", 1),
            new CommandEntry("upshape", "{\\upshape argument}\n\nChanges to an upright font.", 0),
            new CommandEntry("usepackage", "\\usepackage\n\nLoads a package into use (use this in the preamble)", 1),
            new CommandEntry("verb", "\\verb{argument}\n\nFormats the given small amount of text to a fixed-width font without interpreting any LaTeX commands.", 1),
            new CommandEntry("verbatim", "\\verbatim{argument}\n\nFormats the given argument to a fixed-width font without interpreting any LaTeX commands.", 1),
            new CommandEntry("verbatiminput", "\\verbatiminput{filename}\n\nInclude the given file as if it were within a \\verbatim environment.", 1),
            new CommandEntry("vspace", "\\vspace[*]{length}\n\nAdds vertical space. The length of the space can be expressed in any terms that LaTeX understands, i.e., points, inches, etc. You can add negative as well as positive space with a \\vspace command.\nIf \\vspace appears in the middle of a paragraph the space is added after the line containing it.\nLaTeX removes vertical space that comes at the end of a page. If you don't want LaTeX to remove this space, include the optional * argument. Then the space is never removed.", 1)

//            new CommandEntry("LaTeX", "\\LaTeX\n\nWrites out the LaTeX logo", 0),
//            new CommandEntry("TeX", "\\TeX\n\nWrites out the TeX logo", 0),
//            new CommandEntry("newcommand", "\\newcommand{cmd}[args][opt]{def}", 2),
//            new CommandEntry("section", "\\section[content]{heading}\n\nStarts a new section", 1),
//            new CommandEntry("subsection", "\\subsection[content]{heading}\n\nStarts a new subsection", 1),
//            new CommandEntry("subsubsection", "\\subsubsection[content]{heading}\n\nStarts a new subsubsection", 1)
            };

    private HashMap commandHash;
    private CommandEntry[] sortedCommands;
    private int size;
    
    /**
     * Constructs a new command container
     */
    public TexCommandContainer() {
        commandHash = new HashMap(4);
        sortedCommands = builtIn;
        size = 0;
    }

    /**
     * Adds a new reference source (tex file) into this container or
     * updates an existing one.
     * 
     * @param key The name of the reference source (filename)
     * @param refs The commands to insert
     */
    public void addRefSource(String key, ArrayList refs) {
        size += refs.size();
        ArrayList al = (ArrayList) commandHash.put(key, refs);
        if (al != null)
            size -= al.size();
    }

    /**
     * Organizes this container's contents into the sorted command array.
     * The added reference sources' commands are added to the built-in commands
     * and sorted.
     */
    public void organize() {
        if (commandHash.size() == 0)
            return;
        ArrayList allRefs = new ArrayList(size);
        if (commandHash.size() > 1) {
            for (Iterator iter = commandHash.values().iterator(); iter.hasNext();) {
                ArrayList refList = (ArrayList) iter.next();
                allRefs.addAll(refList);
            }
        } else if (commandHash.size() == 1) {
            Iterator iter = commandHash.values().iterator();
            allRefs = (ArrayList) iter.next();
        }
        
        sortedCommands = new CommandEntry[allRefs.size() + builtIn.length];
        allRefs.toArray(sortedCommands);
        System.arraycopy(builtIn, 0, sortedCommands, allRefs.size(), builtIn.length);
        Arrays.sort(sortedCommands);
    }
    
    /**
     * @return Returns the sortedCommands.
     */
    public CommandEntry[] getSortedCommands() {
        return sortedCommands;
    }
}
