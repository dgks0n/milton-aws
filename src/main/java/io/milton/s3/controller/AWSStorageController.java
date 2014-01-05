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
import io.milton.s3.model.BaseEntity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResourceController
public class AWSStorageController {

    private static final Logger LOG = LoggerFactory.getLogger(AWSStorageController.class);
    
    /**
     * Get all of my children, whether they are folders or files.
     * 
     * @param folder
     * @return
     */
    @ChildrenOf
    public List<BaseEntity> children(Folder folder) {
        if (folder == null)
            return null;
        
        List<BaseEntity> children = folder.getChildren();
        LOG.info("Getting childrens of " + folder.getName());
        LOG.info("Returning " + children.size() + " children");
        return children;
    }
    
    @MakeCollection
    public Folder createFolder(Folder parent, String folderName) {
        folderName = UUID.randomUUID().toString();
        LOG.info("Creating folder " + folderName + " in " + parent.getName());
        
        return parent.addFolder(folderName);
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
        LOG.info("Moving " + file.getName() + " to " + newName + " in " + newParent.getName());
        
        if (file.getParent() != newParent) {
            newParent.getChildren().add(file);
            file.setParent(newParent);
        }
        
        file.setName(newName);
    }
    
    @Copy
    public void copy(File file, Folder newParent, String newName) {
        LOG.info("Copying " + file.getName() + " to " + newName + " in " + newParent.getName());
        
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
        return new Long(file.getBytes().length);
    }
}
