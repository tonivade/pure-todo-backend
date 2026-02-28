/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TodoRepository<F extends Kind<F, ?>> {

  Monad<F> monad();

  Kind<F, Todo> create(Todo todo);
  Kind<F, Sequence<Todo>> findAll();
  Kind<F, Option<Todo>> find(Id id);
  Kind<F, Option<Todo>> update(Todo todo);
  Kind<F, Unit> deleteAll();
  Kind<F, Unit> delete(Id id);

  default Kind<F, Option<Todo>> modify(Id id, Operator1<Todo> update) {
    return OptionT.of(monad(), find(id))
      .map(update)
      .flatMapF(this::update);
  }
}
