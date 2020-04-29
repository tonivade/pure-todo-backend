/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

public record Id(int value) {
  public Id {
    if (value < 1) {
      throw new IllegalArgumentException("id must be a positive value");
    }
  }
}


