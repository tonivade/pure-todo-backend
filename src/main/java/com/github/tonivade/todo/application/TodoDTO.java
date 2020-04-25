/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.todo.domain.Todo;

import static com.github.tonivade.purefun.type.Validation.requireNonEmpty;
import static com.github.tonivade.purefun.type.Validation.requireNonNull;
import static com.github.tonivade.purefun.type.Validation.requirePositive;
import static java.util.Objects.nonNull;

public record TodoDTO(Integer id, String title, Integer order, Boolean completed) {

  public Todo toDomain() {
    if (nonNull(id)) {
      return Validation.map4(
          requirePositive(id),
          requireNonEmpty(title),
          requirePositive(order),
          requireNonNull(completed), Todo::create).getOrElseThrow();
    }
    return Validation.map2(
        requireNonEmpty(title),
        requirePositive(order), Todo::draft).getOrElseThrow();
  }

  public static TodoDTO fromDomain(Todo todo) {
    return new TodoDTO(todo.getId(), todo.getTitle(), todo.getOrder(), todo.isCompleted());
  }
}
