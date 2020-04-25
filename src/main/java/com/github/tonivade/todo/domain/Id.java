package com.github.tonivade.todo.domain;

public interface Id {

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


