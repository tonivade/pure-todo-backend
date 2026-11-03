/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infra;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;

public final class TodoInMemoryRepository implements TodoRepository<IO<?>> {

  private final AtomicInteger counter = new AtomicInteger();
  private final Map<Integer, Todo> map = new ConcurrentHashMap<>();

  @Override
  public Monad<IO<?>> monad() {
    return Instances.monad();
  }

  @Override
  public IO<Todo> create(Todo todo) {
    return IO.task(() -> {
      var created = todo.withId(counter.incrementAndGet());
      map.put(created.getId(), created);
      return created;
    });
  }

  @Override
  public IO<Sequence<Todo>> findAll() {
    return IO.task(() -> ImmutableList.from(map.values()));
  }

  @Override
  public IO<Option<Todo>> find(Id id) {
    return IO.task(() -> Option.of(() -> map.get(id.value())));
  }

  @Override
  public IO<Option<Todo>> update(Todo todo) {
    return IO.task(() -> {
      Todo existing = map.get(todo.getId());
      if (existing != null) {
        map.put(todo.getId(), todo);
        return Option.of(todo);
      }
      return Option.none();
    });
  }

  @Override
  public IO<Unit> deleteAll() {
    return IO.exec(map::clear);
  }

  @Override
  public IO<Unit> delete(Id id) {
    return IO.exec(() -> map.remove(id.value()));
  }
}
