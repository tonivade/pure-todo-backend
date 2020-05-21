/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.For;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TodoRepository<F extends Witness> {

  Monad<F> monad();

  Kind<F, Todo> create(Todo todo);
  Kind<F, Sequence<Todo>> findAll();
  Kind<F, Option<Todo>> find(Id id);
  Kind<F, Option<Todo>> update(Todo todo);
  Kind<F, Unit> deleteAll();
  Kind<F, Unit> delete(Id id);

  default Kind<F, Option<Todo>> updateTitle(Id id, String title) {
    return For.with(monad())
        .andThen(() -> find(id))
        .map(option -> option.map(todo -> todo.withTitle(title)))
        .flatMap(option -> option.fold(() -> monad().pure(Option.none()), this::update))
        .run();
  }

  default Kind<F, Option<Todo>> updateOrder(Id id, int order) {
    return For.with(monad())
        .andThen(() -> find(id))
        .map(option -> option.map(todo -> todo.withOrder(order)))
        .flatMap(option -> option.fold(() -> monad().pure(Option.none()), this::update))
        .run();
  }
}
