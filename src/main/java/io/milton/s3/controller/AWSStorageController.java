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
import io.milton.annotations.MakeCollection;
import io.milton.annotations.Move;
import io.milton.annotations.Name;
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;
import io.milton.s3.db.DynamoDBStore;
import io.milton.s3.model.BaseEntity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;
import io.milton.s3.model.IEntity;
import io.milton.s3.util.Crypt;
import io.milton.s3.util.DynamoDBTable;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

@ResourceController
public class AWSStorageController {

    private static final Logger LOG = LoggerFactory.getLogger(AWSStorageController.class);
    
    private static final String NOT_EXIST = "NONE";
    
    private static final String DYNAMODB_TABLE_NAME = "milton-s3-demo";
    
    /**
     * Amazon DynamoDB Storage
     */
    private DynamoDBStore dynamoDBStore = new DynamoDBStore();
    
    /**
     * Initialize Amazon DynamoDB environment
     * 
     * @throws Exception
     */
    protected void openDynamoDBStorage(String tableName) throws Exception {
        LOG.info("Creating table " + tableName + " in Amazon DynamoDB");
        
        dynamoDBStore.openDynamoDB();
        dynamoDBStore.createTable(tableName);
        dynamoDBStore.describeTable(tableName);
    }
    
    /**
     * Return the root folder. Also annotated for Milton to use
     * as a root.
     * 
     * @return
     * @throws Exception 
     */
    @Root
    public Folder getRootFolder() throws Exception {
        
        openDynamoDBStorage(DYNAMODB_TABLE_NAME);
        // Root folder
        Folder rootFolder = new Folder("/", null);
        
        LOG.info("Getting root folder and create if it is not exist..");
        String uniqueId = Crypt.toHexFromText(rootFolder.getName());
        Condition condition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue(uniqueId));
        
        // Create Root folder if it is not exist
        boolean isRootFolderCreated = dynamoDBStore.isRootFolderCreated(DYNAMODB_TABLE_NAME, DynamoDBTable.UUID, condition);
        if (!isRootFolderCreated) {
            Map<String, AttributeValue> item = dynamoDBStore.newItem(uniqueId, rootFolder.getName(), NOT_EXIST, NOT_EXIST, 0, 
                    NOT_EXIST, rootFolder.getCreatedDate(), rootFolder.getModifiedDate());
            
            // Store in the Amazon DynamoDB
            dynamoDBStore.putItem(DYNAMODB_TABLE_NAME, item);
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
    public List<BaseEntity> getChildren(Folder parent) {
        if (parent == null)
            return Collections.emptyList();
        
        // Get UUID depends on parent folder name
        String parentId = Crypt.toHexFromText(parent.getName());
        Condition condition = new Condition().withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue(parentId));
        
        // Get all entities form Amazon DynamoDB
        List<BaseEntity> children = dynamoDBStore.getChildren(parent, DYNAMODB_TABLE_NAME, DynamoDBTable.PARENT_UUID, condition);
        LOG.info("Getting childrens of " + parent.getName() + "; Returning " + children.size() + " children of " + parent.getName());
        return children;
    }
    
    @MakeCollection
    public Folder createFolder(Folder parent, String folderName) {
        LOG.info("Creating folder " + folderName + " in " + parent.getName());
        
        // Create new folder for the given name
        Folder newFolder = parent.addFolder(folderName);
        
        // Get UUID depends on folder name
        String uniqueId = Crypt.toHexFromText(folderName);
        String parentId = Crypt.toHexFromText(parent.getName());
        Map<String, AttributeValue> item = dynamoDBStore.newItem(uniqueId, folderName, parentId, NOT_EXIST, 0,
                NOT_EXIST, newFolder.getCreatedDate(), newFolder.getModifiedDate());
        
        // Store in the Amazon DynamoDB
        dynamoDBStore.putItem(DYNAMODB_TABLE_NAME, item);
        return newFolder;
    }
    
    @Name
    public String getResource(BaseEntity entity) {
        return entity.getName();
    }
    
    @DisplayName
    public String getDisplayName(BaseEntity entity) {
        return entity.getName();
    }
    
    @PutChild
    public File createFile(Folder parent, String newName, byte[] bytes) {
        LOG.info("Creating file with Name: " + newName + "; Size of upload: "
                + bytes.length + " in the folder " + parent.getName());
        
        File file = parent.addFile(newName);
        file.setBytes(bytes);
        return file;
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
}
