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
import io.milton.annotations.PutChild;
import io.milton.annotations.ResourceController;
import io.milton.annotations.Root;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ResourceController
public class StorageController {
    
    private static final Logger LOG = LoggerFactory.getLogger(StorageController.class);
    
    private List<Product> products = new ArrayList<Product>();

    public StorageController() {
        products.add(new Product("hello"));
        products.add(new Product("world"));
    }
            
    @Root
    public StorageController getRoot() {
        return this;
    }    
    
    @ChildrenOf
    public List<Product> getProducts(StorageController root) {
        return products;
    }
    
    @ChildrenOf
    public List<ProductFile> getProductFiles(Product product) {
        return product.getProductFiles();
    }
    
    @PutChild
    public ProductFile upload(Product product, String newName, byte[] bytes) {
        ProductFile pf = new ProductFile(newName, bytes);
        product.getProductFiles().add(pf);
        return pf;
    }
    
    public class Product {
        private String name;
        private List<ProductFile> productFiles = new ArrayList<ProductFile>();

        public Product(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }             

        public List<ProductFile> getProductFiles() {
            return productFiles;
        }                
    }
    
    public class ProductFile {
        private String name;
        private byte[] bytes;

        public ProductFile(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        public String getName() {
            return name;
        }                
    }
}
