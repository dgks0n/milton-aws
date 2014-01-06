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
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;
import io.milton.s3.dao.DynamoDBRepository;
import io.milton.s3.model.Folder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResourceController
public class AWSRootController {

    private static final Logger LOG = LoggerFactory.getLogger(AWSRootController.class);
    
    /**
     * Return the root folder. Also annotated for Milton to use
     * as a root.
     * 
     * @return
     * @throws Exception 
     */
    @Root
    public Folder getRootFolder() throws Exception {
        LOG.info("Getting root folder...");
        
        return DynamoDBRepository.getRootFolder();
    }
    
    @ChildrenOf
    public AWSRootController children(AWSRootController rootFolder) {
        return this;
    }
}
