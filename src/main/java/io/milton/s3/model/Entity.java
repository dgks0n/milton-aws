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

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/** Base type for files and folders stored by the adapter. */
public abstract class Entity {

  private final UUID id;
  private String name;
  private final Date createdDate;
  private Date modifiedDate;
  private Folder parent;

  public Entity(String name, Folder parent) {
    this(UUID.randomUUID(), name, new Date(), new Date(), parent);
  }

  public Entity(UUID id, String name, Date createdDate, Date modifiedDate, Folder parent) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireName(name);
    this.parent = parent;
    this.createdDate = copy(Objects.requireNonNull(createdDate, "createdDate"));
    this.modifiedDate = copy(Objects.requireNonNull(modifiedDate, "modifiedDate"));
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = requireName(name);
    this.modifiedDate = new Date();
  }

  public Date getCreatedDate() {
    return copy(createdDate);
  }

  public Date getModifiedDate() {
    return copy(modifiedDate);
  }

  public void setModifiedDate(Date modifiedDate) {
    this.modifiedDate = copy(Objects.requireNonNull(modifiedDate, "modifiedDate"));
  }

  public abstract boolean isDirectory();

  public Folder getParent() {
    return parent;
  }

  public void setParent(Folder parent) {
    this.parent = parent;
    this.modifiedDate = new Date();
  }

  @Override
  public final boolean equals(Object object) {
    return this == object || (object instanceof Entity other && id.equals(other.id));
  }

  @Override
  public final int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{id="
        + id
        + ", name='"
        + name
        + "', parentId="
        + (parent == null ? null : parent.getId())
        + "}";
  }

  private static Date copy(Date value) {
    return new Date(value.getTime());
  }

  private static String requireName(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return value;
  }
}
