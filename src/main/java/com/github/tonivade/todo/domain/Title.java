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
