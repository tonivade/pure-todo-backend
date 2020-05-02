/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static java.util.Objects.requireNonNull;

public record Title(String value) {
  public Title {
    requireNonNull(value, "title cannot be null");
    if (value.isEmpty()) {
      throw new IllegalArgumentException("title cannot be empty");
    }
  }
}
