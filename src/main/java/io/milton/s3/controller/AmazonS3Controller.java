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
import io.milton.annotations.Copy;
import io.milton.annotations.DisplayName;
import io.milton.annotations.Get;
import io.milton.annotations.MakeCollection;
import io.milton.annotations.Move;
import io.milton.annotations.Name;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.service.AmazonStorageService;
import io.milton.s3.service.AmazonStorageServiceImpl;

import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResourceController
public class AmazonS3Controller {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonS3Controller.class);
    
    private static final String AMAZON_STORAGE_NAME = "milton-s3-demo";
    
    private final AmazonStorageService amazonStorageService;
    
    /**
	 * Initialize Amazon DynamoDB environment for the default table.
	 * 
	 * @throws Exception
	 */
    public AmazonS3Controller() throws Exception {
        this(AMAZON_STORAGE_NAME);
    }
    
    /**
	 * Initialize Amazon Simple Storage Service environment for the given
	 * repository
	 * 
	 * @param repository
	 *            - the table name
	 * @throws Exception
	 */
    public AmazonS3Controller(String repository) throws Exception {
    	amazonStorageService = new AmazonStorageServiceImpl(repository);
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
				+ AMAZON_STORAGE_NAME);
        return amazonStorageService.findRootFolder();
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
        List<Entity> children = amazonStorageService.findEntityByParent(parent);
        LOG.info("Getting childrens of " + parent.getName() + "; Returning "
                + children.size() + " items");
        return children;
    }
    
    @MakeCollection
    public Folder createFolder(Folder parent, String folderName) {
        LOG.info("Creating folder " + folderName + " in " + parent.getName());
        
        // Create new folder for the given name & store in the Amazon DynamoDB
        Folder newFolder = (Folder) parent.addFolder(folderName);
        if (amazonStorageService.putEntity(newFolder, null)) {
        	LOG.info("Create Successful folder " + folderName + " in " + parent.getName());
        }
        return newFolder;
    }
    
    @Name
    public String getResource(Entity entity) {
        return entity.getName();
    }
    
    @DisplayName
    public String getDisplayName(Entity entity) {
        return entity.getName();
    }
    
    @PutChild
    public File createFile(Folder parent, String newName, InputStream inputStream) {
		LOG.info("Creating file " + inputStream.toString() + " with name "
				+ newName + " in the folder " + parent.getName());
        
        // Create a file and store into Amazon Simple Storage Service
        File newFile = parent.addFile(newName);
        boolean isSuccess = amazonStorageService.putEntity(newFile, inputStream);
        if (isSuccess) {
        	LOG.warn("Create Successful file " + newName + " under folder " + parent);
        }
        return newFile;
    }
    
    @Move
    public void renameOrMoveFile(File file, Folder newParent, String newName) {
        LOG.info("Moving or renaming file " + file.getName() + " from folder "
                + file.getParent().getName() + " to " + newName + " in folder "
                + newParent.getName());
		
        boolean isRenaming = true;
        if (file.getParent() != newParent) {
            file.setParent(newParent);
            // Action is moving file
            isRenaming = false;
        }
        amazonStorageService.updateEntityByUniqueId(file, newParent, newName, isRenaming);
        LOG.info("Succesful by updating or moving file " + file.getName()
                + " to " + newName + " in " + newParent.getName());
    }
    
    @Copy
    public void copyFile(File file, Folder newParent, String newName) {
		LOG.info("Copying file " + file.getName() + " to " + newName + " in "
				+ newParent.getName());
		amazonStorageService.copyEntityByUniqueId(file, newParent, null, newName);
    }
    
    @ContentLength
    public Long rootContentLength() {
        return 0L;
    }
    
    @ContentLength
    public Long folderContentLength(Folder folder) {
        return 0L;
    }
    
    @Get
    public InputStream downloadFile(File file) {
		String keyName = file.getParent().getId().toString()
				+ java.io.File.separatorChar + file.getId().toString();
		LOG.info("Key name: " + keyName);
        LOG.info("Downloading file " + file.toString() + " under folder " + file.getParent().getName());
    	return amazonStorageService.downloadEntityByUniqueId(keyName);
    }
}
