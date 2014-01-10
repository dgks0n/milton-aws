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

import io.milton.s3.model.Entity;
import io.milton.s3.model.File;
import io.milton.s3.model.Folder;

import java.util.List;

public interface DynamoDBManager {

    boolean isExistEntity(String entityName, Folder parent);
    
    boolean putEntity(Entity entity);
    
    Folder findRootFolder();
    
    Entity findEntityByUniqueId(Entity entity);
    
    Entity findEntityByUniqueId(String uniqueId, Folder parent);
    
    List<Entity> findEntityByParent(Folder parent);
    
    boolean updateEntityByUniqueId(File file, Folder newParent, String newEntityName, 
    		boolean isRenaming);
    
    boolean deleteEntityByUniqueId(String uniqueId);
    
    /**
	 * Delete storage database in Amazon DynamoDB for the given table name
	 * 
	 */
    void deleteTable();
}
