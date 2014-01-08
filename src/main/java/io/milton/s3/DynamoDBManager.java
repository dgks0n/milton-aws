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

import java.util.List;

import io.milton.s3.model.Entity;
import io.milton.s3.model.Folder;
import io.milton.s3.model.IEntity;
import io.milton.s3.model.IFile;
import io.milton.s3.model.IFolder;

public interface DynamoDBManager {

    boolean putEntity(IEntity entity);
    
    IFolder findRootFolder();
    
    IEntity findEntityByUniqueId(IEntity entity);
    
    List<Entity> findEntityByParent(Folder parent);
    
    boolean updateEntityByUniqueId(IFile file, IFolder newParent, String newEntityName, boolean isRenaming);
    
    boolean deleteEntityByUniqueId(String uniqueId);
}
