/*
 * Copyright (c) 2020-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.application;

import static com.github.tonivade.purefun.type.Validation.mapN;
import static com.github.tonivade.purefun.type.Validation.requireNonEmpty;
import static com.github.tonivade.purefun.type.Validation.requireNonNull;
import static com.github.tonivade.purefun.type.Validation.requirePositive;
import static java.util.Objects.nonNull;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation.Result;
import com.github.tonivade.purejson.Json;
import com.github.tonivade.todo.domain.Todo;

@Json
public record TodoDTO(Integer id, String title, Integer order, Boolean completed, String url) {

  private static final String BASE_URL = "https://tonivade.es/todo/";

  public Either<Throwable, Todo> toDomain() {
    return mapN(
        requirePositive(id),
        requireNonEmpty(title),
        requirePositive(order),
        requireNonNull(completed), Todo::create).mapError(Result::join)
        .<Throwable>mapError(IllegalArgumentException::new).toEither();
  }

  public Either<Throwable, Todo> toDraft() {
    if (nonNull(order)) {
      return mapN(
          requireNonEmpty(title),
          requirePositive(order), Todo::draft).mapError(Result::join)
          .<Throwable>mapError(IllegalArgumentException::new).toEither();
    }
    return requireNonNull(title).map(Todo::draft)
        .<Throwable>mapError(IllegalArgumentException::new).toEither();
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
