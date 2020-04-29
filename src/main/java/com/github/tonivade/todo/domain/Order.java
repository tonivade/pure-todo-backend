/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

public record Order(int value) {
  public Order {
    if (value < 1) {
      throw new IllegalArgumentException("order must be a positive value");
    }
  }
}
