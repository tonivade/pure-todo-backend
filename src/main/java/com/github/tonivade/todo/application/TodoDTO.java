/*
 * Copyright (c) 2020-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import static com.github.tonivade.purefun.type.Validation.requireNonEmpty;
import static com.github.tonivade.purefun.type.Validation.requireNonNull;
import static com.github.tonivade.purefun.type.Validation.requirePositive;
import static java.util.Objects.nonNull;

import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purejson.Json;
import com.github.tonivade.todo.domain.Todo;

@Json
public record TodoDTO(Integer id, String title, Integer order, Boolean completed, String url) {

  private static final String BASE_URL = "https://tonivade.es/todo/";

  public Todo toDomain() {
    return Validation.mapN(
        requirePositive(id),
        requireNonEmpty(title),
        requirePositive(order),
        requireNonNull(completed), Todo::create).getOrElseThrow();
  }

  public Todo toDraft() {
    if (nonNull(order)) {
      return Validation.mapN(
          requireNonEmpty(title),
          requirePositive(order), Todo::draft).getOrElseThrow();
    }
    return requireNonNull(title).map(Todo::draft).getOrElseThrow();
  }

  public static TodoDTO fromDomain(Todo todo) {
    return new TodoDTO(
        todo.getId(),
        todo.getTitle(),
        todo.getOrder(),
        todo.isCompleted(),
        BASE_URL + todo.getId());
  }
}
