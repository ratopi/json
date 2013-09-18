/*
* JsonStringHelper.java
*
* Copyright (c) 2010-2013, Ralf Th. Pietsch. All rights reserved.
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
import java.io.StringReader;
import java.io.StringWriter;

/**
 * A small helper for everyday functions.
 * <p/>
 * Created: 02.10.2010<br/>
 * @author Ralf Th. Pietsch &lt;ratopi@abwesend.de&gt;
 */
public class JsonStringHelper
{
	// ==== static members ====

	// ==== private attributes ====

	private JsonWriter jsonWriter = new SimpleJsonWriter();

	private JsonReader jsonReader = new StraightForwardJsonReader();

	// ==== private members ====

	// ==== constructors ====

	// ==== simple getters and setters ====

	public void setJsonWriter( final JsonWriter jsonWriter )
	{
		this.jsonWriter = jsonWriter;
	}

	public void setJsonReader( final JsonReader jsonReader )
	{
		this.jsonReader = jsonReader;
	}

	// ==== "complex" getters and setters ====

	// ==== lifecycle methods ====

	// ==== interface XXX implementation ====

	// ==== business logic ====

	public String getJsonString( final Object o )
	{
		final StringWriter stringWriter = new StringWriter();

		try
		{
			jsonWriter.writeJson( o, stringWriter );

			return stringWriter.toString();
		}
		finally
		{
			try
			{
				stringWriter.close();
			}
			catch ( IOException e )
			{
				// we have to ignore this ...
				// throw new RuntimeException( e );
			}
		}
	}

	// ---

	public Object parseJsonString( final String json )
		throws IOException
	{
		final StringReader reader = new StringReader( json );

		try
		{
			return jsonReader.readJson( reader );
		}
		finally
		{
			reader.close();
		}
	}

	// ==== private methods ====

	// ==== methods from java.lang.Object ====

	// ==== inner classes ====
}
