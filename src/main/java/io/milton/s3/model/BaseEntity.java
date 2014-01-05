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

public abstract class BaseEntity {

    private UUID id;
    private String name;
    private Date createdDate;
    protected Date modifiedDate;

    private Folder parent;

    public BaseEntity(String name, Folder parent) {
        this.name = name;
        this.parent = parent;
        this.id = UUID.randomUUID();
        this.createdDate = new Date();
        this.modifiedDate = new Date();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.modifiedDate = new Date();
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public Folder getParent() {
        return parent;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    /**
     * Move the source object to the given parent and with the given name
     * 
     * @param the
     *            target folder
     */
    public void moveTo(final Folder target) {
        this.modifiedDate = new Date();
        parent.getChildren().remove(this);
        target.getChildren().add(this);
        this.parent = target;
    }

    /**
     * Remove current entity
     */
    public void delete() {
        parent.getChildren().remove(this);
    }

    /**
     * Copy the source object to the given parent and with the given name
     * 
     * @param the
     *            target folder
     * @param the
     *            target name
     * 
     * @return BaseEntity
     */
    public abstract BaseEntity copyTo(final Folder target, final String targetName);
}
