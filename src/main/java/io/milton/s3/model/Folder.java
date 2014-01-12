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

import java.util.Date;
import java.util.UUID;

public class Folder extends Entity {
    
    public Folder(String folderName, Folder parent) {
        super(folderName, parent);
    }
    
	public Folder(UUID id, String name, Date createdDate, Date modifiedDate,
			Folder parent) {
    	super(id, name, createdDate, modifiedDate, parent);
	}

    public File addFile(final String fileName) {
        File file = new File(fileName, this);
        file.setDirectory(false);
        return file;
    }
    
    public Folder addFolder(final String folderName) {
        Folder folder = new Folder(folderName, this);
        return folder;
    }
}
