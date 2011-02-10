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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Contains the LaTeX commands that can be completed.
 * 
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class TexCommandContainer {

    /**
     * Standard LaTeX commands.
     */
    public static final TexCommandEntry[] builtIn = {
            new TexCommandEntry("abstract", "\\abstract{}\n\nPrepares an abstract page.", 1),
            new TexCommandEntry("addcontentsline", "\\addcontentsline{}{}{}\n\nAdds a chapter title to the contents page.", 3),
            new TexCommandEntry("alph", "\\alph{counter}\n\nCauses the current value of counter to be printed in alphabetic lowercase characters, i.e., a, b, c... The \\Alph command causes upper case alphabetic characters, i.e., A, B, C...", 1),
            new TexCommandEntry("Alph", "\\Alph{counter}\n\nCauses the current value of counter to be printed in alphabetic uppercase characters, i.e., A, B, C... The \\alph command causes upper case alphabetic characters, i.e., a, b, c...", 1),
            new TexCommandEntry("appendix", "\\appendix\n\nChanges the way sectional units are numbered. The command generates no text and does not affect the numbering of parts.", 0),
            new TexCommandEntry("arabic", "\\arabic{counter}\n\nCauses the current value of counter to be printed in arabic numbers, i.e., 3.", 1),
            new TexCommandEntry("author", "\\author{name}\n\nDefines the author, used for output if \\maketitle is used.", 1),
            new TexCommandEntry("backslash", "\\backslash\n\nOutputs a backslash in math mode. (Enclose by two dollar symbols.)", 0),
            new TexCommandEntry("begin", "\\begin{envname}\n\nStart of an environment, must end with \\end{envname}.", 1),
            new TexCommandEntry("bfseries", "\\bfseries\n\nSwitch to a bold font.", 0),
            new TexCommandEntry("bibitem", "\\bibitem{cite key}{item}\n\n\\bibitem requires two arguments for parsing. LaTeX syntax allows writing this as if it were two arguments, even though it is only one. This command is used within a \\thebibliography environment.", 2),
            new TexCommandEntry("bibliography", "\\bibliography{filename}\n\nIncludes the bibliography at this point. The argument should be the name of the BibTeX file to use without the .bib -extension. Multiple files can be used by separating them with commas.", 1),
            new TexCommandEntry("bibliographystyle", "\\bibliographystyle{}\n\nDefines the style file to use for the bibliography generation with BibTeX", 1),
            new TexCommandEntry("bigskip", "\\bigskip\n\nEquivalent to \\vspace{\\bigskipamount} where \\bigskipamount is determined by the document style.", 0),
            new TexCommandEntry("caption", "\\caption{}\n\nUsed within a \\figure or \\table environment to specify a caption. May be followed by a \\label command.", 1),
            new TexCommandEntry("cdots", "\\cdots\n\nOutputs 3 dots.", 0),
            new TexCommandEntry("center", "\\center{text}\n\nCenters a block of text.", 1),
            new TexCommandEntry("centering", "\\centering\n\nThis declaration can be used inside an environment such as quote or in a parbox. The text of a figure or table can be centered on the page by putting a \\centering command at the beginning of the figure or table environment.", 0),
            new TexCommandEntry("centerline", "\\centerline{text}\n\nCenters a text line.\nThis is a TeX Command, better use {\\centering text}.", 1),
            new TexCommandEntry("chapter", "\\chapter{}\n\nOutputs a chapter heading. \\chapter* does not add a contents entry.", 1),
            new TexCommandEntry("cite", "\\cite{reference key}\n\nCites a reference. The reference key must be defined in a .bib file.", 1),
            new TexCommandEntry("comment", "\\comment{argument}\n\nAllows large comments in LaTeX files. The argument is ignored totally.", 1),
            new TexCommandEntry("date", "\\date{}\n\nSets the date of the document. Output by \\maketitle", 1),
            new TexCommandEntry("description", "\\description{}\n\nA list environment. Each \\item of the list should be followed by square-bracketed text, which will be highlighted.", 1),
            new TexCommandEntry("documentclass", "\\documentclass{class}\n\nSpecifies the main style of the document (report, article, ?). Use \\chapter in reports and \\section in articles.", 1),
            new TexCommandEntry("em", "\\em\n\nSwitch to a emphasize (italic) font.", 0),
            new TexCommandEntry("emph", "\\emph{text}\n\nEmphasizes (italic) text.", 1),
            new TexCommandEntry("end", "\\end{envname}\n\nEnd of an environment which was started by \\begin{envname}.", 1),
            new TexCommandEntry("ensuremath", "\\ensuremath{text}\n\nThe argument of the \\ensuremath command is always set in math mode, regardless of the current mode. Note that math mode is not specifically invoked in the argument of the \\ensuremath command.\nLaTeX2e only.", 1),
            new TexCommandEntry("enumerate", "\\enumerate{}\n\nSets numbers to the \\items used in the list environment in question.", 1),
            new TexCommandEntry("figure", "\\figure{}\n\nAn environment for figures. Allows interpretation of embedded caption commands as figures.", 1),
            new TexCommandEntry("flushleft", "\\flushleft{text}\n\nFlushes the text to the left margin.", 1),
            new TexCommandEntry("flushright", "\\flushright{text}\n\nFlushes the text to the right margin.", 1),
            new TexCommandEntry("fnsymbol", "\\fnsymbol{counter}\n\nCauses the current value of counter to be printed in a specific sequence of nine symbols that can be used for numbering footnotes. The sequence is asterisk, dagger, double dagger, section mark, paragraph mark, double vertical lines, double asterisks, double daggers, double double daggers.", 1),
            new TexCommandEntry("footnote", "\\footnote{}\n\nCreates a footnote to the end of the section.", 1),
            new TexCommandEntry("footnotemark", "\\footnotemark[number]\n\nPuts the footnote number in the text. It is used in conjunction with \\footnotetext to produce footnotes where \\footnote cannot be used.", 0),
            new TexCommandEntry("footnotetext", "\\footnotetext[number]{text}\n\nUsed in conjunction with the \\footnotemark command and places the text in the argument at the bottom of the page. This command can come anywhere after the \\footnotemark command.", 1),
            new TexCommandEntry("hline", "\\hline\n\nDraws a horizontal line below the current row within a \\tabular environment.", 0),
            new TexCommandEntry("hrule", "\\hrule\n\nDraws a horizontal line below the current paragraph.", 0),
            new TexCommandEntry("huge", "{\\huge text}\n\nOutputs the text in huge font.", 0),
            new TexCommandEntry("Huge", "{\\Huge text}\n\nOutputs the text in even more huge font than \\huge", 0),
            new TexCommandEntry("hyphenation", "\\hyphenation{words}\n\nDeclares allowed hyphenation points, where words is a list of words, separated by spaces, in which each hyphenation point is indicated by a - character. For example, \\hyphenation{fortran,er-go-no-mic} indicates that 'fortran' cannot be hyphenated and indicates allowed hyphenation points for 'ergonomic'.\nThe argument to this command should contain words composed only of normal letters. To suggest hyphenation points for strings containing nonletters or accented letters, use the \\- command in the input text.\nThis command is normally given in the preamble.", 1),
            new TexCommandEntry("ldots", "\\ldots\n\nOutputs 3 dots.", 0),
            new TexCommandEntry("include", "\\include{filename}\n\nIncludes the specified file.", 1),
            new TexCommandEntry("index", "\\index{keyword}\n\nAdds a keyword to the keyword list of the current topic.", 1),
            new TexCommandEntry("input", "\\input{filename}\n\nIncludes the specified file.", 1),
            new TexCommandEntry("insertatlevel", "\\insertatlevel{level}{text}\n\nInserts the text into the given level of the document.", 2),
            new TexCommandEntry("item", "\\item\n\nMarks an item of a list. Each \\item of the list may be followed by square-bracketed text, which will be highlighted.", 0),
            new TexCommandEntry("itemize", "\\itemize{}\n\nIndents each \\item of the list in question with a bullet.", 1),
            new TexCommandEntry("itemsep", "\\itemsep\n\nSpecifies the separator between list items.", 0),
            new TexCommandEntry("itshape", "\\itshape\n\nSwitch to a italic font.", 0),
            new TexCommandEntry("label", "\\label{label}\n\nLabels the chapter, section, subsection or figure caption with the given label.", 1),
            new TexCommandEntry("large", "{\\large text}\n\nOutputs the text in large.", 0),
            new TexCommandEntry("linebreak", "\\linebreak[number]\n\n The \\linebreak command tells LaTeX to break the current line at the point of the command. With the optional argument, number, you can convert the \\linebreak command from a demand to a request. The number must be a number from 0 to 4. The higher the number, the more insistent the request is.\nThe \\linebreak command causes LaTeX to stretch the line so it extends to the right margin. ", 0),
            new TexCommandEntry("Large", "{\\Large text}\n\nOutputs the text in even more large than \\large.", 0),
            new TexCommandEntry("LARGE", "{\\LARGE text}\n\nOutputs the text in even more large than \\Large.", 0),
            new TexCommandEntry("LaTeX", "\\LaTeX\n\nOutputs the LaTeX text in specific format.", 0),
            new TexCommandEntry("maketitle", "\\maketitle\n\nCreates the article or report title by combining the \\title, \\author and optionally \\date.", 0),
            new TexCommandEntry("marginpar", "\\marginpar{note}\n\nInserts a marginal note.", 1),
            new TexCommandEntry("marginpareven", "\\marginpareven{note}\n\nInserts a marginal note on even pages.", 1),
            new TexCommandEntry("marginparodd", "\\marginparodd{note}\n\nInserts a marginal note on odd pages.", 1),
            new TexCommandEntry("marginparwidth", "\\marginparwidth{}\n\nSpecifies the margin width to be used.", 1),
            new TexCommandEntry("mdseries", "\\mdseries\n\nStarts using a medium-weight font.", 0),
            new TexCommandEntry("multicolumn", "\\multicolumn{}{}{}\n\nUsed in \\tabular environment to denote a multicolumn cell.", 3),
            new TexCommandEntry("newcommand", "\\newcommand\n\nDefines a new command. An optional argument between the two mandatory ones defines the number of arguments of the command. Arguments can be referred in the definition as #1, #2 etc.", 2),
            new TexCommandEntry("newenvironment", "\\newenvironment{nam}[args]{begdef}{enddef}\n\n These command define an environment.\n\n* nam: The name of the environment. For \\newenvironment there must be no currently defined environment by that name, and the command \\nam must be undefined. For \\renewenvironment the environment must already be defined.\n* args: An integer from 1 to 9 denoting the number of arguments of the newly-defined environment. The default is no arguments.\n* begdef: The text substituted for every occurrence of \\begin{name};  a parameter of the form #n in cmd is replaced by the text of the nth argument when this substitution takes place.\n* enddef: The text substituted for every occurrence of \\end{nam}. It may not contain any argument parameters. ", 3),            
            new TexCommandEntry("newline", "\\newline\n\nThe \\newline command breaks the line right where it is. The \\newline command can be used only in paragraph mode.", 0),
            new TexCommandEntry("newpage", "\\newpage\n\nInserts a page break.", 0),
            new TexCommandEntry("nocite", "\\nocite{reference}\n\nUses this reference in the bibliography, but not in the text.", 1),
            new TexCommandEntry("noindent", "\\noindent\n\nSets the paragraph indentation to zero.", 0),
            new TexCommandEntry("normalsize", "\\normalsize{}\n\nSets the font size back to normal.", 1),
            new TexCommandEntry("onecolumn", "\\onecolumn\n\nSets the number of columns to one.", 0),
            new TexCommandEntry("pagenumbering", "\\pagenumbering{argument}\n\nalph is [a,b,?], Alph is [A,B,?], arabic is [1,2,?], roman is [i,ii,?], Roman is [I,II,?]", 1),
            new TexCommandEntry("pageref", "\\pageref{label}\n\nCreates a page reference to the given label.", 1),
            new TexCommandEntry("pagestyle", "\\pagestyle{option}\n\nChanges the style from the current page on throughout the remainder of your document. Valid options are plain, empty, headings and myheadings.", 1),
            new TexCommandEntry("par", "\\par\n\nEnds paragraph.", 0),
            new TexCommandEntry("part", "\\part{title}\n\nStarts a new part of a book.", 1),
            new TexCommandEntry("paragraph", "\\paragraph\n\nStarts a new paragraph with the specified heading.\nEssentially a lower level sectioning command than subsubsection.", 0),
            new TexCommandEntry("parindent", "\\parindent{indentation}\n\nIndents the first line of the next paragraphs by the specified amount.", 1),
            new TexCommandEntry("parskip", "\\parskip{}\n\nChanges the spacing between paragraphs.", 1),
            new TexCommandEntry("printindex", "\\printindex\n\nInserts an index.", 0),
            new TexCommandEntry("protect", "\\protect\n\nA fragile command that appears in a moving argument must be preceded by a \\protect command. The \\protect applies only to the immediately following command; if arguments of this command also contain fragile commands, the latter must be protected with their own \\protect.", 0),
            new TexCommandEntry("providecommand", "\\providecommand\n\nDefines a new command if the command has not been defined previously. An optional argument between the two mandatory ones defines the number of arguments of the command. Arguments can be referred in the definition as #1, #2 etc.", 2),
            new TexCommandEntry("put", "\\put(x-coord,y-coord){object}\n\nPlaces the object specified by the mandatory argument at the given coordinates. The coordinates are in units of \\unitlength. Some of the object items you might place are \\line, \\vector, \\circle and \\oval.", 1),
            new TexCommandEntry("quotation", "\\quotation{quotation}\n\nIndents a long quotation.", 1),
            new TexCommandEntry("quote", "\\quote{quotation}\n\nIndents a short quotation.", 1),
            new TexCommandEntry("ref", "\\ref{}\n\nRefers to a \\label and causes the number of that section or figure to be printed.", 1),
            new TexCommandEntry("renewcommand", "\\renewcommand\n\nRedefines a command. An optional argument between the two mandatory ones defines the number of arguments of the command. Arguments can be referred in the definition as #1, #2 etc.", 2),
            new TexCommandEntry("renewenvironment", "\\renewenvironment{nam}[args]{begdef}{enddef}\n\n These command redefine an environment.\n\n* nam: The name of the environment. For \\newenvironment there must be no currently defined environment by that name, and the command \\nam must be undefined. For \\renewenvironment the environment must already be defined.\n* args: An integer from 1 to 9 denoting the number of arguments of the newly-defined environment. The default is no arguments.\n* begdef: The text substituted for every occurrence of \\begin{name};  a parameter of the form #n in cmd is replaced by the text of the nth argument when this substitution takes place.\n* enddef: The text substituted for every occurrence of \\end{nam}. It may not contain any argument parameters. ", 3),            
            new TexCommandEntry("rmfamily", "\\rmfamily\n\nSwitch to a plain roman font.", 0),
            new TexCommandEntry("roman", "\\roman{counter}\n\nCauses the current value of counter to be printed in lower case roman numerals, i.e., i, ii, iii, ... The \\Roman command causes upper case roman numerals, i.e., I, II, III...", 1),
            new TexCommandEntry("Roman", "\\Roman{counter}\n\nCauses the current value of counter to be printed in upper case roman numerals, i.e., I, II, III, ... The \\roman command causes upper case roman numerals, i.e., i, ii, iii...", 1),
            new TexCommandEntry("scshape", "\\scshape\n\nUses small capitals for the output print.", 0),
            new TexCommandEntry("section", "\\section{heading}\n\nStarts a new section with the given heading.", 1),
            new TexCommandEntry("sffamily", "\\sffamily\n\nSwitch to a sans-serif font.", 0),
            new TexCommandEntry("shortcite", "\\shortcite{reference key}\n\nCites a reference. The reference key must be defined in a .bib file.", 1),
            new TexCommandEntry("slshape", "\\slshape\n\nSwitch to a slanted font.", 0),
            new TexCommandEntry("small", "{\\small argument}\n\nOutputs the argument in a small font.", 0),
            new TexCommandEntry("special", "\\special{argument}\n\nThe argument is printed on the output file as it is without processing.", 1),
            new TexCommandEntry("ss", "\\ss\n\nOutputs the German sharp double S.", 0),
            new TexCommandEntry("subparagraph", "\\subparagraph{heading}\n\nStarts a new subparagraph with the given heading.", 1),
            new TexCommandEntry("subsection", "\\subsection{heading}\n\nStarts a new subsection with the given heading.", 1),
            new TexCommandEntry("subsubsection", "\\subsubsection{heading}\n\nStarts a new subsubsection with the given heading.", 1),
            new TexCommandEntry("tabbing", "\\tabbing{}\n\nInitializes tabbing environment.", 1),
            new TexCommandEntry("table", "\\table{}\n\nInitializes an environment for tables.", 1),
            new TexCommandEntry("tableofcontents", "\\tableofcontents\n\nInserts a table of contents to the current location.", 0),
            new TexCommandEntry("tabular", "\\tabular{}{}\n\nTabular environment: The first argument specifies the column formatting. a pipe symbol (|) denotes a vertical border, one of l, r, c signifies a normal column of default width, and p followed by a dimension specifies a column of given width.", 2),
            new TexCommandEntry("TeX", "\\TeX\n\nOutputs the TeX text in specific format.", 0),
            new TexCommandEntry("textbackslash", "\\textbackslash\n\nOutputs a backslash", 0),
            new TexCommandEntry("textbf", "\\textbf{text}\n\nOutputs the text in bold font.", 1),
            new TexCommandEntry("textit", "\\textit{text}\n\nOutputs the text in italic.", 1),
            new TexCommandEntry("textmd", "\\textmd{text}\n\nOutputs the text in a medium weight font.", 1),
            new TexCommandEntry("textnormal", "\\textnormal{text}\n\nOutputs the text in the standard document font.", 1),
            new TexCommandEntry("textrm", "\\textrm{argument}\n\nFormats the argument to a plain roman font.", 1),
            new TexCommandEntry("textsc", "\\textsc{text}\n\nUses small capitals for the output print.", 1),
            new TexCommandEntry("textsf", "\\textsf{argument}\n\nFormats the argument to sans-serif font.", 1),
            new TexCommandEntry("textsl", "\\textsl{text}\n\nOutputs slanted text.", 1),
            new TexCommandEntry("texttt", "\\texttt{argument}\n\nFormats the argument to teletype font.", 1),
            new TexCommandEntry("textup", "\\textup{argument}\n\nPrints the argument in an upright font.", 1),
            new TexCommandEntry("textwidth", "\\textwidth<width>\n\nSets the text width.", 0),
            new TexCommandEntry("thebibliography", "\\thebibliography{}\n\nAn environment for specifying the bibliography as a series of \\bibitem commands.", 1),
            new TexCommandEntry("thispagestyle", "\\thispagestyle{option}\n\nWorks in the same manner as the \\pagestyle command except that it changes the style for the current page only.", 1),
            new TexCommandEntry("tiny", "{\\tiny argument}\n\nFormats the argument to very small font.", 0),
            new TexCommandEntry("title", "\\title{title}\n\nSets the title to be used with the \\maketitle command.", 1),
            new TexCommandEntry("today", "\\today\n\nPrints today's date.", 0),
            new TexCommandEntry("ttfamily", "\\ttfamily\n\nSwitch to a teletype font.", 0),
            new TexCommandEntry("twocolumn", "\\twocolumn\n\nStarts a new page and produces two-column output.", 0),
            new TexCommandEntry("underline", "\\underline{argument}\n\nUnderlines the given argument.", 1),
            new TexCommandEntry("upshape", "{\\upshape argument}\n\nChanges to an upright font.", 0),
            new TexCommandEntry("usepackage", "\\usepackage\n\nLoads a package into use (use this in the preamble)", 1),
            new TexCommandEntry("verb", "\\verb{argument}\n\nFormats the given small amount of text to a fixed-width font without interpreting any LaTeX commands.", 1),
            new TexCommandEntry("verbatim", "\\verbatim{argument}\n\nFormats the given argument to a fixed-width font without interpreting any LaTeX commands.", 1),
            new TexCommandEntry("verbatiminput", "\\verbatiminput{filename}\n\nInclude the given file as if it were within a \\verbatim environment.", 1),
            new TexCommandEntry("vspace", "\\vspace[*]{length}\n\nAdds vertical space. The length of the space can be expressed in any terms that LaTeX understands, i.e., points, inches, etc. You can add negative as well as positive space with a \\vspace command.\nIf \\vspace appears in the middle of a paragraph the space is added after the line containing it.\nLaTeX removes vertical space that comes at the end of a page. If you don't want LaTeX to remove this space, include the optional * argument. Then the space is never removed.", 1)

            };

    public static final TexCommandEntry[] greekSmall = {
    	new TexCommandEntry("alpha", "\\alpha\n\nPrints a small greek alpha", "mathSym/greek/alpha"),
    	new TexCommandEntry("beta", "\\beta\n\nPrints a small greek beta", "mathSym/greek/beta"),
    	new TexCommandEntry("gamma", "\\gamma\n\nPrints a small greek gamma", "mathSym/greek/gamma"),
    	new TexCommandEntry("delta", "\\delta\n\nPrints a small greek delta", "mathSym/greek/delta"),
    	new TexCommandEntry("epsilon", "\\epsilon\n\nPrints a small greek epsilon", "mathSym/greek/epsilon"),
    	new TexCommandEntry("varepsilon", "\\varepsilon\n\nPrints a small greek epsilon", "mathSym/greek/varepsilon"),
    	new TexCommandEntry("zeta", "\\zeta\n\nPrints a small greek zeta", "mathSym/greek/zeta"),
    	new TexCommandEntry("eta", "\\eta\n\nPrints a small greek eta", "mathSym/greek/eta"),
    	new TexCommandEntry("theta", "\\theta\n\nPrints a small greek theta", "mathSym/greek/theta"),
    	new TexCommandEntry("vartheta", "\\vartheta\n\nPrints a small greek theta", "mathSym/greek/vartheta"),
    	new TexCommandEntry("iota", "\\iota\n\nPrints a small greek iota", "mathSym/greek/iota"),
    	new TexCommandEntry("kappa", "\\kappa\n\nPrints a small greek kappa", "mathSym/greek/kappa"),
    	new TexCommandEntry("lambda", "\\lambda\n\nPrints a small greek lambda", "mathSym/greek/lambda"),
    	new TexCommandEntry("mu", "\\mu\n\nPrints a small greek mu", "mathSym/greek/mu"),
    	new TexCommandEntry("nu", "\\nu\n\nPrints a small greek nu", "mathSym/greek/nu"),
    	new TexCommandEntry("xi", "\\xi\n\nPrints a small greek xi", "mathSym/greek/xi"),
    	new TexCommandEntry("pi", "\\pi\n\nPrints a small greek pi", "mathSym/greek/pi"),
    	new TexCommandEntry("varpi", "\\varpi\n\nPrints a small greek pi", "mathSym/greek/varpi"),
    	new TexCommandEntry("rho", "\\rho\n\nPrints a small greek rho", "mathSym/greek/rho"),
    	new TexCommandEntry("varrho", "\\varrho\n\nPrints a small greek rho", "mathSym/greek/varrho"),
    	new TexCommandEntry("sigma", "\\sigma\n\nPrints a small greek sigma", "mathSym/greek/sigma"),
    	new TexCommandEntry("varsigma", "\\varsigma\n\nPrints a small greek sigma", "mathSym/greek/varsigma"),
    	new TexCommandEntry("tau", "\\tau\n\nPrints a small greek tau", "mathSym/greek/tau"),
    	new TexCommandEntry("upsilon", "\\upsilon\n\nPrints a small greek upsilon", "mathSym/greek/upsilon"),
    	new TexCommandEntry("phi", "\\phi\n\nPrints a small greek phi", "mathSym/greek/phi"),
    	new TexCommandEntry("varphi", "\\varphi\n\nPrints a small greek phi", "mathSym/greek/varphi"),
    	new TexCommandEntry("chi", "\\chi\n\nPrints a small greek chi", "mathSym/greek/chi"),
    	new TexCommandEntry("psi", "\\psi\n\nPrints a small greek psi", "mathSym/greek/psi"),
    	new TexCommandEntry("omega", "\\omega\n\nPrints a small greek omega", "mathSym/greek/omega")
    };
    
    public static final TexCommandEntry[] greekCapital = {
    	new TexCommandEntry("Gamma", "\\Gamma\n\nPrints a capital greek gamma", "mathSym/greek/cgamma"),
    	new TexCommandEntry("Delta", "\\Delta\n\nPrints a capital greek delta", "mathSym/greek/cdelta"),
    	new TexCommandEntry("Theta", "\\Theta\n\nPrints a capital greek theta", "mathSym/greek/ctheta"),
    	new TexCommandEntry("Lambda", "\\Lambda\n\nPrints a capital greek lambda", "mathSym/greek/clambda"),
    	new TexCommandEntry("Xi", "\\Xi\n\nPrints a capital greek xi", "mathSym/greek/cxi"),
    	new TexCommandEntry("Pi", "\\Pi\n\nPrints a capital greek pi", "mathSym/greek/cpi"),
    	new TexCommandEntry("Sigma", "\\Sigma\n\nPrints a capital greek sigma", "mathSym/greek/csigma"),
    	new TexCommandEntry("Upsilon", "\\Upsilon\n\nPrints a capital greek upsilon", "mathSym/greek/cupsilon"),
    	new TexCommandEntry("Phi", "\\Phi\n\nPrints a capital greek phi", "mathSym/greek/cphi"),
    	new TexCommandEntry("Psi", "\\Psi\n\nPrints a capital greek psi", "mathSym/greek/cpsi"),
    	new TexCommandEntry("Omega", "\\Omega\n\nPrints a capital greek omega", "mathSym/greek/comega")
    };

    public static final TexCommandEntry[] miscMath = {
        new TexCommandEntry("label", "\\label{label}\n\nLabels the current equation line with the given label.", 1, TexCommandEntry.MATH_CONTEXT),
        new TexCommandEntry("nonumber", "\\nonumber\n\nThe preceding formula in an eqnarray environment gets no number.", 0, TexCommandEntry.MATH_CONTEXT),    	
        new TexCommandEntry("right", "\\right\n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
        new TexCommandEntry("left", "\\left\n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
        new TexCommandEntry("frac", "\\frac{numerator}{denominator}\n\nPrints a fraction", 2, TexCommandEntry.MATH_CONTEXT),    	
        new TexCommandEntry("sqrt", "\\sqrt{}\n\nSquare root", 1, TexCommandEntry.MATH_CONTEXT),
        new TexCommandEntry("backslash", "\\backslash\n\nBackslash", 0, TexCommandEntry.MATH_CONTEXT),
        new TexCommandEntry("not", "\\not\n\nCancels symbol", 0, TexCommandEntry.MATH_CONTEXT),
    	new TexCommandEntry("forall", "\\forall\n\nfor all Symbol", "mathSym/misc/forall"),
        new TexCommandEntry("exists", "\\exists\n\nexists Symbol", "mathSym/misc/exists"),
        new TexCommandEntry("nabla", "\\nabla\n\n", "mathSym/misc/nabla"),
        new TexCommandEntry("partial", "\\partial\n\nSymbol for partial derivation", "mathSym/misc/partial"),
        new TexCommandEntry("infty", "\\infty\n\nInfinity", "mathSym/misc/infty"),
        new TexCommandEntry("angle", "\\angle\n\nAngle symbol", "mathSym/misc/angle"),
        new TexCommandEntry("emptyset", "\\emptyset\n\nEmpty set", "mathSym/misc/emptyset"),        
        new TexCommandEntry("int", "\\int\n\nIntegral symbol", "mathSym/misc/int"),
        new TexCommandEntry("oint", "\\oint\n\nIntegral symbol with circle", "mathSym/misc/oint"),
        new TexCommandEntry("sum", "\\sum\n\nSummation symbol", "mathSym/misc/sum"),
        new TexCommandEntry("prod", "\\prod\n\nProduct symbol", "mathSym/misc/prod"),
        new TexCommandEntry("coprod", "\\coprod\n\nReverse product symbol", "mathSym/misc/coprod"),
        new TexCommandEntry("Re", "\\Re\n\n", "mathSym/misc/re"),
        new TexCommandEntry("Im", "\\Im\n\n", "mathSym/misc/im"),
        new TexCommandEntry("imath", "\\imath\n\n", "mathSym/misc/imath"),
        new TexCommandEntry("jmath", "\\jmath\n\n", "mathSym/misc/jmath"),
        new TexCommandEntry("overbrace", "\\overbrace{equation}\n\nGenerates a brace over equation. To \"label\" the overbrace, use a superscript.", 1),        
        new TexCommandEntry("underbrace", "\\underbrace{equation}\n\nGenerates a brace below equation. To \"label\" the underbrace, use a subscript.", 1),
        new TexCommandEntry("wp", "\\wp\n\nWeierstrass p", "mathSym/misc/wp")
    };
    public static final TexCommandEntry[] stdBraces = {
        new TexCommandEntry("langle", "\\langle\n\n", "mathSym/braces/langle"),
        new TexCommandEntry("rangle", "\\rangle\n\n", "mathSym/braces/rangle"),
        new TexCommandEntry("lfloor", "\\lfloor\n\n", "mathSym/braces/lfloor"),
        new TexCommandEntry("rfloor", "\\rfloor\n\n", "mathSym/braces/rfloor"),
        new TexCommandEntry("lceil", "\\lceil\n\n", "mathSym/braces/lceil"),
        new TexCommandEntry("rceil", "\\rceil\n\n", "mathSym/braces/rceil"),
        new TexCommandEntry("{", "\\{\n\n", "mathSym/braces/lbrace"),
        new TexCommandEntry("}", "\\}\n\n", "mathSym/braces/rbrace"),
        new TexCommandEntry("|", "\\|\n\n", "mathSym/braces/norm")
    };
    
    public static final TexCommandEntry[] stdAccents = {
        new TexCommandEntry("hat", "\\hat{char}\n\n", 1, "mathSym/accents/hat"),
        new TexCommandEntry("check", "\\check{char}\n\n", 1, "mathSym/accents/check"),
        new TexCommandEntry("breve", "\\breve{char}\n\n", 1, "mathSym/accents/breve"),
        new TexCommandEntry("acute", "\\acute{char}\n\n", 1, "mathSym/accents/acute"),
        new TexCommandEntry("grave", "\\grave{char}\n\n", 1, "mathSym/accents/grave"),
        new TexCommandEntry("tilde", "\\tilde{char}\n\n", 1, "mathSym/accents/tilde"),
        new TexCommandEntry("bar", "\\bar{char}\n\n", 1, "mathSym/accents/bar"),
        new TexCommandEntry("vec", "\\vec{char}\n\n", 1, "mathSym/accents/vec"),
        new TexCommandEntry("dot", "\\dot{char}\n\n", 1, "mathSym/accents/dot"),
        new TexCommandEntry("ddot", "\\ddot{char}\n\n", 1, "mathSym/accents/ddot")
    };

    public static final TexCommandEntry[] stdArrows = {
        new TexCommandEntry("leftarrow", "\\leftarrow\n\nSimple arrow which points to the left", "mathSym/arrows/leftarrow"),
        new TexCommandEntry("gets", "\\gets\n\nSimple arrow which points to the left", "mathSym/arrows/leftarrow"),
        new TexCommandEntry("Leftarrow", "\\Leftarrow\n\nDouble arrow which points to the left", "mathSym/arrows/bigleftarrow"),
        new TexCommandEntry("rightarrow", "\\rightarrow\n\nSimple arrow which points to the right", "mathSym/arrows/to"),
        new TexCommandEntry("to", "\\to\n\nSimple arrow which points to the right", "mathSym/arrows/to"),
        new TexCommandEntry("Rightarrow", "\\Rightarrow\n\nDouble arrow which points to the right", "mathSym/arrows/bigto"),
        new TexCommandEntry("leftrightarrow", "\\leftrightarrow\n\nSimple arrow which points to the left and the right", "mathSym/arrows/leftrightarrow"),
        new TexCommandEntry("Leftrightarrow", "\\Leftrightarrow\n\nDouble arrow which points to the left and the right", "mathSym/arrows/bigleftrightarrow"),
        new TexCommandEntry("mapsto", "\\mapsto\n\nSimple mapping arrow which points to the right", "mathSym/arrows/mapsto"),

        new TexCommandEntry("hookleftarrow", "\\hookleftarrow\n\nHooked arrow which points to the left", "mathSym/arrows/hookleftarrow"),
        new TexCommandEntry("hookrightarrow", "\\hookrightarrow\n\nHooked arrow which points to the right", "mathSym/arrows/hookrightarrow"),
        new TexCommandEntry("leftharpoonup", "\\leftharpoonup\n\nHarpon arrow which points to the left", "mathSym/arrows/leftharpoonup"),
        new TexCommandEntry("leftharpoondown", "\\leftharpoondown\n\nHarpon arrow which points to the left", "mathSym/arrows/leftharpoondown"),
        new TexCommandEntry("rightharpoonup", "\\rightharpoonup\n\nHarpon arrow which points to the right", "mathSym/arrows/rightharpoonup"),
        new TexCommandEntry("rightharpoondown", "\\rightharpoondown\n\nHarpon arrow which points to the right", "mathSym/arrows/rightharpoondown"),
        new TexCommandEntry("leftrightharpoons", "\\leftrightharpoons\n\nHarpon arrow which points to the left and the right", "mathSym/arrows/leftrightharpoons"),

        new TexCommandEntry("longleftarrow", "\\longleftarrow\n\nLong arrow which points to the left", "mathSym/arrows/longleftarrow"),
        new TexCommandEntry("Longleftarrow", "\\Longleftarrow\n\nLong double arrow which points to the left", "mathSym/arrows/biglongleftarrow"),
        new TexCommandEntry("longrightarrow", "\\longrightarrow\n\nLong arrow which points to the right", "mathSym/arrows/longrightarrow"),
        new TexCommandEntry("Longrightarrow", "\\Longrightarrow\n\nLong double arrow which points to the right", "mathSym/arrows/biglongrightarrow"),
        new TexCommandEntry("longleftrightarrow", "\\longleftrightarrow\n\nLong arrow which points to the left and the right", "mathSym/arrows/longleftrightarrow"),
        new TexCommandEntry("Longleftrightarrow", "\\Longleftrightarrow\n\nLong double arrow which points to the left and the right", "mathSym/arrows/biglongleftrightarrow"),
        new TexCommandEntry("longmapsto", "\\longmapsto\n\nLong mapping arrow which points to the right", "mathSym/arrows/longmapsto"),

        new TexCommandEntry("uparrow", "\\uparrow\n\nSimple arrow which points up", "mathSym/arrows/uparrow"),
        new TexCommandEntry("Uparrow", "\\Uparrow\n\nDouble arrow which points up", "mathSym/arrows/biguparrow"),
        new TexCommandEntry("downarrow", "\\downarrow\n\nSimple arrow which points down", "mathSym/arrows/downarrow"),
        new TexCommandEntry("Downarrow", "\\Downarrow\n\nDouble arrow which points down", "mathSym/arrows/bigdownarrow"),
        new TexCommandEntry("updownarrow", "\\updownarrow\n\nSimple arrow which points up and down", "mathSym/arrows/updownarrow"),
        new TexCommandEntry("Updownarrow", "\\Updownarrow\n\nDouble arrow which points up and down", "mathSym/arrows/bigupdownarrow"),
        
        new TexCommandEntry("nearrow", "\\nearrow\n\nSimple arrow which points north east", "mathSym/arrows/nearrow"),
        new TexCommandEntry("searrow", "\\searrow\n\nSimple arrow which points south east", "mathSym/arrows/searrow"),
        new TexCommandEntry("swarrow", "\\swarrow\n\nSimple arrow which points south west", "mathSym/arrows/swarrow"),
        new TexCommandEntry("nwarrow", "\\nwarrow\n\nSimple arrow which points north west", "mathSym/arrows/nwarrow"),
        new TexCommandEntry("leadsto", "\\leadsto\n\ns-shaped arrow which points to the right", "mathSym/arrows/leadsto")      
    };
    
    public static final TexCommandEntry[] stdCompare = {
        new TexCommandEntry("leq", "\\leq\n\nLower or equal symbol", "mathSym/compare/le"),
        new TexCommandEntry("ll", "\\ll\n\nDouble lower symbol", "mathSym/compare/ll"),
        new TexCommandEntry("subset", "\\subset\n\nSubset", "mathSym/compare/subset"),
        new TexCommandEntry("subseteq", "\\subseteq\n\nSubset or equal", "mathSym/compare/subseteq"),
        new TexCommandEntry("sqsubset", "\\sqsubset\n\nSquare subset", "mathSym/compare/sqsubset"),
        new TexCommandEntry("sqsubseteq", "\\sqsubseteq\n\nSquare subset or equal", "mathSym/compare/sqsubseteq"),
        new TexCommandEntry("in", "\\in\nis Element of", "mathSym/compare/in"),
        new TexCommandEntry("vdash", "\\vdash\n\n", "mathSym/compare/vdash"),
        new TexCommandEntry("models", "\\models\n\n", "mathSym/compare/models"),
        new TexCommandEntry("geq", "\\geq\n\nGreater or equal symbol", "mathSym/compare/ge"),
        new TexCommandEntry("gg", "\\gg\n\nDouble greater symbol", "mathSym/compare/gg"),
        new TexCommandEntry("supset", "\\supset\n\nSuperset", "mathSym/compare/supset"),
        new TexCommandEntry("supseteq", "\\supseteq\n\nSuperset or equal", "mathSym/compare/supseteq"),
        new TexCommandEntry("sqsupset", "\\sqsupset\n\nSquare superset", "mathSym/compare/sqsupset"),
        new TexCommandEntry("sqsupseteq", "\\sqsupseteq\n\nSquare superset or equal", "mathSym/compare/sqsupseteq"),
        new TexCommandEntry("ni", "\\ni\n\nis not element of", "mathSym/compare/owns"),
        new TexCommandEntry("dashv", "\\dashv\n\n", "mathSym/compare/dashv"),
        new TexCommandEntry("perp", "\\perp\n\n", "mathSym/compare/perp"),
        new TexCommandEntry("neq", "\\neq\n\nnot equal", "mathSym/compare/ne"),
        new TexCommandEntry("dotequal", "\\dotequal\n\nequal sign with dot", "mathSym/compare/doteq"),
        new TexCommandEntry("approx", "\\approx\n\nApproximately equal", "mathSym/compare/approx"),
        new TexCommandEntry("cong", "\\cong\n\n", "mathSym/compare/cong"),
        new TexCommandEntry("equiv", "\\equiv\n\nequivalent", "mathSym/compare/equiv"),
        new TexCommandEntry("propto", "\\propto\n\n", "mathSym/compare/propto"),
        new TexCommandEntry("prec", "\\prec\n\nPredecessor", "mathSym/compare/prec"),
        new TexCommandEntry("preceq", "\\prec\n\nPredecessor or equal", "mathSym/compare/preceq"),
        new TexCommandEntry("parallel", "\\parallel\n\nParallel", "mathSym/compare/parallel"),
        new TexCommandEntry("sim", "\\sim\n\n", "mathSym/compare/sim"),
        new TexCommandEntry("simeq", "\\simeq\n\n", "mathSym/compare/simeq"),
        new TexCommandEntry("asymp", "\\asymp\n\n", "mathSym/compare/asymp"),
        new TexCommandEntry("smile", "\\smile\n\n", "mathSym/compare/smile"),
        new TexCommandEntry("frown", "\\frown\n\n", "mathSym/compare/frown"),
        new TexCommandEntry("bowtie", "\\bowtie\n\n", "mathSym/compare/bowtie"),
        new TexCommandEntry("succ", "\\succ\n\nSuccessor", "mathSym/compare/succ"),
        new TexCommandEntry("succeq", "\\succeq\n\nSuccessor or equal", "mathSym/compare/succeq"),
        new TexCommandEntry("mid", "\\mid\n\n|", "mathSym/compare/mid")
    };

    public static final TexCommandEntry[] stdBinOpSymbols = {
    	new TexCommandEntry("pm", "\\pm  \n\n Plusminus", "mathSym/binopsymbols/pm"),
    	new TexCommandEntry("mp", "\\mp  \n\n Minusplus", "mathSym/binopsymbols/mp"),
    	new TexCommandEntry("times", "\\times Times symbol\n\n ", "mathSym/binopsymbols/times"),
    	new TexCommandEntry("div", "\\div Division symbol\n\n ", "mathSym/binopsymbols/div"),
    	new TexCommandEntry("cdot", "\\cdot \n\n Centered dot", "mathSym/binopsymbols/cdot"),
    	new TexCommandEntry("ast", "\\ast  \n\n Asterix", "mathSym/binopsymbols/ast"),
    	new TexCommandEntry("star", "\\star  \n\n Star", "mathSym/binopsymbols/star"),
    	new TexCommandEntry("dagger", "\\dagger  \n\n Dagger", "mathSym/binopsymbols/dagger"),
    	new TexCommandEntry("ddagger", "\\ddagger  \n\n Doulbe dagger", "mathSym/binopsymbols/ddag"),
    	new TexCommandEntry("amalg", "\\amalg  \n\n ", "mathSym/binopsymbols/amalg"),
    	new TexCommandEntry("cap", "\\cap  \n\n Intersection symbol", "mathSym/binopsymbols/cap"),
    	new TexCommandEntry("cup", "\\cup  \n\n Union symbol", "mathSym/binopsymbols/cup"),
    	new TexCommandEntry("bigcap", "\\bigcap  \n\n Big intersection symbol ", "mathSym/binopsymbols/bigcap"),
    	new TexCommandEntry("bigcup", "\\bigcup  \n\n Big union symbol", "mathSym/binopsymbols/bigcup"),
    	new TexCommandEntry("uplus", "\\uplus  \n\n Union symbol with plus", "mathSym/binopsymbols/uplus"),
    	new TexCommandEntry("sqcap", "\\sqcap  \n\n Squared intersection symbol", "mathSym/binopsymbols/sqcap"),
    	new TexCommandEntry("sqcup", "\\sqcup  \n\n Squared union symbol", "mathSym/binopsymbols/sqcup"),
    	new TexCommandEntry("vee", "\\vee  \n\n Logical or", "mathSym/binopsymbols/vee"),
    	new TexCommandEntry("wedge", "\\wedge  \n\n Logical and", "mathSym/binopsymbols/wedge"),
    	new TexCommandEntry("bigvee", "\\bigvee  \n\n Big logical or", "mathSym/binopsymbols/bigvee"),
    	new TexCommandEntry("bigwedge", "\\bigwedge  \n\n Big logical and", "mathSym/binopsymbols/bigwedge"),
    	new TexCommandEntry("setminus", "\\setminus  \n\n Set minus", "mathSym/binopsymbols/setminus"),
    	new TexCommandEntry("wr", "\\wr  \n\n ", "mathSym/binopsymbols/wr"),
    	new TexCommandEntry("circ", "\\circ  \n\n Circle", "mathSym/binopsymbols/circ"),
    	new TexCommandEntry("bullet", "\\bullet  \n\n Bullet", "mathSym/binopsymbols/bullet"),
    	new TexCommandEntry("diamond", "\\diamond  \n\n Diamond", "mathSym/binopsymbols/diamond"),
    	new TexCommandEntry("oslash", "\\oslash  \n\n Circle with slash", "mathSym/binopsymbols/oslash"),
    	new TexCommandEntry("odot", "\\odot  \n\n Circle with dot", "mathSym/binopsymbols/odot"),
    	new TexCommandEntry("oplus", "\\oplus  \n\n Circle with plus", "mathSym/binopsymbols/oplus"),
    	new TexCommandEntry("ominus", "\\ominus  \n\n Circle with minus", "mathSym/binopsymbols/ominus"),
    	new TexCommandEntry("otimes", "\\otimes  \n\n Circle with times symbol", "mathSym/binopsymbols/otimes"),
    	new TexCommandEntry("bigodot", "\\bigodot  \n\n Big circle with dot", "mathSym/binopsymbols/bigodot"),
    	new TexCommandEntry("bigoplus", "\\bigoplus  \n\n Big circle with plus", "mathSym/binopsymbols/bigoplus"),
    	new TexCommandEntry("bigotimes", "\\bigotimes  \n\n Big Circle with times symbol", "mathSym/binopsymbols/bigotimes"),
    	new TexCommandEntry("bigtriangleup", "\\bigtriangleup  \n\n Triangle up", "mathSym/binopsymbols/bigtriangleup"),
    	new TexCommandEntry("bigtriangledown", "\\bigtriangledown  \n\n Triangle down", "mathSym/binopsymbols/bigtriangledown"),
    	new TexCommandEntry("triangleleft", "\\triangleleft  \n\n Triangle left", "mathSym/binopsymbols/triangleleft"),
    	new TexCommandEntry("triangleright", "\\triangleright  \n\n Triangle right", "mathSym/binopsymbols/triangleright")
    };

    public static final TexCommandEntry[] functionNames = {
    	new TexCommandEntry("sin", "\\sin  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("sinh", "\\sinh  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("arcsin", "\\arcsin  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("cos", "\\cos  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("cosh", "\\cosh  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("arccos", "\\arccos  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("tan", "\\tan  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("tanh", "\\tanh  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("arctan", "\\arctan  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("cot", "\\cot  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("coth", "\\coth  \n\n", 0, TexCommandEntry.MATH_CONTEXT),  
    	new TexCommandEntry("sec", "\\sec  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("min", "\\min  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("inf", "\\inf  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("max", "\\max  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("sup", "\\sup  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("lim", "\\lim  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("liminf", "\\liminf  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("limsup", "\\limsup  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("exp", "\\exp  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("ln", "\\ln  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("log", "\\log  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("arg", "\\arg  \n\n", 0, TexCommandEntry.MATH_CONTEXT),
    	new TexCommandEntry("csc", "\\csc  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("deg", "\\deg  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("det", "\\det  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("dim", "\\dim  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("gcd", "\\gcd  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("hom", "\\hom  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("ker", "\\ker  \n\n", 0, TexCommandEntry.MATH_CONTEXT),    	
    	new TexCommandEntry("lg", "\\lg  \n\n", 0, TexCommandEntry.MATH_CONTEXT),
    	new TexCommandEntry("Pr", "\\Pr  \n\n", 0, TexCommandEntry.MATH_CONTEXT)
    };

    private Map<String, List<TexCommandEntry>> commandHash;
    private List<TexCommandEntry> sortedCommands;
    private int size;
    //Saves the positions of the contexts
    private int[] contexts;
    
    /**
     * Constructs a new command container
     */
    public TexCommandContainer() {
    	sortedCommands = new ArrayList<TexCommandEntry>(100);
        commandHash = new HashMap<String, List<TexCommandEntry>>(4);
        contexts = new int[TexCommandEntry.NUMBER_OF_CONTEXTS + 1];
        organize();
//        for (int i=0; i<builtIn.length; i++) sortedCommands.add(builtIn[i]);
        size = 0;
    }

    /**
     * Adds a new reference source (tex file) into this container or
     * updates an existing one.
     * 
     * @param key The name of the reference source (filename)
     * @param refs The commands to insert
     * @return true if the container needs a reorganize
     */
    public boolean addRefSource(String key, List<TexCommandEntry> refs) {
        //Add filenames to the entries
        for (Iterator<TexCommandEntry> iter = refs.iterator(); iter.hasNext();) {
            AbstractEntry r = (AbstractEntry) iter.next();
            r.fileName = key;
        }
        size += refs.size();
        List<TexCommandEntry> al = commandHash.put(key, refs);
        if (al != null)
            size -= al.size();
        //Check if something has changed
        if (refs.equals(al))
            return false;
        else
            return true;
    }

    /**
     * Searches (very inefficient) for the last entries of each
     * context and saves them in contexts
     */
    private void createContexts(){
    	contexts[0] = -1;
    	int current = 1;
    	for (int i=0; i < sortedCommands.size(); i++){
    		if (current != ((TexCommandEntry) sortedCommands.get(i)).context){
    			contexts[current] = i - 2;
    			current++;
    			if (current == TexCommandEntry.NUMBER_OF_CONTEXTS) break;
    		}
    	}
    	contexts[current] = sortedCommands.size() - 2;
    }
    
    /**
     * Organizes this container's contents into the sorted command array.
     * The added reference sources' commands are added to the built-in commands
     * and sorted.
     */
    public void organize() {
        //if (commandHash.size() == 0)
        //    return;
        List<TexCommandEntry> allRefs = new ArrayList<TexCommandEntry>(size);
        if (commandHash.size() > 1) {
            for (List<TexCommandEntry> l : commandHash.values()) {
                allRefs.addAll(l);
            }
        } else if (commandHash.size() == 1) {
            Iterator<List<TexCommandEntry>> iter = commandHash.values().iterator();
            allRefs = iter.next();
        }
        //copy all commands and change the context to activate them also in mathmode
        List<TexCommandEntry> mathRefs = new ArrayList<TexCommandEntry> (allRefs.size());
        for (TexCommandEntry c : allRefs) {
            TexCommandEntry element = new TexCommandEntry(c);
            element.context = TexCommandEntry.MATH_CONTEXT;
            mathRefs.add(element);
        }
        
        sortedCommands.clear();
        for (TexCommandEntry c : builtIn) sortedCommands.add(c);
        sortedCommands.addAll(allRefs);
        for (TexCommandEntry c : greekCapital) sortedCommands.add(c);
        for (TexCommandEntry c : greekSmall) sortedCommands.add(c);
        for (TexCommandEntry c : miscMath) sortedCommands.add(c);
        for (TexCommandEntry c : stdArrows) sortedCommands.add(c);
        for (TexCommandEntry c : stdCompare) sortedCommands.add(c);
        for (TexCommandEntry c : functionNames) sortedCommands.add(c);
        for (TexCommandEntry c : stdBinOpSymbols) sortedCommands.add(c);
        for (TexCommandEntry c : stdBraces) sortedCommands.add(c);
        for (TexCommandEntry c : stdAccents) sortedCommands.add(c);
        sortedCommands.addAll(mathRefs);
        Collections.sort(sortedCommands);
        createContexts();
    }
    
    /**
     * @return Returns the sortedCommands.
     */
    public List<TexCommandEntry> getSortedCommands(int context) {
        return sortedCommands.subList(contexts[context-1]+1, contexts[context]+2);
    }

}
