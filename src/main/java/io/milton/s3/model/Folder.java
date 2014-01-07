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

public class Folder extends Entity implements IFolder {
    
	/**
	 * A list of children's folder
	 */
    private List<Entity> childrens = new ArrayList<Entity>();
    
    public Folder(String folderName, IFolder parent) {
        super(folderName, parent);
    }
    
    @Override
    public synchronized IFile addFile(final String fileName) {
        File file = new File(fileName, this);
        // Calculate local path for entity
        file.setLocalPath(getLocalPath() + java.io.File.separator + file.getName());
        childrens.add(file);
        return file;
    }
    
    @Override
    public synchronized IFolder addFolder(final String folderName) {
        Folder folder = new Folder(folderName, this);
        // Calculate local path for entity
        folder.setLocalPath(getLocalPath() + java.io.File.separator + folder.getName());
        childrens.add(folder);
        return folder;
    }

    /**
     * Remove entity for the given name in a folder
     * 
     * @param the given name of entity
     */
    public void remove(final String entityName) {
        Iterator<Entity> iterator = childrens.iterator();
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
    public synchronized List<IEntity> getChildren() {
    	List<IEntity> children = new ArrayList<IEntity>();
    	children.addAll(childrens);
        return children;
    }
    
    /**
     * Get a entity for the given name in a folder
     * 
     * @param name
     * @return
     */
    @Override
    public IEntity getChildren(final String entityName) {
        for(Entity entity : childrens) {
            if(entity.getName().equals(entityName))
                return entity;
        }
        return null;
    }
    
    @Override
    public void moveTo(final IFolder target) {
        super.moveTo(target);
    }
    
    @Override
    public IEntity copyTo(final IFolder target, final String targetName) {
    	Folder folder = (Folder) target.addFolder(targetName);
        for(Entity entity : childrens) {
            entity.copyTo(folder, entity.getName());
        }
        return folder;
    }
}
