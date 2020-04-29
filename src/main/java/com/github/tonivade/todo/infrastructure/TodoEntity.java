/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.todo.domain.Todo;

public record TodoEntity(Integer id, String title, Integer order, Boolean completed) {

  public Todo toDomain() {
    return Todo.create(id, title, order, completed);
  }

  public static TodoEntity fromDomain(Todo todo) {
    return new TodoEntity(
        todo.getId(),
        todo.getTitle(),
        todo.getOrder(),
        todo.isCompleted());
  }
}
