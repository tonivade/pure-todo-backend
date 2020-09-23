/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import static com.github.tonivade.purefun.type.Option.none;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
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

  default Kind<F, Option<Todo>> modify(Id id, Operator1<Todo> update) {
    return For.with(monad())
        .andThen(() -> find(id))
        .map(option -> option.map(update::apply))
        .flatMap(option -> option.<Kind<F, Option<Todo>>>fold(() -> monad().pure(none()), this::update))
        .run();
  }
}
