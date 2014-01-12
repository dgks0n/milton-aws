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

public class Entity {

	/**
	 * UUID is unique ID
	 */
    private UUID id;
    
    /**
     * Name of entity
     */
    private String name;
    
    /**
     * Created date of entity
     */
    private Date createdDate;
    
    /**
     * Modified date of entity
     */
    private Date modifiedDate;
    
    /**
	 * Check entity is folder or not. Default is a Folder
	 */
    private boolean isDirectory;

    /**
     * Parent folder entity
     */
    private Folder parent;

    public Entity(String name, Folder parent) {
    	this.id = UUID.randomUUID();
    	this.name = name;
        this.parent = parent;
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        this.isDirectory = true;
    }
    
	public Entity(UUID id, String name, Date createdDate, Date modifiedDate,
			Folder parent) {
		this.id = id;
		this.name = name;
		this.parent = parent;
		this.createdDate = createdDate;
		this.modifiedDate = modifiedDate;
		this.isDirectory = true;
	}

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
		this.id = id;
	}

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
        this.modifiedDate = new Date();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }
    
    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

	public boolean isDirectory() {
		return isDirectory;
	}

	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	public Folder getParent() {
        return parent;
    }

    public void setParent(final Folder parent) {
        this.parent = parent;
    }

	@Override
	public String toString() {
		return "Entity [id=" + id + ", name=" + name + ", createdDate="
				+ createdDate + ", modifiedDate=" + modifiedDate
				+ ", isDirectory=" + isDirectory + ", parent=" + parent + "]";
	}

	@Override
	public boolean equals(Object obj) {
	    if (!(obj instanceof Entity)) {
	        return false;
	    }
	    
		Entity other = (Entity) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		} else if (!parent.equals(other.parent)) {
			return false;
		}
		
		if (isDirectory != other.isDirectory) {
			return false;
		}
		
		return true;
	}
}
