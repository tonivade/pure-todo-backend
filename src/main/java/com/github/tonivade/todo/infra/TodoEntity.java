/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infra;

import com.github.tonivade.purefun.Nullable;
import com.github.tonivade.todo.domain.Todo;

public record TodoEntity(@Nullable Long id, String title, Integer order, Boolean completed) {

  public Todo toDomain() {
    return Todo.create(id != null ? id.intValue() : null, title, order, completed);
  }

  public static TodoEntity fromDomain(Todo todo) {
    return new TodoEntity(
        todo.getId() != null ? todo.getId().longValue() : null,
        todo.getTitle(),
        todo.getOrder(),
        todo.isCompleted());
  }
}
