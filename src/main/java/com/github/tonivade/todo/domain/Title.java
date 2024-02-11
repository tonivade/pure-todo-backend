/*
 * Copyright (c) 2020-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.core.Precondition.checkNonEmpty;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

public record Title(String value) {
  public Title {
    checkNonNull(value, "title cannot be null");
    checkNonEmpty(value, "title cannot be empty");
  }
}
