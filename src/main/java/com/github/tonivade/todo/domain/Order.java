/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.core.Precondition.checkPositive;

public record Order(int value) {
  public Order {
    checkPositive(value, "order must be a positive value");
  }
}
