package com.github.tonivade.todo.application;

import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.todo.domain.Todo;

import static com.github.tonivade.purefun.type.Validation.*;

public record TodoDTO(Integer id, String title, Integer order, Boolean completed) {

  public Todo toDomain() {
    return Validation.map4(
        requirePositive(id),
        requireNonEmpty(title),
        requirePositive(order),
        requireNonNull(completed), Todo::create).getOrElseThrow();
  }

  public static TodoDTO fromDomain(Todo todo) {
    return new TodoDTO(todo.getId(), todo.getTitle(), todo.getOrder(), todo.isCompleted());
  }
}
