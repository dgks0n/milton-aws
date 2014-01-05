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
package io.milton.s3;

import io.milton.annotations.ChildrenOf;
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
import io.milton.s3.model.BaseEntity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResourceController
public class StorageController {
    
    private static final Logger LOG = LoggerFactory.getLogger(StorageController.class);
    
    private Folder root;
    
    public StorageController() {
        if (root == null)
            root = new Folder(null, null);
        
        root.addFolder("java");
        root.addFolder("php");
    }
    
    @Root
    public StorageController getRoot() {
        return this;
    }    
    
    @Name
    public String getResource(BaseEntity item) {
        return item.getName();
    }
    
    @ChildrenOf
    public List<BaseEntity> children(StorageController storage) {
        return root.getChildren();
    }
            
    @ChildrenOf
    public List<BaseEntity> children(Folder folder) {
        return folder.getChildren();
    }
    
    @MakeCollection
    public Folder createFolder(Folder parent, String name) {
        return parent.addFolder(name);
    }
    
    @PutChild
    public File createFile(Folder parent, String name, byte[] bytes) {
        File file = parent.addFile(name);
        file.setBytes(bytes);
        return file;
    }
    
    @Get
    public byte[] render(Folder item) throws UnsupportedEncodingException {
        return "<html>\n<body><h1>Hello World</h1></body></html>".getBytes("UTF-8");        
    }
    
    @Get
    public void writeContent(File file, OutputStream out) throws UnsupportedEncodingException, IOException {
        out.write(file.getBytes());
    }    
    
    @Move
    public void move(BaseEntity source, Folder target, String targetName) {
        source.moveTo(target);
        source.setName(targetName);
    }
    
    @Copy
    public void copy(BaseEntity source, Folder target, String targetName) {
        source.copyTo(target, targetName);
    }    
    
    @Delete
    public void delete(BaseEntity source) {
        source.delete();
    }
    
    @DisplayName
    public String getDisplayName(BaseEntity source) {
        return "Hello " + source.getName();
    }
    
    @UniqueId
    public String getUniqueId(BaseEntity source) {
        return source.getId().toString();
    }    
    
    @ModifiedDate
    public Date getModifiedDate(BaseEntity source) {
        return source.getModifiedDate();
    }
    
    @CreatedDate
    public Date getCreatedDate(BaseEntity source) {
        return source.getCreatedDate();
    }
}
