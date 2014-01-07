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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Folder extends BaseEntity implements IFolder {
    
	/**
	 * A list of children's folder
	 */
    private List<BaseEntity> childrens = new ArrayList<BaseEntity>();
    
    public Folder(String folderName, Folder parent) {
        super(folderName, parent);
    }
    
    @Override
    public synchronized File addFile(final String fileName) {
        File file = new File(fileName, this);
        childrens.add(file);
        return file;
    }
    
    @Override
    public synchronized Folder addFolder(final String folderName) {
        Folder folder = new Folder(folderName, this);
        childrens.add(folder);
        return folder;
    }

    /**
     * Remove entity for the given name in a folder
     * 
     * @param the given name of entity
     */
    public void remove(final String entityName) {
        Iterator<BaseEntity> iterator = childrens.iterator();
        while(iterator.hasNext()) {
            if(iterator.next().getName().equals(entityName)) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Get all entities have existing in a folder
     * 
     * @return a list of entities
     */
    @Override
    public synchronized List<BaseEntity> getChildren() {
        return childrens;
    }
    
    /**
     * Get a entity for the given name in a folder
     * 
     * @param name
     * @return
     */
    @Override
    public BaseEntity getChildren(final String entityName) {
        for(BaseEntity entity : childrens) {
            if(entity.getName().equals(entityName))
                return entity;
        }
        return null;
    }
    
    @Override
    public void moveTo(final Folder target) {
        super.moveTo(target);
    }
    
    @Override
    public BaseEntity copyTo(final Folder target, final String targetName) {
    	Folder folder = target.addFolder(targetName);
        for(BaseEntity entity : childrens) {
            entity.copyTo(folder, entity.getName());
        }
        return folder;
    }
}
