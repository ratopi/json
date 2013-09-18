/*
* StraightForwardJsonReader.java
*
* Copyright (c) 2009-2013, Ralf Th. Pietsch. All rights reserved.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301 USA
*/
package de.ratopi.libs.json;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * A straight forward implementation of a JsonReader.
 * This JsonReader returns "{}" as java.util.Maps and "[]" as java.util.Lists.
 * It returns JSON numbers as Integer or Double, so you should use the Number-interface
 * (which both implement) to get what you want.
 * <p/>
 * This implemention is very tolerant and can handle an extended JSON-syntax.
 * Additional to JSON it can handle:
 * <ul>
 * <li>one line comments ("//...") and comment blocks ("/*...* /")</li>
 * <li>strings limitied by single quotes "'"</li>
 * <li>strings may contain line breaks and special characters</li>
 * <li>objects holding any legal json type as key</li>
 * <p/>
 * Created: 13.06.2009<br/>
 * @author Ralf Th. Pietsch &lt;ratopi@abwesend.de&gt;
 */
public class StraightForwardJsonReader implements JsonReader
{
	// ==== static members ====

	private static final int PUSHBACK_BUFFER_SIZE = 10;

	// ==== private attributes ====

	private int errorLoggingContextSize;

	// ==== private members ====

	// ==== constructors ====

	// ==== simple getters and setters ====

	public int getErrorLoggingContextSize()
	{
		return errorLoggingContextSize;
	}

	public void setErrorLoggingContextSize( final int errorLoggingContextSize )
	{
		this.errorLoggingContextSize = errorLoggingContextSize;
	}

	// ==== "complex" getters and setters ====

	// ==== interface JsonReader implementation ====

	public Object readJson( final Reader reader ) throws IOException
	{
		final MyReader myReader = new MyReader( reader, errorLoggingContextSize );

		final int ch = myReader.nextNonWhitespaceCharacter();

		if ( ch == -1 ) return null;

		myReader.unread( ch );

		return readNextElement( myReader );
	}

	// ==== business logic ====

	// ==== lifecycle methods ====

	// ==== private methods ====

	private Object readNextElement( final MyReader reader ) throws IOException
	{
		final int ch = reader.nextNonWhitespaceCharacter();

		if ( ch == '{' )
		{
			return createObject( reader );
		}
		else if ( ch == '[' )
		{
			return createList( reader );
		}
		else if ( Character.isDigit( ch )  ||  ch == '-'  ||  ch == '+'  ||  ch == '.' )
		{
			reader.unread( ch );
			return createNumber( reader );
		}
		else if ( ch == '"'  ||  ch == '\'' )
		{
			return createString( reader, ch );
		}
		else if ( Character.isLetter( ch ) )
		{
			reader.unread( ch );
			return createToken( reader );
		}

		throw new RuntimeException( createErrorText( reader, "Syntax error at line " + reader.line + ", column " + reader.characterIndex + ". " + "Unexpected character \"" + Character.toString((char)ch) + "\"." ) );
	}

	private Object createObject( final MyReader reader ) throws IOException
	{
		final HashMap<Object,Object> map = new HashMap<Object,Object>();

		int ch = reader.nextNonWhitespaceCharacter();
		if ( ch != '}' )
		{
			reader.unread( ch );

			while ( ch != '}' )
			{
				ch = reader.nextNonWhitespaceCharacter();
				if ( ch != '}' )
				{
					reader.unread( ch );
					Object key = readNextElement( reader );

					ch = reader.nextNonWhitespaceCharacter();
					if ( ch != ':' ) throw new RuntimeException( createErrorText( reader, "Syntax error in object: Missing key value separator" ) );

					Object value = readNextElement( reader );

					map.put( key, value );

					ch = reader.nextNonWhitespaceCharacter();

					if ( ch != ','  &&  ch != '}' )
					{
						throw new RuntimeException( createErrorText( reader, "Syntax error in object: Missing end of object" ) );
					}
				}
			}
		}

		return map;
	}

	private Object createList( final MyReader reader ) throws IOException
	{
		final ArrayList<Object> list = new ArrayList<Object>();

		int ch = reader.nextNonWhitespaceCharacter();
		if ( ch != ']' )
		{
			reader.unread( ch );

			while ( ch != ']' )
			{
				ch = reader.nextNonWhitespaceCharacter();
				if ( ch != ']' )
				{
					reader.unread( ch );
					list.add( readNextElement( reader ) );

					ch = reader.nextNonWhitespaceCharacter();

					if ( ch != ','  &&  ch != ']' )
					{
						throw new RuntimeException( createErrorText( reader, "Syntax error in array: Missing value separator.  (Got '" + ( (char) ch ) + "')" ) );
					}
				}
			}
		}

		return list;
	}

	private Object createString( final MyReader reader, final int limitChar ) throws IOException
	{
		final StringBuilder stringBuilder = new StringBuilder();

		int ch;
		while ( ( ch = reader.read() ) != limitChar )
		{
			if ( ch == -1 ) throw new RuntimeException( createErrorText( reader, "Unexpected end of stream in string '" + stringBuilder.toString() + "'" ) );

			if ( ch == '\\' )
			{
				ch = reader.read();
				stringBuilder.append( escapedCharacter( ch, reader ) );
			}
			else
			{
				stringBuilder.append( (char) ch );
			}
		}

		return stringBuilder.toString();
	}

	private Object createToken( final MyReader reader ) throws IOException
	{
		int ch = reader.read();

		final StringBuilder chars = new StringBuilder();
		while ( Character.isLetter( ch )  ||  Character.isDigit( ch ) )
		{
			chars.append( (char) ch );
			ch = reader.read();
		}

		reader.unread( ch );

		final String text = chars.toString();

		if ( "null".equals( text ) ) return null;
		if ( "true".equals( text ) ) return true;
		if ( "false".equals( text ) ) return false;

		return text;
	}

	protected Object createNumber( final MyReader reader ) throws IOException
	{
		boolean isDouble = false;

		final StringBuilder stringBuilder = new StringBuilder();

		int ch = reader.read();
		while ( isNumberCharacter( ch ) )
		{
			if ( ch == '.'  ||  ch == 'e'  ||  ch == 'E' ) isDouble = true;

			stringBuilder.append( (char) ch );
			ch = reader.read();
		}

		if ( ch != -1 ) reader.unread( ch );

		if ( isDouble )
		{
			return Double.parseDouble( stringBuilder.toString() );
		}

		return Long.parseLong( stringBuilder.toString() );
	}

	protected boolean isNumberCharacter( final int ch )
	{
		return
			ch == '+' ||
			ch == '-' ||
			ch == '0' ||
			ch == '1' ||
			ch == '2' ||
			ch == '3' ||
			ch == '4' ||
			ch == '5' ||
			ch == '6' ||
			ch == '7' ||
			ch == '8' ||
			ch == '9' ||
			ch == 'e' ||
			ch == 'E' ||
			ch == '.';
	}

	private char escapedCharacter( final int ch, final MyReader reader ) throws IOException
	{
		switch ( ch )
		{
			case 'b': return '\b';
			case 'f': return '\f';
			case 'n': return '\n';
			case 'r': return '\r';
			case 't': return '\t';

			case 'u':
				StringBuilder digits = new StringBuilder();
				for ( int i = 0; i < 4; i++ )
				{
					digits.append( (char) reader.read() );
				}
				return (char) ( Integer.parseInt( digits.toString(), 16 ) );
		}

		return (char) ch;
	}

	// ---

	private String createErrorText( final MyReader reader, final String msg )
		throws RuntimeException
	{
		final StringBuilder message = new StringBuilder( msg );

		final List<StringBuilder> lastLines = reader.getLastLines();
		if ( lastLines != null )
		{
			message.append( "\n=====================================================================" );
			for ( final StringBuilder stringBuilder : lastLines )
			{
				message.append( "\n" ).append( stringBuilder );
			}
			message.append( " <<<---\n=====================================================================" );
		}

		return message.toString();
	}

	// ==== methods from java.lang.Object ====

	public static class MyReader extends PushbackReader
	{
		private int line;
		private int characterIndex;

		private LinkedList<StringBuilder> lastLines;
		private StringBuilder currentLine;
		private int maxLinesToStore = 10;

		// ===

		public MyReader( final Reader in, final int maxLinesToStore )
		{
			super( in, PUSHBACK_BUFFER_SIZE );

			this.maxLinesToStore = maxLinesToStore;

			if ( maxLinesToStore > 0 )
			{
				lastLines = new LinkedList<StringBuilder>();

				currentLine = new StringBuilder();
				lastLines.add( currentLine );
			}
		}

		// ===

		@Override
		public int read() throws IOException
		{
			final int ch = super.read();

			if ( ch == '\n' )
			{
				line++;
				characterIndex = 0;

				if ( currentLine != null )
				{
					currentLine = new StringBuilder();
					lastLines.add( currentLine );
					if ( lastLines.size() > maxLinesToStore )
					{
						lastLines.remove( 0 );
					}
				}
			}
			else
			{
				characterIndex++;

				if ( currentLine != null )
				{
					currentLine.append( (char) ch );
				}
			}


			return ch;
		}

		@Override
		public void unread( final int c ) throws IOException
		{
			characterIndex--;
			super.unread( c );

			if ( currentLine != null  &&  currentLine.length() > 0 )
			{
				currentLine.deleteCharAt( currentLine.length() - 1 );
			}
		}

		// ===

		private List<StringBuilder> getLastLines()
		{
			return lastLines;
		}

		// ---

		private int nextNonWhitespaceCharacter() throws IOException
		{
			int ch = this.read();
			while ( Character.isWhitespace( ch )  ||  ch == 160  ||  ch == 0xFEFF ) // Start of Unicode files ...
			{
				// if ( ch == -1 ) throw new RuntimeException( "Unexpected end of stream" );
				ch = this.read();
			}

			if ( ch == '/' )
			{
				ch = this.read();
				if ( ch == '/' )
				{
					while ( ch != '\n' )
					{
						ch = this.read();
					}
					ch = nextNonWhitespaceCharacter();
				}
				else if ( ch == '*' )
				{
					ch = this.read();
					boolean endOfComment = false;
					while ( ! endOfComment )
					{
						while ( ch != '*' )
						{
							ch = this.read();
						}
						ch = this.read();
						if ( ch == '/' ) endOfComment = true;
					}


					ch = nextNonWhitespaceCharacter();

				}
				else
				{
					this.unread( '/' );
				}
			}

			return ch;
		}
	}

}
