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
package io.milton.s3.model;

public class File extends BaseEntity implements IFile {
    
    private byte[] bytes;
    private String contentType;
    
    /**
     * Size of file
     */
    private long size;

    public File(String fileName, IFolder parent) {
        super(fileName, parent);
    }
    
    public File(String fileName, IFolder parent, byte[] bytes) {
        super(fileName, parent);
        this.bytes = bytes;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public IEntity copyTo(final IFolder target, final String targetName) {
        File file = (File) ((Folder) target).addFile(targetName);
        file.bytes = bytes;
        file.contentType = contentType;
        file.size = size;
        return file;
    }
}
