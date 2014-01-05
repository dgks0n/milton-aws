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
package io.milton.s3.dao;

import io.milton.s3.model.Folder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DynamoDBRepository.class);
    
    public static Folder getRoot() {
        Folder root = new Folder("/", null);
        
        // TODO: Only for test
        Folder java = root.addFolder("Java").addFolder("Java01");
        java.addFolder("Java02").addFile("test.txt");
        java.addFolder("Java03");
        
        root.addFolder("PHP").addFolder("Work");
        
        return root;
    }
}
