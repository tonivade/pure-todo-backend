/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

public interface Id {

  default int get() {
    if (this instanceof Impl impl) {
      return impl.value();
    }
    return 0;
  }

  static Id create(int value) {
    return new Impl(value);
  }

  static Id empty() {
    return new Empty();
  }

  record Impl(int value) implements Id {
    public Impl {
      if (value < 1) {
        throw new IllegalArgumentException("id must be a positive value");
      }
    }
  }

  record Empty() implements Id { }
}


