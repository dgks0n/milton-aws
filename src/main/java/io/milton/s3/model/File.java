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

import java.net.URLConnection;
import java.util.Date;
import java.util.UUID;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang.StringUtils;

public class File extends Entity {
    
    private long size;
    
    private String contentType;

    public File(String fileName, Folder parent) {
        super(fileName, parent);
        this.setDirectory(false);
        this.contentType = getContentTypeFromName();
    }

	public File(UUID id, String name, Date createdDate, Date modifiedDate,
			Folder parent) {
		super(id, name, createdDate, modifiedDate, parent);
		this.setDirectory(false);
		this.contentType = getContentTypeFromName();
	}

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
    
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * Get content type of file based on given name
     * 
     * @return ContentType
     */
    protected String getContentTypeFromName() {
    	String contentType = URLConnection.guessContentTypeFromName(getName());
        if (StringUtils.isEmpty(contentType)) {
            contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(getName());
        }
        return contentType;
    }
    
    @Override
	public String toString() {
		return "Entity [id=" + getId() + ", name=" + getName()
				+ ", createdDate=" + getCreatedDate() + ", modifiedDate="
				+ getModifiedDate() + ", isDirectory=" + isDirectory()
				+ ", parent=" + getParent() + ", size=" + getSize()
				+ ", contentType=" + getContentType() + "]";
	}
    
}
