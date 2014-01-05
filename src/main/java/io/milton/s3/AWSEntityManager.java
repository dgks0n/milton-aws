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

import java.io.File;
import java.io.InputStream;

public interface AWSEntityManager {

    public boolean isRootFolder(String folerName);
    
    public void createFolder(String folerName);

    public void deleteFolder(String folerName);

    public void uploadFile(String folderName, String fileName, File file);

    public void deleteFile(String folderName, String fileName);
    
    public void makePublic(String folderName, String fileName);

    public boolean isFilePublic(String folderName, String key);

    public boolean downloadFile(String folderName, String keyNotAvailable, File outputFile);

    public void uploadFile(String folderName, String keyName, InputStream fileStream);

    public InputStream downloadFile(String folderName, String keyName);

    public String getFileUrl(String folderName, String keyName);
}
