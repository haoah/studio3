package com.aptana.editor.coffee.internal.text;

import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.junit.After;
import org.junit.Test;

import com.aptana.editor.coffee.parsing.CoffeeParser;
import com.aptana.editor.common.text.reconciler.IFoldingComputer;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.ParseState;
import com.aptana.parsing.ast.IParseNode;

public class CoffeeFoldingComputerTest extends TestCase
{
	private IFoldingComputer folder;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	@After
	protected void tearDown() throws Exception
	{
		try
		{
			// EclipseUtil.instanceScope().getNode(CoffeeEditorPlugin.PLUGIN_ID).remove(IPreferenceConstants.INITIALLY_FOLD_COMMENTS);
		}
		finally
		{
			folder = null;
			super.tearDown();
		}
	}

	protected synchronized IFoldingComputer getFoldingComputer(IDocument document)
	{
		if (folder == null)
		{
			folder = new CoffeeFoldingComputer(null, document)
			{
				@Override
				protected IParseNode getAST()
				{
					IParseState parseState = new ParseState();
					parseState.setEditState(getDocument().get(), null, 0, 0);
					try
					{
						return new CoffeeParser().parse(parseState);
					}
					catch (Exception e)
					{
						fail(e.getMessage());
					}
					return null;
				};
			};
		}
		return folder;
	}

	@Test
	public void testFoldingObject() throws Exception
	{
		String src = "math =\n" + //
				"  root:   Math.sqrt\n" + //
				"  square: square\n" + //
				"  cube:   (x) -> x * square x";
		IDocument document = new Document(src);
		Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
		Collection<Position> positions = annotations.values();
		assertEquals(1, positions.size());
		assertTrue(positions.contains(new Position(0, src.length()))); // only can go so far as EOF
	}

	@Test
	public void testFoldingFunction() throws Exception
	{
		String src = "race = (winner, runners...) ->\n" + //
				"  print winner, runners"; //
		IDocument document = new Document(src);
		Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
		Collection<Position> positions = annotations.values();
		assertEquals(1, positions.size());
		// folding from args to end of function body
		assertTrue("Folding incorrect for anonymous function", positions.contains(new Position(7, src.length() - 7)));
	}

	@Test
	public void testFoldingIndentedObjectWithAssignment() throws Exception
	{
		String src = "kids =\n" + //
				"  brother:\n" + //
				"    name: \"Max\"\n" + //
				"    age:  11\n" + //
				"  sister:\n" + //
				"    name: \"Ida\"\n" + //
				"    age:  9"; //
		IDocument document = new Document(src);
		Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
		Collection<Position> positions = annotations.values();
		assertEquals(3, positions.size());
		assertTrue("Folding incorrect for 'kids' object", positions.contains(new Position(0, src.length())));
		assertTrue("Folding incorrect for 'brother'", positions.contains(new Position(9, 38)));
		assertTrue("Folding incorrect for 'sister'", positions.contains(new Position(49, src.length() - 49)));
	}

	@Test
	public void testMultilineArrayLiteral() throws Exception
	{
		String src = "bitlist = [\n" + //
				"  1, 0, 1\n" + //
				"  0, 0, 1\n" + //
				"  1, 1, 0\n" + //
				"]"; //
		IDocument document = new Document(src);
		Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
		Collection<Position> positions = annotations.values();
		assertEquals(1, positions.size());
		assertTrue("Folding incorrect for multieline array literal", positions.contains(new Position(0, src.length())));
	}

	@Test
	public void testIfBody() throws Exception
	{
		String src = "if this.studyingEconomics\n" + //
				"  buy()  while supply > demand\n" + //
				"  sell() until supply > demand"; //
		IDocument document = new Document(src);
		Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
		Collection<Position> positions = annotations.values();
		assertEquals(1, positions.size());
		assertTrue("Folding incorrect for if block", positions.contains(new Position(0, src.length())));
	}

	public void testClassesExample1() throws Exception
	{
		String source = "class Animal\n" + //
				"  constructor: (@name) ->\n" + //
				"\n" + //
				"  move: (meters) ->\n" + //
				"    alert @name + \" moved \" + meters + \"m.\"\n" + //
				"\n" + //
				"class Snake extends Animal\n" + //
				"  move: ->\n" + //
				"    alert \"Slithering...\"\n" + //
				"    super 5\n" + //
				"\n" + //
				"class Horse extends Animal\n" + //
				"  move: ->\n" + //
				"    alert \"Galloping...\"\n" + //
				"    super 45\n" + //
				"\n" + //
				"sam = new Snake \"Sammy the Python\"\n" + //
				"tom = new Horse \"Tommy the Palomino\"\n" + //
				"\n" + //
				"sam.move()\n" + //
				"tom.move()\n"; //

		IDocument document = new Document(source);
		Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
		Collection<Position> positions = annotations.values();
		assertEquals("Incorrect number of folding positions reported", 7, positions.size());
		assertTrue("Folding incorrect for Animal class block", positions.contains(new Position(0, 104)));
		assertTrue("Folding incorrect for Animal.constructor function block", positions.contains(new Position(28, 12)));
		assertTrue("Folding incorrect for Animal.move function block", positions.contains(new Position(48, 56)));
		assertTrue("Folding incorrect for Snake class block", positions.contains(new Position(105, 76)));
		assertTrue("Folding incorrect for Snake.move function block", positions.contains(new Position(140, 41)));
		assertTrue("Folding incorrect for Horse class block", positions.contains(new Position(182, 76)));
		assertTrue("Folding incorrect for Horse.move function block", positions.contains(new Position(217, 41)));
	}

	// TODO Do we want folding on try/catch/finally blocks?
	// @Test
	// public void testTryCatch() throws Exception
	// {
	// String src = "alert(\n" + //
	// "  try\n" + //
	// "    nonexistent / undefined\n" + //
	// "  catch error\n" + //
	// "    \"And the error is ... \" + error\n" + //
	// ")"; //
	// IDocument document = new Document(src);
	// Map<ProjectionAnnotation, Position> annotations = getFoldingComputer(document).emitFoldingRegions(true, null);
	// Collection<Position> positions = annotations.values();
	// assertEquals(2, positions.size());
	// assertTrue("Folding incorrect for try block", positions.contains(new Position(9, 32)));
	// assertTrue("Folding incorrect for catch block", positions.contains(new Position(43, 48)));
	// }

}
