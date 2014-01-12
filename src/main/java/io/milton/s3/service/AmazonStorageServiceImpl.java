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

import io.milton.s3.AmazonS3Manager;
import io.milton.s3.AmazonS3ManagerImpl;
import io.milton.s3.DynamoDBManager;
import io.milton.s3.DynamoDBManagerImpl;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class AmazonStorageServiceImpl implements AmazonStorageService {
	
    /**
     * Amazon DynamoDB Storage
     */
    private final DynamoDBManager dynamoDBManager;
    
    /**
     * Amazon Simple Storage Service (S3)
     */
    private final AmazonS3Manager amazonS3Manager;
    
    public AmazonStorageServiceImpl(Region region) {
        dynamoDBManager = new DynamoDBManagerImpl(region);
        amazonS3Manager = new AmazonS3ManagerImpl(region.getName());
    }
    
    @Override
    public Bucket createBucket(String bucketName) {
        Bucket bucket = amazonS3Manager.createBucket(bucketName);
        if (bucket != null) {
            dynamoDBManager.createTable(bucketName);
            return bucket;
        }
        return null;
    }
    
    @Override
	public void deleteBucket(String bucketName) {
    	// Deletes the specified bucket in Amazon S3
    	if (amazonS3Manager.deleteBucket(bucketName)) {
    		dynamoDBManager.deleteTable(bucketName);
    	}
	}
    
    @Override
    public Folder findRootFolder(String bucketName) {
        Folder rootFolder = (Folder) dynamoDBManager.findRootFolder(bucketName);
        if (rootFolder == null) {
            rootFolder = new Folder("/", null);
            
            // Tries to create new folder for the given UUID
            // if it does not exist in Amazon S3
            dynamoDBManager.putEntity(bucketName, rootFolder);
        }
        return rootFolder;
    }

    @Override
    public Entity findEntityByUniqueId(String bucketName, Entity entity) {
        if (entity == null)
        	return null;
        
        S3Object s3Object = amazonS3Manager.findEntityByUniqueKey(bucketName, entity.getId().toString());
        if (s3Object == null)
        	return null;
        return dynamoDBManager.findEntityByUniqueId(bucketName, entity);
    }

    @Override
    public List<Entity> findEntityByParent(String bucketName, Folder parent) {
    	if (parent == null) {
    		return Collections.emptyList();
    	}
    	
    	// Get all files of current folder have already existing in Amazon S3
    	List<S3ObjectSummary> objectSummaries = amazonS3Manager.findEntityByPrefixKey(bucketName, 
    	        parent.getId().toString());
    	List<Entity> children = new ArrayList<Entity>();
    	for (S3ObjectSummary objectSummary : objectSummaries) {
    	    String uniqueId = objectSummary.getKey();
    	    
    	    // Search by only unique UUID of entity
    	    uniqueId = uniqueId.substring(uniqueId.indexOf("/") + 1);
    		File file = (File) dynamoDBManager.findEntityByUniqueId(bucketName, uniqueId, parent);
    		if (file != null) {
    		    file.setSize(objectSummary.getSize());
    		    children.add(file);
    		}
    	}
    	
    	// Get all folders of current folder have already existing in Amazon DynamoDB
    	List<Entity> folders = dynamoDBManager.findEntityByParentAndType(bucketName, parent, true);
    	if (folders != null && !folders.isEmpty()) {
    		for (Entity folder : folders) {
    			if (!children.contains(folder)) {
    				children.add(folder);
    			}
    		}
    	}
        return children;
    }
    
    @Override
	public boolean putEntity(String bucketName, Entity entity, InputStream inputStream) {
    	if (entity == null) {
    		return false;
    	}
    	
    	if (entity instanceof File) {
    	    String keyName = getAmazonS3UniqueKey(entity);
    	    // Only store file in Amazon S3
    		amazonS3Manager.uploadEntity(bucketName, keyName, inputStream);
    	}
    	
    	// Store folder as hierarchy in Amazon DynamoDB
    	return dynamoDBManager.putEntity(bucketName, entity);
	}
    
    @Override
    public void copyEntityByUniqueId(String bucketName, Entity entity, Folder newParent, 
            String newBucketName, String newName) {
        String sourceKeyName = getAmazonS3UniqueKey(entity);
        String targetKeyName = newParent.getId().toString()
                + java.io.File.separatorChar + entity.getId().toString();
        
        // Copies a source object to a new destination in Amazon S3
        boolean isSuccess = amazonS3Manager.copyEntity(bucketName, sourceKeyName, newBucketName, 
                targetKeyName);
        if (isSuccess) {
            entity.setParent(newParent);
            entity.setName(newName);
            
            // Store folder as hierarchy in Amazon DynamoDB
            dynamoDBManager.putEntity(bucketName, entity);
        }
    }

    @Override
    public boolean updateEntityByUniqueId(String bucketName, Entity entity, Folder newParent, 
            String newEntityName, boolean isRenamingAction) {
        if (!isRenamingAction) {
            String sourceKeyName = getAmazonS3UniqueKey(entity);
            String destinationKeyName = newParent.getId().toString()
                    + java.io.File.separatorChar + entity.getId().toString();
            
            // We must update entity in S3 because action is moving file
            boolean isSuccess = amazonS3Manager.copyEntity(bucketName, sourceKeyName, null, 
                    destinationKeyName);
            if (isSuccess == false) {
                return false;
            }
            
            // Remove old entity after moved
            amazonS3Manager.deleteEntity(bucketName, sourceKeyName);
        }
        
        // Update stored entity in DynamoDB
        return dynamoDBManager.updateEntityByUniqueId(bucketName, entity, newParent,
                newEntityName, isRenamingAction);
    }
    
    @Override
    public boolean deleteEntityByUniqueId(String bucketName, String uniqueId) {
        if (StringUtils.isEmpty(uniqueId))
        	return false;
        
        amazonS3Manager.deleteEntity(bucketName, uniqueId);
        dynamoDBManager.deleteEntityByUniqueId(bucketName, uniqueId);
        return true;
    }

	@Override
	public boolean downloadEntityByUniqueId(String bucketName, String keyNotAvailable, 
	        java.io.File destinationFile) {
		return amazonS3Manager.downloadEntity(bucketName, keyNotAvailable, destinationFile);
	}

	@Override
	public InputStream downloadEntityByUniqueId(String bucketName, String keyName) {
		return amazonS3Manager.downloadEntity(bucketName, keyName);
	}

	private String getAmazonS3UniqueKey(Entity entity) {
        String keyName = null;
        if (entity.getParent() == null) {
            keyName = entity.getId().toString();
        } else {
            keyName = entity.getParent().getId().toString()
                    + java.io.File.separatorChar + entity.getId().toString();
        }
        return keyName;
    }
	
}
