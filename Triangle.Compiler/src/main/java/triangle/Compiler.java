/*
 * @(#)Compiler.java                        2.1 2003/10/07
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

package triangle;

import triangle.abstractSyntaxTrees.Program;
import triangle.codeGenerator.Emitter;
import triangle.codeGenerator.Encoder;
import triangle.contextualAnalyzer.Checker;
import triangle.syntacticAnalyzer.Parser;
import triangle.syntacticAnalyzer.Scanner;
import triangle.syntacticAnalyzer.SourceFile;
import triangle.treeDrawer.Drawer;
import triangle.optimiser.ConstantFolder;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

/**
 * The main driver class for the Triangle compiler.
 *
 * @version 2.1 7 Oct 2003
 * @author Deryck F. Brown
 */
public class Compiler {

	@Argument(alias = "i", description = "Input", required = true)
	static String sourceName;
	@Argument(alias = "tree", description = "before optimisations occur", required = false)
	static boolean showTree = false;
	@Argument(alias = "treeAfter", description = "after optimisations occur", required = false)
	static boolean showTreeOptimised = false;
	@Argument(alias = "s", description = "Show summary stat", required = false)
	static boolean stats = false;

	/** The filename for the object program, normally obj.tam. */
	@Argument(alias = "o", description = "Output", required = false)
	static String objectName = "obj.tam";

	private static Scanner scanner;
	private static Parser parser;
	private static Checker checker;
	private static Encoder encoder;
	private static Emitter emitter;
	private static ErrorReporter reporter;
	private static Drawer drawer;
	private static Drawer drawerAfter;
	private static Summariser summariser;

	/** The AST representing the source program. */
	private static Program theAST;

	/**
	 * Compile the source program to TAM machine code.
	 *
	 * @param sourceName   the name of the file containing the source program.
	 * @param objectName   the name of the file containing the object program.
	 * @param showingAST   true iff the AST is to be displayed after contextual
	 *                     analysis
	 * @param showingTable true iff the object description details are to be
	 *                     displayed during code generation (not currently
	 *                     implemented).
	 * @return true iff the source program is free of compile-time errors, otherwise
	 *         false.
	 */
	static boolean compileProgram(String sourceName, String objectName, boolean showingAST, boolean showingTable) {

		System.out.println("********** " + "Triangle Compiler (Java Version 2.1)" + " **********");

		System.out.println("Syntactic Analysis ...");
		SourceFile source = SourceFile.ofPath(sourceName);

		if (source == null) {
			System.out.println("Can't access source file " + sourceName);
			System.exit(1);
		}

		scanner = new Scanner(source);
		reporter = new ErrorReporter();
		parser = new Parser(scanner, reporter);
		checker = new Checker(reporter);
		emitter = new Emitter(reporter);
		encoder = new Encoder(emitter, reporter);
		drawer = new Drawer();
		drawerAfter = new Drawer();
		summariser = new Summariser();

		// scanner.enableDebugging();
		theAST = parser.parseProgram(); // 1st pass
		if (reporter.getNumErrors == 0) {
			// if (show1ingAST) {
			// drawer.draw(theAST);
			// }
			System.out.println("Contextual Analysis ...");
			checker.check(theAST); // 2nd pass
			if (showingAST) {
				drawer.draw(theAST);
			}
			if (folding) {
				theAST.visit(new ConstantFolder());
			}
			if (showingASTAfter) {
				drawerAfter.draw(theAST);
			}
			if (stats) {
				System.out.println("Generating Summary ...");
				theAST.visit(summariser);
				System.out.println("Number of Binary Expressions: " + summariser.getNumBinaryExpressions());
				System.out.println("Number of If Commands: " + summariser.getNumIfCommands());
				System.out.println("Number of While Commands: " + summariser.getNumWhileCommands());

			}

			if (reporter.getNumErrors == 0) {
				System.out.println("Code Generation ...");
				encoder.encodeRun(theAST, showingTable); // 3rd pass
			}
		}

		boolean successful = (reporter.getNumErrors() == 0);
		if (successful) {
			emitter.saveObjectProgram(objectName);
			System.out.println("Compilation was successful.");
		} else {
			System.out.println("Compilation was unsuccessful.");
		}
		return successful;
	}

	/**
	 * Triangle compiler main program.
	 *
	 * @param args the only command-line argument to the program specifies the
	 *             source filename.
	 */
	public static void main(String[] args) {
		Compiler compiler = new Compiler();
		Args.parseOrExit(compiler, args);

		String sourceName = args[0];
		boolean tree = (args.length > 1 && args[1].equalsIgnoreCase("tree"));
		var compiledOK = compileProgram(sourceName, objectName, showTree, showTreeOptimised, false);

		if (!compiledOK && !showTree && !showTreeOptimised) {
			System.exit(1);
		}
	}
}
