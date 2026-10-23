/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.core.Precondition.checkNonEmpty;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

public value record Title(String value) {
  public Title {
    checkNonNull(value, "title cannot be null");
    checkNonEmpty(value, "title cannot be empty");
  }
}
