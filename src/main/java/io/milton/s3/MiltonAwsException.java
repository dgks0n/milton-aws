/*
 * Copyright (C) McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.milton.s3;

/** Indicates that an AWS-backed storage operation could not be completed. */
public final class MiltonAwsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MiltonAwsException(String message) {
    super(message);
  }

  public MiltonAwsException(String message, Throwable cause) {
    super(message, cause);
  }
}
