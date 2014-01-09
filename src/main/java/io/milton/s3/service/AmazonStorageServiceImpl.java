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
import com.amazonaws.regions.Regions;
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
    
    public AmazonStorageServiceImpl(String repository) {
        dynamoDBManager = new DynamoDBManagerImpl(Region.getRegion(Regions.US_WEST_2), 
        		repository);
        amazonS3Manager = new AmazonS3ManagerImpl(Regions.US_WEST_2.getName(), repository);
    }
    
    @Override
    public Folder findRootFolder() {
        Folder rootFolder = (Folder) dynamoDBManager.findRootFolder();
        if (rootFolder == null) {
            rootFolder = new Folder("/", null);
            
            // Tries to create new folder for the given UUID
            dynamoDBManager.putEntity(rootFolder);
            return rootFolder;
        }
        return rootFolder;
    }

    @Override
    public Entity findEntityByUniqueId(Entity entity) {
        if (entity == null)
        	return null;
        
        S3Object s3Object = amazonS3Manager.findEntityByUniqueKey(entity.getId().toString());
        if (s3Object == null)
        	return null;
        return dynamoDBManager.findEntityByUniqueId(entity);
    }

    @Override
    public List<Entity> findEntityByParent(Folder parent) {
    	if (parent == null) {
    		return Collections.emptyList();
    	}
    	
    	// Get all files of current folder have already existing in Amazon S3
    	List<S3ObjectSummary> objectSummaries = amazonS3Manager.findEntityByPrefixKey(
    			parent.getId().toString());
    	List<Entity> files = new ArrayList<Entity>();
    	for (S3ObjectSummary objectSummary : objectSummaries) {
    		Entity entity = dynamoDBManager.findEntityByUniqueId(objectSummary.getKey(), parent);
    		if (entity != null) {
    			files.add(entity);
    		}
    	}
    	
    	List<Entity> children = new ArrayList<Entity>();
    	if (files != null && !files.isEmpty()) {
    		children.addAll(files);
    	}
    	
    	// Get all folders of current folder have already existing in Amazon DynamoDB
    	List<Entity> folders = dynamoDBManager.findEntityByParent(parent);
    	if (folders != null && !folders.isEmpty()) {
    		children.addAll(folders);
    	}
        return children;
    }
    
    @Override
	public boolean putEntity(Entity entity, InputStream inputStream) {
    	if (entity == null)
    		return false;
    	
    	// Only store file in Amazon S3
    	if (entity instanceof File) {
    		String keyName = null;
    		if (entity.getParent() == null) {
    			keyName = entity.getId().toString();
    		} else {
				keyName = entity.getParent().getId().toString()
						+ java.io.File.separatorChar
						+ entity.getId().toString();
    		}
    		amazonS3Manager.uploadEntity(keyName, inputStream);
    	}
    	
    	// Store folder as hierarchy in Amazon DynamoDB
    	dynamoDBManager.putEntity(entity);
    	return true;
	}

    @Override
    public boolean updateEntityByUniqueId(File file, Folder newParent, String newEntityName, 
    		boolean isRenaming) {
        return false;
    }
    
    @Override
    public boolean deleteEntityByUniqueId(String uniqueId) {
        if (StringUtils.isEmpty(uniqueId))
        	return false;
        
        amazonS3Manager.deleteEntity(uniqueId);
        dynamoDBManager.deleteEntityByUniqueId(uniqueId);
        return true;
    }

	@Override
	public boolean downloadEntityByUniqueId(String keyNotAvailable, java.io.File destinationFile) {
		return amazonS3Manager.downloadEntity(keyNotAvailable, destinationFile);
	}

	@Override
	public InputStream downloadEntityByUniqueId(String keyName) {
		return amazonS3Manager.downloadEntity(keyName);
	}

}
