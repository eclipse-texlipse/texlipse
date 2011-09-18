/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.extension.BibProvider;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.model.TexDocumentModel;
import net.sourceforge.texlipse.model.TexStyleCompletionManager;
import net.sourceforge.texlipse.spelling.SpellChecker;
import net.sourceforge.texlipse.templates.TexContextType;
import net.sourceforge.texlipse.templates.TexTemplateCompletion;
import net.sourceforge.texlipse.texparser.LatexParserUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.HippieProposalProcessor;

/**
 * Implements a LaTeX-content assistant for displaying a list of completions for
 * \ref, \pageref and \cite. For BibTeX-completions (\cite), the details of the
 * selected entry are displayed. The actualy completions are fetched from the
 * ReferenceManager, owned by the TexDocumentModel of the current document.
 * 
 * @author Oskar Ojala
 */
public class TexCompletionProcessor implements IContentAssistProcessor {

	private TexTemplateCompletion templatesCompletion = new TexTemplateCompletion(
			TexContextType.TEX_CONTEXT_TYPE);

	private TexDocumentModel model;
	private ReferenceManager refManager;
	private ISourceViewer fviewer;
	private TexStyleCompletionManager styleManager;

	public static final int assistLineLength = 60;

	private final HippieProposalProcessor hippie = new HippieProposalProcessor();

	/**
	 * A regexp pattern for resolving the command used for referencing (in the
	 * 1st group)
	 */
	private static final Pattern comCapt = Pattern
			.compile("([a-zA-Z]+)\\s*(?:\\[.*?\\]\\s*)?");

	/**
	 * Receives the document model from the editor (one model/editor view) and
	 * creates a new completion processor.
	 * 
	 * @param tdm
	 *            The document model for this editor
	 */
	public TexCompletionProcessor(TexDocumentModel tdm, ISourceViewer viewer) {
		this.model = tdm;
		this.fviewer = viewer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.text.contentassist.IContentAssistProcessor#
	 * computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		if (refManager == null)
			this.refManager = this.model.getRefMana();

		model.removeStatusLineErrorMessage();
		IDocument doc = viewer.getDocument();

		// try to see if a text area is selected
		Point selectedRange = viewer.getSelectedRange();
		if (selectedRange.y > 0) {
			try {
				String text = doc.get(selectedRange.x, selectedRange.y);
				return computeStyleProposals(text, selectedRange);
			} catch (BadLocationException e) {
			}
		}

		// if not, proceed to templates and "regular" completions
		try {
			int lineStartOffset = doc
					.getLineOffset(doc.getLineOfOffset(offset));
			String lineStart = doc.get(lineStartOffset, offset
					- lineStartOffset);

			ICompletionProposal[] proposals = null;
			ICompletionProposal[] templateProposals = computeTemplateCompletions(
					offset, lineStart, viewer);

			if (!(lineStart.length() >= 2 && lineStart.endsWith("\\\\"))
					&& (lineStart.length() > 0)) {

				int seqStartIdx = resolveCompletionStart(lineStart, lineStart
						.length() - 1);
				String seqStart = lineStart.substring(seqStartIdx);

				if (seqStart.startsWith("\\")) {
					String replacement = seqStart.substring(1);
					proposals = computeCommandCompletions(offset, replacement
							.length(), replacement);
				} else if (seqStart.startsWith("{")) {
					proposals = resolveReferenceCompletions(lineStart, offset,
							seqStart);
					if (proposals == null) {
						// Maybe there is a wrong spelled word here (e.g.
						// \section{Wroang ...})
						proposals = SpellChecker.getSpellingProposal(offset,
								fviewer);
						if (proposals != null && proposals.length > 0) {
							return proposals;
						} else {
							// Hippie!!
							proposals = hippie.computeCompletionProposals(
									fviewer, offset);
						}

					}
				} else if (seqStart.length() > 0) {
					// ---------------------spell-checking-code-starts----------------------
					// spell checking can't help with words not starting with a
					// letter...
					proposals = SpellChecker.getSpellingProposal(offset,
							fviewer);
					if (proposals != null && proposals.length > 0) {
						return proposals;
					}
					// if there is no spelling corrections, use the
					// HippieProposal
					proposals = hippie.computeCompletionProposals(fviewer,
							offset);
				}
			}

			// Concatenate the lists if necessary
			if (proposals != null) {
				ICompletionProposal[] value = new ICompletionProposal[proposals.length
						+ templateProposals.length];
				System.arraycopy(templateProposals, 0, value, 0,
						templateProposals.length);
				System.arraycopy(proposals, 0, value, templateProposals.length,
						proposals.length);
				return value;
			} else {
				if (templateProposals.length == 0) {
					model
							.setStatusLineErrorMessage(" No completions available.");
				}
				return templateProposals;
			}
		} catch (BadLocationException e) {
			TexlipsePlugin.log("TexCompletionProcessor: ", e);
			return new ICompletionProposal[0];
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.text.contentassist.IContentAssistProcessor#
	 * computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {

		// FIXME -- for testing
		// Retrieve selected range
		Point selectedRange = viewer.getSelectedRange();
		if (selectedRange.y > 0) {

			if (styleManager == null) {
				styleManager = TexStyleCompletionManager.getInstance();
			}
			return styleManager.getStyleContext();
		}
		return new ContextInformation[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.text.contentassist.IContentAssistProcessor#
	 * getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '{', '\\' };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.text.contentassist.IContentAssistProcessor#
	 * getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		// return new char[] {'#'};
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage
	 * ()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.jface.text.contentassist.IContentAssistProcessor#
	 * getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return new ContextInformationValidator(this);
	}

	/**
	 * Resolves the completion replacement string so that the activation
	 * character is the first character following the returned offset.
	 * 
	 * @param doc
	 *            The document
	 * @param offset
	 *            The offset from where to search backwards
	 * @return The offset where the activation char is found or -1 if it was not
	 *         found
	 */
	private int resolveCompletionStart(String doc, int offset) {
		while (offset > 0) {
			if (Character.isWhitespace(doc.charAt(offset))
					|| doc.charAt(offset) == '}' || doc.charAt(offset) == '{'
					|| doc.charAt(offset) == '\\')
				break;
			offset--;
		}
		return offset;
	}

	/**
	 * Resolves the command used for referencing (i.e. from '\foo{bar' it
	 * resolves bar), figures out what kind of reference we want, fetches the
	 * list of matching completions and returns it.
	 * 
	 * @param line
	 *            The line containing the referencing command
	 * @param offset
	 *            The offset of the cursor position in the document
	 * @param lineEnd
	 *            The last part of the line, containing the partial match
	 * @return The completion proposals
	 */
	private ICompletionProposal[] resolveReferenceCompletions(String line,
			int offset, String lineEnd) {
		int lastIndex = line.lastIndexOf('\\');
		if (lastIndex == -1) {
			return null;
		}
		String fullCommand = line.substring(lastIndex + 1, line.length()
				- lineEnd.length());
		Matcher m = comCapt.matcher(fullCommand);
		if (!m.matches()) {
			return null;
		}
		String command = m.group(1);

		String replacement = lineEnd.lastIndexOf(',') != -1 ? lineEnd
				.substring(lineEnd.lastIndexOf(',') + 1) : lineEnd.substring(1);

		ICompletionProposal[] proposals = null;
		if (command.indexOf("cite") > -1) {
			proposals = computeBibCompletions(offset, replacement.length(),
					replacement);
		} else if (command.indexOf("ref") > -1) {
			proposals = computeRefCompletions(offset, replacement.length(),
					replacement);
		}
		return proposals;
	}

	/**
	 * Computes and returns BibTeX-proposals.
	 * 
	 * @param offset
	 *            Current cursor offset
	 * @param replacementLength
	 *            The length of the string to be replaced
	 * @param prefix
	 *            The already typed prefix of the entry to assist with
	 * @return An array of completion proposals to use directly or null
	 */
	private ICompletionProposal[] computeBibCompletions(int offset,
			int replacementLength, String prefix) {

		List<ICompletionProposal> resultAsList = new ArrayList<ICompletionProposal>();
		List<ReferenceEntry> bibEntries = refManager.getCompletionsBib(prefix);
		//add the entries of the .bib file(s) to the results
		if (bibEntries != null) {
			for (int i = 0; i < bibEntries.size(); i++) {
				ReferenceEntry bib = bibEntries.get(i);
				String infoText = bib.info.length() > assistLineLength ? wrapString(
						bib.info, assistLineLength)
						: bib.info;
						resultAsList.add(new CompletionProposal(bib.key, offset
								- replacementLength, replacementLength, bib.key.length(),
								null, bib.key, null, infoText));
			}
		}
		//the extension points
		IConfigurationElement[] configuration = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						"net.sourceforge.texlipse.CiteAutocompleteExtension");
		if (configuration.length > 0) {
			for (IConfigurationElement elem : configuration) {
				try {
					//fetches the BibProvider and updates the result
					BibProvider prov = (BibProvider) elem
							.createExecutableExtension("class");
					resultAsList = prov.getCompletions(offset, replacementLength,
							prefix, refManager.getBibContainer());

				} catch (CoreException e) {
					// e.printStackTrace();
					//log
				}
			}
		}

		//if there are no entries, return null
		if (resultAsList == null || resultAsList.size() == 0)
			return null;
		ICompletionProposal[] result = new ICompletionProposal[resultAsList
				.size()];
		return resultAsList.toArray(result);
	}

	/**
	 * Computes and returns reference-proposals (labels).
	 * 
	 * @param offset
	 *            Current cursor offset
	 * @param replacementLength
	 *            The length of the string to be replaced
	 * @param prefix
	 *            The already typed prefix of the entry to assist with
	 * @return An array of completion proposals to use directly or null
	 */
	private ICompletionProposal[] computeRefCompletions(int offset,
			int replacementLength, String prefix) {
		List<ReferenceEntry> refEntries = refManager.getCompletionsRef(prefix);
		if (refEntries == null)
			return null;

		ICompletionProposal[] result = new ICompletionProposal[refEntries
				.size()];

		for (int i = 0; i < refEntries.size(); i++) {

			String infoText = null;
			ReferenceEntry ref = refEntries.get(i);

			if (ref.info != null) {
				infoText = (ref.info.length() > assistLineLength) ? wrapString(
						ref.info, assistLineLength) : ref.info;
			}

			result[i] = new CompletionProposal(ref.key, offset
					- replacementLength, replacementLength, ref.key.length(),
					null, ref.key, null, infoText);
		}
		return result;
	}

	/**
	 * Returns a replacement String for an ICompleteProposal if there is an open
	 * environment
	 * 
	 * @param doc
	 *            IDocument.get()
	 * @param offset
	 *            current offset
	 * @return null if no open environment was found, else end{+name+}
	 */
	static String environmentEnd(String doc, int offset) {
		int o = offset;
		while ((o = doc.lastIndexOf("\\begin", o)) >= 0) {
			IRegion r = LatexParserUtils.getCommand(doc, o + 1);
			if (r != null) {
				String command = doc.substring(r.getOffset(), r.getOffset()
						+ r.getLength());
				if ("\\begin".equals(command)) {
					IRegion r2 = LatexParserUtils.getCommandArgument(doc, o);
					if (r2 != null) {
						String envName = doc.substring(r2.getOffset(), r2
								.getOffset()
								+ r2.getLength());
						if (TexAutoIndentStrategy.needsEnd(envName, doc, r
								.getOffset())) {
							return "end{" + envName + "}";
						}
					}
				}
			}
			o--;
		}
		return null;
	}

	/**
	 * Computes and returns command-proposals
	 * 
	 * @param offset
	 *            Current cursor offset
	 * @param replacementLength
	 *            The length of the string to be replaced
	 * @param prefix
	 *            The already typed prefix of the entry to assist with
	 * @return An array of completion proposals to use directly or null
	 */
	private ICompletionProposal[] computeCommandCompletions(int offset,
			int replacementLength, String prefix) {
		List<TexCommandEntry> comEntries = refManager.getCompletionsCom(prefix,
				TexCommandEntry.NORMAL_CONTEXT);
		if (comEntries == null)
			return null;

		CompletionProposal cp = null;
		if ("\\".equals(prefix) || "end".startsWith(prefix)) {
			String endString = environmentEnd(fviewer.getDocument().get(),
					offset);
			if (endString != null) {
				cp = new CompletionProposal(endString, offset
						- replacementLength, replacementLength, endString
						.length());
			}
		}

		int start;
		ICompletionProposal[] result;
		if (cp == null) {
			result = new ICompletionProposal[comEntries.size()];
			start = 0;
		} else {
			result = new ICompletionProposal[comEntries.size() + 1];
			result[0] = cp;
			start = 1;
		}

		for (int i = 0; i < comEntries.size(); i++) {
			result[i + start] = new TexCompletionProposal(comEntries.get(i),
					offset - replacementLength, replacementLength, fviewer);
		}
		return result;
	}

	/**
	 * Calculates and returns the template completions proposals.
	 * 
	 * @param offset
	 *            Current cursor offset
	 * @param lineStart
	 *            The already typed prefix of the entry to assist with
	 * @param viewer
	 *            The text viewer of this document
	 * @return An array of completion proposals to use directly
	 */
	private ICompletionProposal[] computeTemplateCompletions(int offset,
			String lineStart, ITextViewer viewer) {
		int t = lineStart.lastIndexOf(' ');
		if (t < lineStart.lastIndexOf('\t'))
			t = lineStart.lastIndexOf('\t');
		String replacement = lineStart.substring(t + 1);

		List<ICompletionProposal> returnProposals = templatesCompletion
				.addTemplateProposals(viewer, offset, replacement);
		ICompletionProposal[] proposals = new ICompletionProposal[returnProposals
				.size()];

		returnProposals.toArray(proposals);
		return proposals;
	}

	/**
	 * Wraps the given string to the given column width.
	 * 
	 * TODO this method will probably be mvoed to another class
	 * 
	 * @param input
	 *            The string to wrap
	 * @param width
	 *            The wrapping width
	 * @return The wrapped string
	 */
	public static String wrapString(String input, int width) {
		StringBuffer sbout = new StringBuffer();

		// \n should suffice since we prettify in parsing...
		String[] paragraphs = input.split("\r\n|\n|\r");
		for (int i = 0; i < paragraphs.length; i++) {
			// skip if short
			if (paragraphs[i].length() < width) {
				sbout.append(paragraphs[i]);
				sbout.append("\n");
				continue;
			}
			// imagine how much better this would be with functional
			// programming...
			String[] words = paragraphs[i].split("\\s");
			int currLength = 0;
			for (int j = 0; j < words.length; j++) {
				if (words[j].length() + currLength <= width || currLength == 0) {
					if (currLength > 0)
						sbout.append(" ");
					sbout.append(words[j]);
					currLength += 1 + words[j].length();
				} else {
					sbout.append("\n");
					sbout.append(words[j]);
					currLength = words[j].length();
				}
			}
			sbout.append("\n");
		}
		return sbout.toString();
	}

	// Some very quick style completions follow...
	// TODO improve this

	// private final static String[] STYLETAGS = new String[] {
	// "\\bf", "\\it", "\\rm", "\\sf", "\\sc", "\\em", "\\huge", "\\Huge"
	// };
	// private final static String[] STYLELABELS = new String[] {
	// "bold", "italic", "roman", "sans serif", "small caps", "emphasize",
	// "huge", "Huge"
	// };

	private ICompletionProposal[] computeStyleProposals(String selectedText,
			Point selectedRange) {
		if (styleManager == null) {
			styleManager = TexStyleCompletionManager.getInstance();
		}
		return styleManager.getStyleCompletions(selectedText, selectedRange);
		/*
		 * ICompletionProposal[] result = new
		 * ICompletionProposal[STYLETAGS.length];
		 * 
		 * // Loop through all styles for (int i = 0; i < STYLETAGS.length; i++)
		 * { String tag = STYLETAGS[i];
		 * 
		 * // Compute replacement text String replacement = "{" + tag + " " +
		 * selectedText + "}";
		 * 
		 * // Derive cursor position int cursor = tag.length() + 2;
		 * 
		 * // Compute a suitable context information IContextInformation
		 * contextInfo = new ContextInformation(null, STYLELABELS[i]+" Style");
		 * 
		 * // Construct proposal result[i] = new CompletionProposal(replacement,
		 * selectedRange.x, selectedRange.y, cursor, null, STYLELABELS[i],
		 * contextInfo, replacement); } return result;
		 */
	}

}
