/*
 * Copyright (c) 2020-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.core.Precondition.checkPositive;

public record Id(int value) {
  public Id {
    checkPositive(value, "id must be a positive value");
  }
}
