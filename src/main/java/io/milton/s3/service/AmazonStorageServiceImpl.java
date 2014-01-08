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
import io.milton.s3.model.Folder;
import io.milton.s3.model.IEntity;
import io.milton.s3.model.IFile;
import io.milton.s3.model.IFolder;
import io.milton.s3.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class AmazonStorageServiceImpl implements AmazonStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonStorageServiceImpl.class);
    
    private final DynamoDBManager dynamoDBManager;
    private final AmazonS3Manager amazonS3Manager;
    
    public AmazonStorageServiceImpl(String repository) {
        dynamoDBManager = new DynamoDBManagerImpl(Region.getRegion(Regions.US_WEST_2), repository);
        amazonS3Manager = new AmazonS3ManagerImpl(Regions.US_WEST_2.getName(), repository);
    }
    
    /* (non-Javadoc)
     * @see io.milton.s3.service.AmazonStorageService#findRootFolder()
     */
    @Override
    public IFolder findRootFolder() {
        Folder rootFolder = (Folder) dynamoDBManager.findRootFolder();
        if (rootFolder == null) {
            rootFolder = new Folder("/", null);
            
            // Tries to create new file in the system for the given UUID
            String fileName = rootFolder.getId().toString();
            amazonS3Manager.uploadEntity(fileName, FileUtils.createNewFile(fileName));
            dynamoDBManager.putEntity(rootFolder);
            return rootFolder;
        }
        
        // TODO: Is it necessary to find entity from Amazon S3 ?
        // 
        return rootFolder;
    }

    /* (non-Javadoc)
     * @see io.milton.s3.service.AmazonStorageService#findEntityByUniqueId(io.milton.s3.model.IEntity)
     */
    @Override
    public IEntity findEntityByUniqueId(IEntity entity) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see io.milton.s3.service.AmazonStorageService#findEntityByParent(io.milton.s3.model.Folder)
     */
    @Override
    public List<Entity> findEntityByParent(Folder parent) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see io.milton.s3.service.AmazonStorageService#updateEntityByUniqueId(io.milton.s3.model.IFile, io.milton.s3.model.IFolder, java.lang.String, boolean)
     */
    @Override
    public boolean updateEntityByUniqueId(IFile file, IFolder newParent,
            String newEntityName, boolean isRenaming) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see io.milton.s3.service.AmazonStorageService#deleteEntityByUniqueId(java.lang.String)
     */
    @Override
    public boolean deleteEntityByUniqueId(String uniqueId) {
        // TODO Auto-generated method stub
        return false;
    }

}
