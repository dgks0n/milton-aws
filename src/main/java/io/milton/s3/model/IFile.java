/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.s3.model;

import java.io.IOException;
import java.io.InputStream;

public interface IFile extends IEntity {

	byte[] getBytes();
	
	String getContentType();
	
	long getSize();
	
	/**
	 * A client wants data back, so give them a stream they can read.
	 * 
	 * @return
	 * @throws IOException
	 */
	abstract InputStream getInputStream() throws IOException;
}
