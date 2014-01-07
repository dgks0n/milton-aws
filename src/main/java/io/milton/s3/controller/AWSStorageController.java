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
import io.milton.s3.db.DynamoDBService;
import io.milton.s3.db.DynamoDBServiceImpl;
import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.util.AttributeKey;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

@ResourceController
public class AWSStorageController {

    private static final Logger LOG = LoggerFactory.getLogger(AWSStorageController.class);
    
    private static final String DYNAMODB_TABLE_NAME = "milton-s3-demo";
    
    /**
     * Amazon DynamoDB Storage
     */
    private final DynamoDBService dynamoDBService;
    
    /**
     * Initialize Amazon DynamoDB environment
     * 
     * @throws Exception
     */
    public AWSStorageController() throws Exception {
        
        dynamoDBService = new DynamoDBServiceImpl(Region.getRegion(Regions.US_WEST_2), DYNAMODB_TABLE_NAME);
        if (!dynamoDBService.isTableExist()) {
            LOG.info("Creating table " + DYNAMODB_TABLE_NAME + " in Amazon DynamoDB");
            
            // Create table if it's not exist
            dynamoDBService.createTable();
            // Describe the table for the given table after created
            dynamoDBService.describeTable();
        }
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
    	LOG.info("Getting root folder [/] and create if it is not exist..");
    	
        // Root folder [/]
        Folder rootFolder = new Folder("/", null);
        rootFolder.setLocalPath("");
        
        // Create Root folder if it is not exist
        boolean isRootFolderCreated = dynamoDBService.isRootFolderCreated(rootFolder);
        if (!isRootFolderCreated) {
            Map<String, AttributeValue> item = dynamoDBService.newItem(rootFolder);
            // Store in the Amazon DynamoDB
            dynamoDBService.putItem(item);
        } else {
        	LOG.info("Root folder [/] already created in the table " + DYNAMODB_TABLE_NAME);
        }
        return rootFolder;
    }
    
    @ChildrenOf
    public AWSStorageController getChildren(AWSStorageController rootFolder) {
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
        
        Condition condition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue().withS(parent.getId()));
        
        Map<String, Condition> conditions = new HashMap<String, Condition>();
        conditions.put(AttributeKey.PARENT_UUID, condition);
        // Get all entities form Amazon DynamoDB
        List<Entity> children = dynamoDBService.getChildren(parent, conditions);
        LOG.info("Getting childrens of " + parent.getName() + "; Returning " + children.size() + " children of " + parent.getName());
        return children;
    }
    
    @MakeCollection
    public Folder createFolder(Folder parent, String folderName) {
        LOG.info("Creating folder " + folderName + " in " + parent.getName());
        
        // Create new folder for the given name
        Folder newFolder = (Folder) parent.addFolder(folderName);
        Map<String, AttributeValue> item = dynamoDBService.newItem(newFolder);
        
        // Store in the Amazon DynamoDB
        dynamoDBService.putItem(item);
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
    public File createFile(Folder parent, String newName, byte[] bytes) {
        LOG.info("Creating file with Name: " + newName + "; Size of upload: " + bytes.length
                + " in the folder " + parent.getName());
        
        // Create a file and store into Amazon DynamoDB
        File newFile = (File) parent.addFile(newName);
        newFile.setBytes(bytes);
        newFile.setSize(bytes.length);
        Map<String, AttributeValue> item = dynamoDBService.newItem(newFile);
        
        // Store in the Amazon DynamoDB
        dynamoDBService.putItem(item);
        return newFile;
    }
    
    @Move
    public void move(File file, Folder newParent, String newName) {
        LOG.info("Moving file " + file.getName() + " to " + newName + " in " + newParent.getName());
        
        if (file.getParent() != newParent) {
            newParent.getChildren().add(file);
            file.setParent(newParent);
        }
        
        file.setName(newName);
    }
    
    @Copy
    public void copy(File file, Folder newParent, String newName) {
        LOG.info("Copying file " + file.getName() + " to " + newName + " in " + newParent.getName());
        
        File copyOfFile = new File(newName, newParent, Arrays.copyOf(file.getBytes(), file.getBytes().length));
        newParent.getChildren().add(copyOfFile);
        // Get new item based on copied file and store in the Amazon DynamoDB
        Map<String, AttributeValue> item = dynamoDBService.newItem(copyOfFile);
        dynamoDBService.putItem(item);
    }
    
    @ContentLength
    public Long rootContentLength() {
        return 0L;
    }
    
    @ContentLength
    public Long folderContentLength(Folder folder) {
        return 0L;
    }
    
    @ContentLength
    public Long fileContentLength(File file) {
    	Long fileSize = new Long(file.getBytes().length);
    	
    	LOG.info("Getting size of " + file.getName() + "; Returning " + fileSize + " bytes");
        return fileSize;
    }
    
    @Get
    public InputStream getFile(File file) throws IOException {
    	return file.getInputStream();
    }
}
