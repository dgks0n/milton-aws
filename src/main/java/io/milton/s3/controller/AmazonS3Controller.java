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
package io.milton.s3.controller;

import io.milton.annotations.ChildrenOf;
import io.milton.annotations.ContentLength;
import io.milton.annotations.ContentType;
import io.milton.annotations.Copy;
import io.milton.annotations.CreatedDate;
import io.milton.annotations.Delete;
import io.milton.annotations.DisplayName;
import io.milton.annotations.Get;
import io.milton.annotations.MakeCollection;
import io.milton.annotations.ModifiedDate;
import io.milton.annotations.Move;
import io.milton.annotations.Name;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;
import io.milton.annotations.UniqueId;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.service.AmazonStorageService;
import io.milton.s3.service.AmazonStorageServiceImpl;
import io.milton.s3.util.DateUtils;

import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

@ResourceController
public class AmazonS3Controller {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonS3Controller.class);
    
    private static final String BUCKET_NAME = "milton-s3-demo";
    
    private final Region region = Region.getRegion(Regions.US_WEST_2);
    
    private final AmazonStorageService amazonStorageService;
    
    /**
	 * Initialize Amazon Simple Storage Service environment for the given
	 * repository
	 * 
	 */
    public AmazonS3Controller() throws Exception {
    	amazonStorageService = new AmazonStorageServiceImpl(region);
    	amazonStorageService.createBucket(BUCKET_NAME);
    }
    
    /**
     * Return the root folder. Also annotated for Milton to use
     * as a root.
     * 
     * @return Folder
     * @throws Exception 
     */
    @Root
    public Folder getRootFolder() throws Exception {
		LOG.info("Getting root folder [/] and create if it is not exist in the table "
				+ BUCKET_NAME);
        return amazonStorageService.findRootFolder(BUCKET_NAME);
    }
    
    @ChildrenOf
    public AmazonS3Controller getChildren(AmazonS3Controller rootFolder) {
        return this;
    }
    
    /**
     * Get all of my children, whether they are folders or files.
     * 
     * @param folder
     * @return
     * @throws ParseException 
     */
    @ChildrenOf
    public List<Entity> getChildren(Folder parent) {
        if (parent == null) {
        	return Collections.emptyList();
        }
        
        // Get all entities form Amazon DynamoDB
        List<Entity> children = amazonStorageService.findEntityByParent(BUCKET_NAME, parent);
        LOG.info("Listing collection of folder " + parent.getName() + ": "
                + children.size() + " items");
        return children;
    }
    
    @MakeCollection
    public Folder createFolder(Folder parent, String folderName) {
        LOG.info("Creating folder " + folderName + " in " + parent.getName());
        
        // Create new folder for the given name & store in the Amazon DynamoDB
        Folder newFolder = (Folder) parent.addFolder(folderName);
        if (amazonStorageService.putEntity(BUCKET_NAME, newFolder, null)) {
        	LOG.info("Create Successful folder " + folderName + " in " + parent.getName());
        }
        return newFolder;
    }
    
    @Name
    public String getResourceName(Entity entity) {
        return entity.getName();
    }
    
    @DisplayName
    public String getDisplayName(Entity entity) {
        return entity.getName();
    }
    
    @PutChild
    public File createFile(Folder parent, String newName, InputStream inputStream, Long contentLength, 
    		String contentType) {
		LOG.info("Creating file " + inputStream.toString() + " with name "
				+ newName + " in the folder " + parent.getName());
        
        // Create a file and store into Amazon Simple Storage Service
        File newFile = parent.addFile(newName);
        newFile.setSize(contentLength);
        // Get default content type if cannot get via milton
        if (StringUtils.isEmpty(contentType)) {
        	contentType = new MimetypesFileTypeMap(inputStream).getContentType(newName);
        }
        newFile.setContentType(contentType);
        
		LOG.info("Created new file " + newName + " [name=" + newName
				+ ", contentLength=" + contentLength + ", contentType="
				+ contentType + "]");
        boolean isSuccess = amazonStorageService.putEntity(BUCKET_NAME, newFile, inputStream);
        if (isSuccess) {
        	LOG.warn("Create Successful file " + newName + " under folder " + parent);
        }
        return newFile;
    }
    
    @Move
    public void renameOrMoveEntity(Entity entity, Folder newParent, String newName) {
        boolean isRenamingAction = true;
        if (!entity.getParent().equals(newParent)) {
            isRenamingAction = false;
            // Current action is moving file (not renaming)
            LOG.info("Moving file " + entity.getName() + " from folder "
                    + entity.getParent().getName() + " to " + newName + " in folder " + newParent.getName());
        } else {
            LOG.info("Renaming file " + entity.getName() + " to " + newName
                    + " in folder " + entity.getParent().getName());
        }
        boolean isSuccess = amazonStorageService.updateEntityByUniqueId(BUCKET_NAME, entity, newParent, 
                newName, isRenamingAction);
        if (isSuccess) {
            LOG.info("Succesful by updating or moving file " + entity.getName() + " to " 
                    + newName + " in " + newParent.getName());
        }
    }
    
    @Copy
    public void copyFile(Entity entity, Folder newParent, String newName) {
		LOG.info("Copying file " + entity.getName() + " from folder " + entity.getParent().getName() 
		        + " to folder " + newParent.getName());
		
		amazonStorageService.copyEntityByUniqueId(BUCKET_NAME, entity, newParent, null, newName);
    }
    
    @ContentLength
    public Long getContentLength() {
        return 0L;
    }
    
    @ContentLength
    public Long getContentLength(Entity entity) {
    	long contentLength = 0L;
        if (entity instanceof Folder) {
            LOG.warn("Could not get content length of folder " + entity.getName());
            return contentLength;
        }
        
        contentLength = ((File) entity).getSize();
        LOG.info("Getting the content length for the source object " + entity.getName()
        		+ ": " + contentLength);
        return contentLength;
    }
    
    @ContentType
    public String getContentType(Entity entity) {
    	String contentType = "";
        if (entity instanceof File) {
        	contentType = ((File) entity).getContentType();
        	LOG.info("Getting the content type for the source object " + entity.getName()
        			+ ": " + contentType);
            return contentType;
        }
        return contentType;
    }
    
    @CreatedDate
    public Date getCreatedDate(Entity entity) {
    	Date createdDate = entity.getCreatedDate();
        LOG.info("Getting the created date for the source object "
                + entity.getName() + ": " + DateUtils.dateToString(createdDate));
        return createdDate;
    }
    
    @ModifiedDate
    public Date getModifiedDate(Entity entity) {
    	Date modifiedDate = entity.getModifiedDate();
        LOG.info("Getting the modified date for the source object "
                + entity.getName() + ": " + DateUtils.dateToString(modifiedDate));
        return modifiedDate;
    }
    
    @UniqueId
    public String getUniqueId(Entity entity) {
    	String uniqueId = entity.getId().toString();
    	LOG.info("Getting the unique UUID for the source object " + entity.getName() 
    			+ ": " + uniqueId);
        return uniqueId;
    }
    
    @Get
    public InputStream downloadFile(File file) {
		String keyName = file.getParent().getId().toString()
				+ java.io.File.separatorChar + file.getId().toString();
        LOG.info("Downloading file " + file.toString() + " under folder "
                + file.getParent().getName());
    	return amazonStorageService.downloadEntityByUniqueId(BUCKET_NAME, keyName);
    }
    
    @Delete
    public void deleteFileOrFolder(Entity entity) {
        LOG.info("Deleting the entity " + entity.getName() + " in the " + BUCKET_NAME + " in Amazon S3");
        boolean isDeleted = amazonStorageService.deleteEntityByUniqueId(BUCKET_NAME, 
                entity.getId().toString());
        if (!isDeleted) {
            LOG.warn("Could not delete the entity " + entity.getName() + " in the " + BUCKET_NAME);
            return;
        }
        LOG.info("The entity " + entity.getName() + " was deleted");
    }
}
