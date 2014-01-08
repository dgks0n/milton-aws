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

public abstract class Entity implements IEntity {

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
     * Parent folder entity
     */
    private Folder parent;

    public Entity(String name, IFolder parent) {
    	this.id = UUID.randomUUID();
    	this.name = name;
        this.parent = (Folder) parent;
        this.createdDate = new Date();
        this.modifiedDate = new Date();
    }
    
    public Entity(UUID id, String name, Date createdDate, Date modifiedDate, IFolder parent) {
		this.id = id;
		this.name = name;
		this.parent = (Folder) parent;
		this.createdDate = createdDate;
		this.modifiedDate = modifiedDate;
	}

	@Override
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
		this.id = id;
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
    		throw new RuntimeException("Attempt to delete root folder");
    		
    	if (parent.getChildren() == null)
    		throw new NullPointerException("Children of root folder is null");
    	
        parent.getChildren().remove(this);
    }
}
