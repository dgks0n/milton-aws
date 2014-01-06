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

import java.util.UUID;

public interface IEntity {
	
	UUID getId();
	
	String getName();
	
	void setName(final String name);
	
	IFolder getParent();
	
	void setParent(final IFolder parent);
	
	void moveTo(final IFolder target);
	
	/**
     * Copy the source object to the given parent and with the given name
     * 
     * @param target
     *            - the target folder
     * @param targetName
     *            - the target name
     * 
     * @return BaseEntity
     */
	abstract IEntity copyTo(final IFolder target, final String targetName);
}
