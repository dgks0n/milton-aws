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
package io.milton.s3.service;

import io.milton.s3.model.Entity;
import io.milton.s3.model.Folder;

import java.io.InputStream;
import java.util.List;

public interface AmazonStorageService {
	
	/**
	 * Remove storage database in Amazon S3
	 * 
	 */
	void deleteBucket();
	
    Folder findRootFolder();
    
    Entity findEntityByUniqueId(Entity entity);
    
    List<Entity> findEntityByParent(Folder parent);
    
    boolean putEntity(Entity entity, InputStream inputStream);
    
    void copyEntityByUniqueId(Entity entity, Folder newParent, String newBucketName, String newName);
    
    boolean updateEntityByUniqueId(Entity entity, Folder newParent, String newEntityName, 
    		boolean isRenaming);
    
    boolean deleteEntityByUniqueId(String uniqueId);
    
    boolean downloadEntityByUniqueId(String keyNotAvailable, java.io.File destinationFile);
    
    InputStream downloadEntityByUniqueId(String keyName);
}
