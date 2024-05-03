/*
 * Copyright (c) 2020-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import static com.github.tonivade.purefun.effect.Task.exec;
import static com.github.tonivade.purefun.effect.Task.task;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;

public final class TodoInMemoryRepository implements TodoRepository<Task<?>> {

  private final AtomicInteger counter = new AtomicInteger();
  private final Map<Integer, Todo> map = new ConcurrentHashMap<>();

  @Override
  public Monad<Task<?>> monad() {
    return Instances.monad();
  }

  @Override
  public Task<Todo> create(Todo todo) {
    return task(() -> {
      var created = todo.withId(counter.incrementAndGet());
      map.put(created.getId(), created);
      return created;
    });
  }

  @Override
  public Task<Sequence<Todo>> findAll() {
    return task(() -> ImmutableList.from(map.values()));
  }

  @Override
  public Task<Option<Todo>> find(Id id) {
    return task(() -> Option.of(() -> map.get(id.value())));
  }

  @Override
  public Task<Option<Todo>> update(Todo todo) {
    return task(() -> {
      Todo existing = map.get(todo.getId());
      if (existing != null) {
        map.put(todo.getId(), todo);
        return Option.of(todo);
      }
      return Option.none();
    });
  }

  @Override
  public Task<Unit> deleteAll() {
    return exec(map::clear);
  }

  @Override
  public Task<Unit> delete(Id id) {
    return exec(() -> map.remove(id.value()));
  }
}
