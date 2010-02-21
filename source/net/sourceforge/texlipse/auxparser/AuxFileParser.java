package net.sourceforge.texlipse.auxparser;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexLexer;
import net.sourceforge.texlipse.texparser.node.EOF;
import net.sourceforge.texlipse.texparser.node.TArgument;
import net.sourceforge.texlipse.texparser.node.TCcite;
import net.sourceforge.texlipse.texparser.node.TCsymbol;
import net.sourceforge.texlipse.texparser.node.TCword;
import net.sourceforge.texlipse.texparser.node.TWord;
import net.sourceforge.texlipse.texparser.node.Token;

/**
 * Extracts information from the .aux file which is created by a latex run. 
 * If \include is used, several .aux-files exist. They are parsed recursively. 
 *
 * At the moment, this information is used to
 *
 * 1) decide if bibtex needs to be invoked
 *    (see method <code>referencesChanged()</code>)
 *
 * 2) to extract all labels for the whole document (some of them might be
 *    declared in self-defined latex commands, so the normal texlipse-parser
 *    does not find them
 *
 * further use cases are possible ...
 * 
 * @author Frank Lehrieder
 *
 */
public class AuxFileParser {

       
       /**
        * <code>bibcites</code> contains the list of strings as they appear in
        * the aux-file as "\bibcite{ref1}".
        * It is used for the comparison to <code>citations</code>.
        */
       private List<String> bibcites;

       /**
        * <code>citations</code> contains the citations ("\citation") in the
        * aux-file. It contains them in the order as they appear in the document,
        * but it does not contain dupplicate entries.
        */
       private List<String> citations;

       /**
        * Contains all labels defined in the aux-file
        */
       private List<String> labels;
       
       /**
        * The corresponding project
        */
       private IProject project;
       
       /** 
        * The .aux-file of the main document
        */
       private String rootAuxfile;

   public AuxFileParser(IProject project, String auxFile) {
	   this.project = project;
	   this.rootAuxfile = auxFile;
   }
       
   /**
    * Parses the aux-files. To be called by TexBuilder.java.
    */
   public void parse() {
       labels = doParse(rootAuxfile, "\\newLabel");
       citations = removeDupplicateCitations(doParse(rootAuxfile, "\\citation"));
       bibcites = doParse(rootAuxfile, "\\bibcite");
   }

   /**
    * Parses <code>input</code> and collects all tokens which follow the given
    * <code>command</code>. Used to extract e.g., labels, citations, bibcites
    *
    * @param filename name of the file to be parsed
    * @param command
    * @return list of Strings following the given command
    */
   private List<String> doParse(String filename, String command) {
	   IResource auxFile = project.getFile(filename);
	   String input = null;

	   try {
		   input = TexlipseProperties.getFileContents(auxFile);
	   } catch (IOException e) {
		   TexlipsePlugin.log("Could not parse .aux-file " + auxFile, e);
		   return new LinkedList<String>();
	   }

	   LatexLexer lexer = new LatexLexer(new PushbackReader(new StringReader(input), 1024));
	   Token prevCommand = null;
       Token prevSymbol = null;
       Token prevText = null;
       List<String> results = new LinkedList<String>();

       try {
           for (Token t = lexer.next(); !(t instanceof EOF); t = lexer.next()) {
               // extract the strings following <code>command</code>
        	   if (prevCommand == null) {
                       if ((t instanceof TCword || t instanceof TCcite)
                                       && t.getText().equalsIgnoreCase(command)) {
                               prevCommand = t;
                       }
               } else {
                       if (t instanceof TWord || t instanceof TArgument) {
                               if (command.equalsIgnoreCase(prevCommand.getText())) {
                                       results.add(t.getText());
                                       prevCommand = null;
                               } 
                       }
               }
               
               // look for additional .aux-files and parse them
               if (prevText != null) {
            	   if (prevText.getText().equalsIgnoreCase("input") &&
            			   t instanceof TWord) {
            		   prevText = null;   
            		   String newAuxFileName = t.getText();
            		   results.addAll(doParse(newAuxFileName, command));
            	   }
               }
               if (prevSymbol == null) {
            	   if (t instanceof TCsymbol) {
            		   prevSymbol = t;
            	   }
               } else {
            	   if (prevSymbol.getText().equals("\\@")) {
            		   prevText = t;
            	   }
            	   prevSymbol = null;
               }
           }
       } catch (Exception e) {
               // do nothing
       }
       return results;
   }

   /**
    * Removes dupplicates from the list of citations and splits
    * "multi-citations"
    *
    * @param citations
    * @return
    */
   private List<String> removeDupplicateCitations(List<String> citations) {
       List<String> unique = new LinkedList<String>();
       for (String citation : citations) {
               // split references like "ref1,ref2,ref3"
               String[] cits = citation.split(",");
               for (int i = 0; i < cits.length; i++) {
                       if (unique.contains(cits[i]) == false) {
                               unique.add(cits[i]);
                       }
               }
       }
       return unique;
   }

   /**
    * @return a list of all labels in the aux-file as
    * <code>ReferenceEntry</code>
    */
   public List<ReferenceEntry> getLabels() {
       List<ReferenceEntry> result = new LinkedList<ReferenceEntry>();
       for (String key : labels) {
               result.add(new ReferenceEntry(key, "No info available"));
       }
       return result;
   }

   /**
    * Checks if bibtex needs to be invoked because references in the tex
    * document changed. This can be the case when
    * 1) new references are added to the document
    * 2) references are removed from the document
    * 3) the order of the references changed (because some styles use numbers
    *    e.g., [1], [2], ...
    *
    * @return
    */
   public boolean referencesChanged() {
       /*
        * bibtex does not need to be invoked when both list contain the same
        * entries in the same order.
        */
       return !citations.equals(bibcites);
   }

}