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

import io.milton.s3.util.Crypt;

import java.io.File;
import java.util.Date;

public abstract class Entity implements IEntity {

	/**
	 * UUID is unique ID
	 */
    private String id;
    
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
     * Stored full path of entity
     */
    private String localPath;

    /**
     * Parent folder entity
     */
    private Folder parent;

    public Entity(String name, IFolder parent) {
    	this.id = Crypt.toHexFromText(name);
    	this.name = name;
        this.parent = (Folder) parent;
        this.createdDate = new Date();
        this.modifiedDate = new Date();
        
        if (this.parent != null)
            this.localPath = this.parent.getLocalPath() + File.separatorChar + getName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
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

    @Override
    public IFolder getParent() {
        return parent;
    }

    @Override
    public void setParent(final IFolder parent) {
        this.parent = (Folder) parent;
    }
    
    @Override
    public String getLocalPath() {
        return this.localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
    
    @Override
    public String getFullPathName() {
        if (parent == null) {
            return this.name;
        } else if (parent.getParent() == null) {
            return parent.getFullPathName() + getName();
        } else {
            return parent.getFullPathName() + File.separatorChar + getName();
        }
    }

    /**
     * Move the source object to the given parent and with the given name
     * 
     * @param target
     *            - the target folder
     */
    public void moveTo(final IFolder target) {
        this.modifiedDate = new Date();
        
        // Remove the source object
        parent.getChildren().remove(this);
        target.getChildren().add(this);
        this.parent = (Folder) target;
    }

    /**
     * Remove current entity
     */
    public void delete() {
    	if (this.getParent() == null)
    		throw new RuntimeException("Attempt to delete root");
    		
    	if (parent.getChildren() == null)
    		throw new NullPointerException("Children is null");
    	
        parent.getChildren().remove(this);
    }
}
