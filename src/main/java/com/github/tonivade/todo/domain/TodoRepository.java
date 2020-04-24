package com.github.tonivade.todo.domain;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface TodoRepository<F extends Kind> {

  Monad<F> monad();

  Higher1<F, Todo> create(Todo todo);
  Higher1<F, Sequence<Todo>> findAll();
  Higher1<F, Option<Todo>> find(Id id);
  Higher1<F, Option<Todo>> update(Todo todo);
  Higher1<F, Unit> deleteAll();
  Higher1<F, Unit> delete(Id id);
}
