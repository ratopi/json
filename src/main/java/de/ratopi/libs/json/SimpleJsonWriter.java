/*
* SimpleJsonWriter.java
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
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A simple Object to JSON converter, which writes out a nice idented JSON.
 * See the following table to see what is transformed to what:
 * <table>
 *     <tr><th>Java class/interface/value</th><th>JSON type</th><th>e.g.</th></tr>
 *     <tr><td><i>null</i></td><td><i>null</i></td></tr>
 *     <tr><td>Boolean,boolean</td><td><i>false</i> or <i>true</i></td></tr>
 *     <tr><td>char,Character,String,C</td><td>null</td></tr>
 *     <tr><td>Collection, Iterator, Enumeration</td><td>array</td><td>List,Set</td></tr>
 *     <tr><td><i>[]</i> (array)</td><td>array</td></tr>
 *     <tr><td>Map</td><td>object</td></tr>
 *     <tr><td>java.lang.Object</td><td>object</td><td>All getters (and isser) are queried, except of "getClass".</td></tr>
 * </table>
 * <p/>
 * It has some switches:
 * <ul>
 *     <li>escapeNonAsciiCharacters: If set true, any non-ascii-character will be escaped as '\\u'-sequence. This is sometimes useful if you data will transfered over many steps or in mail.</li>
 * </ul>
 * <p/>
 * This class was very useful for me in the last years, therefore I state the
 * original class-comment here: <i>This is just a straight-forward implementation.  Don't know, if it will be useful any day.</i>
 * <p/>
 * Created: 18.04.2009<br/>
 * @author Ralf Th. Pietsch &lt;ratopi@abwesend.de&gt;
 */
public class SimpleJsonWriter implements JsonWriter
{
	// ==== static members ====

	private static final Collection<String> UNSERIALIZED_METHODS = Arrays.asList( "getClass" );

	private static final Map<Character, String> ENCODED_CHARACTERS;

	static
	{
		ENCODED_CHARACTERS = new HashMap<Character, String>();

		ENCODED_CHARACTERS.put( '"', "\\\"" );
		ENCODED_CHARACTERS.put( '\\', "\\\\" );

		ENCODED_CHARACTERS.put( '\b', "\\b" );
		ENCODED_CHARACTERS.put( '\f', "\\f" );
		ENCODED_CHARACTERS.put( '\n', "\\n" );
		ENCODED_CHARACTERS.put( '\r', "\\r" );
		ENCODED_CHARACTERS.put( '\t', "\\t" );
	}

	// ==== private attributes  ====

	/**
	 * If set true, any non-ascii-character will be escaped as '\\u'-sequence.
	 */
	private boolean escapeNonAsciiCharacters = false;

	private SimpleDateFormat simpleDateFormat;

	// ==== private members ====

	// ==== constructors ====

	public SimpleJsonWriter()
	{
		simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ); // ISO date string for time zone 'Z' = UTC
		simpleDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	// ==== simple getters and setters ====

	public boolean isEscapeNonAsciiCharacters()
	{
		return escapeNonAsciiCharacters;
	}

	public void setEscapeNonAsciiCharacters( final boolean escapeNonAsciiCharacters )
	{
		this.escapeNonAsciiCharacters = escapeNonAsciiCharacters;
	}

	public void setSimpleDateFormat( final SimpleDateFormat simpleDateFormat )
	{
		this.simpleDateFormat = simpleDateFormat;
	}

	// ==== "complex" getters and setters ====

	// ==== interface JsonWriter implementation ====

	public void writeJson( final Object bean, final Writer writer )
	{
		try
		{
			if ( bean == null )
			{
				writer.write( "null" );
			}
			else if ( bean instanceof JsonSubstitutable )
			{
				final Object substitute = ( (JsonSubstitutable) bean ).giveJsonSubstitute();
				writeJson( substitute, writer );
			}
			else if ( bean instanceof JsonRepresentable )
			{
				final JsonRepresentable isonRepresentable = (JsonRepresentable) bean;
				isonRepresentable.writeJsonRepresent(writer);
			}
			else if ( bean instanceof Collection )
			{
				collectionToJson( writer, (Collection) bean );
			}
			else if ( bean instanceof Iterator )
			{
				iteratorToJson( writer, (Iterator) bean );
			}
			else if ( bean instanceof Enumeration )
			{
				enumeratorToJson( writer, (Enumeration) bean );
			}
			else if ( bean instanceof Object[] )
			{
				arrayToJson( writer, (Object[]) bean );
			}
			else if ( bean instanceof Map )
			{
				mapToJson( writer, (Map) bean );
			}
			else if ( bean instanceof CharSequence )
			{
				stringToJson( writer, (CharSequence) bean );
			}
			else if ( bean instanceof Character )
			{
				stringToJson( writer, Character.toString( (Character) bean ) );
			}
			else if ( bean instanceof Number )
			{
				numberToJson( writer, (Number) bean );
			}
			else if ( bean instanceof Boolean )
			{
				booleanToJson( writer, (Boolean) bean );
			}
			else if ( bean instanceof Date )
			{
				dateToJson( writer, (Date) bean );
			}
			else if ( bean instanceof Enum )
			{
				enumToJson( writer, (Enum) bean );
			}
			else
			{
				beanToJson( writer, bean );
			}
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	// ==== business logic ====

	// ==== lifecycle methods ====

	// ==== private methods ====

	private void collectionToJson( final Writer writer, final Collection collection ) throws IOException
	{
		iteratorToJson( writer, collection.iterator() );
	}

	private void enumeratorToJson( final Writer writer, final Enumeration enumeration ) throws IOException
	{
		iteratorToJson( writer, new EnumerationIterator( enumeration ) );
	}

	private void iteratorToJson( final Writer writer, final Iterator iterator ) throws IOException
	{
		writer.write( '[' );

		while ( iterator.hasNext() )
		{
			final Object o = iterator.next();
			writeJson( o, writer );

			if ( iterator.hasNext() ) writer.write( ',' );
		}

		writer.write( ']' );
	}

	private void arrayToJson( final Writer writer, final Object[] array ) throws IOException
	{
		writer.write( '[' );

		for ( int i = 0; i < array.length; i++ )
		{
			if ( i > 0 ) writer.write( ',' );

			final Object o = array[ i ];
			writeJson( o, writer );
		}

		writer.write( ']' );
	}

	private void mapToJson( final Writer writer, final Map map ) throws IOException
	{
		writer.write( '{' );

		boolean isNotFirst = false;

		for ( final Object x : map.entrySet() )
		{
			if ( isNotFirst ) writer.write( ',' );
			isNotFirst = true;

			final Map.Entry entry = (Map.Entry) x;

			final Object key = entry.getKey();
			writeJson( key, writer );

			writer.write( ':' );

			final Object value = entry.getValue();
			writeJson( value, writer );
		}

		writer.write( '}' );
	}

	private void stringToJson( final Writer writer, final CharSequence charSequence ) throws IOException
	{
		writer.write( '"' );

		for ( int i = 0; i < charSequence.length(); i++ )
		{
			final char ch = charSequence.charAt( i );

			if ( ch < '\u0020'  ||  ( escapeNonAsciiCharacters  &&  ch > '\u007E' ) )
			{
				if ( ENCODED_CHARACTERS.keySet().contains( ch ) )
				{
					writer.write( ENCODED_CHARACTERS.get( ch ) );
				}
				else
				{
					String codeText = Integer.toHexString( ch );
					while ( codeText.length() < 4 )
					{
						codeText = "0" + codeText;
					}
					writer.write( "\\u" + codeText );
				}
			}
			else if ( ENCODED_CHARACTERS.keySet().contains( ch ) )
			{
				writer.write( ENCODED_CHARACTERS.get( ch ) );
			}
			else
			{
				writer.write( ch );
			}
		}

		writer.write( '"' );
	}

	private void numberToJson( final Writer writer, final Number number ) throws IOException
	{
		writer.write( String.valueOf( number ) );
	}

	private void booleanToJson( final Writer writer, final Boolean aBoolean ) throws IOException
	{
		writer.write( String.valueOf( aBoolean ) );
	}

	private void dateToJson( final Writer writer, final Date aDate ) throws IOException
	{
		writer.write( '"' );
		synchronized ( simpleDateFormat )
		{
			writer.write( simpleDateFormat.format( aDate ) );
		}
		writer.write( '"' );
	}

	private void enumToJson( final Writer writer, final Enum anEnum ) throws IOException
	{
		stringToJson( writer, anEnum.toString() );
	}

	private void beanToJson( final Writer writer, final Object bean ) throws IOException
	{
		try
		{
			writer.write( '{' );

			final Class clazz = bean.getClass();
			final Method[] methods = clazz.getMethods();

			boolean isNotFirst = false;
			for ( final Method method : methods )
			{
				if ( method.getParameterTypes().length == 0  &&  ! UNSERIALIZED_METHODS.contains( method.getName() ) )
				{
					if ( ( method.getName().startsWith( "get" ) || method.getName().startsWith( "is" ) ) && method.getAnnotation( JsonTransient.class ) == null )
					{
						if ( isNotFirst ) writer.write( ',' );
						isNotFirst = true;

						final String attributeName = convertMethodNameToAttributeName( method.getName() );
						writeJson( attributeName, writer );

						writer.write( ':' );

						final Object value = method.invoke( bean );
						writeJson( value, writer );
					}
				}
			}

			writer.write( '}' );
		}
		catch ( InvocationTargetException e )
		{
			throw new RuntimeException( e );
		}
		catch ( IllegalAccessException e )
		{
			throw new RuntimeException( e );
		}
	}

	// ----

	private String convertMethodNameToAttributeName( final String name )
	{
		if ( name.startsWith( "is" ) )
		{
			return Character.toLowerCase( name.charAt( 2 ) ) + name.substring( 3 );
		}

		return Character.toLowerCase( name.charAt( 3 ) ) + name.substring( 4 );
	}

	// ==== methods from java.lang.Object ====

	// ==== inner classes ====

	private class EnumerationIterator implements Iterator
	{
		private Enumeration enumeration;

		private EnumerationIterator( final Enumeration enumeration )
		{
			this.enumeration = enumeration;
		}

		public boolean hasNext()
		{
			return enumeration.hasMoreElements();
		}

		public Object next()
		{
			return enumeration.nextElement();
		}

		public void remove()
		{
			throw new UnsupportedOperationException( "'remove' is not supported" );
		}
	}

}
