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

import com.amazonaws.services.s3.model.Bucket;

public interface AmazonStorageService {
	
    /**
     * Create and name a bucket that stores data. Buckets are the fundamental
     * container in Amazon S3 for data storage
     * 
     * @param bucketName
     *              - the bucket name
     */
    Bucket createBucket(String bucketName);
    
	/**
	 * Remove storage database in Amazon S3
	 * 
	 * @param bucketName
	 *             - the storage database name
	 */
	void deleteBucket(String storageName);
	
    Folder findRootFolder(String bucketName);
    
    Entity findEntityByUniqueId(String bucketName, Entity entity);
    
    List<Entity> findEntityByParent(String bucketName, Folder parent);
    
    boolean putEntity(String bucketName, Entity entity, InputStream inputStream);
    
    void copyEntityByUniqueId(String bucketName, Entity entity, Folder newParent, 
            String newBucketName, String newName);
    
    boolean updateEntityByUniqueId(String bucketName, Entity entity, Folder newParent, 
            String newEntityName, boolean isRenamingAction);
    
    boolean deleteEntityByUniqueId(String bucketName, String uniqueId);
    
    boolean downloadEntityByUniqueId(String bucketName, String keyNotAvailable, java.io.File destinationFile);
    
    InputStream downloadEntityByUniqueId(String bucketName, String keyName);
}
