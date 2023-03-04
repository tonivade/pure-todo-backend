/*
 * Copyright (c) 2020-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.Precondition.checkPositive;

public record Order(int value) {
  public Order {
    checkPositive(value, "order must be a positive value");
  }
}
