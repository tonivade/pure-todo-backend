package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.todo.domain.Id;
import com.github.tonivade.todo.domain.Todo;
import com.github.tonivade.todo.domain.TodoRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tonivade.purefun.effect.Task.exec;
import static com.github.tonivade.purefun.effect.Task.task;

public class TodoInMemoryRepository implements TodoRepository<Task.µ> {

  private final AtomicInteger counter = new AtomicInteger();
  private final Map<Id, Todo> map = new ConcurrentHashMap<>();

  @Override
  public Monad<Task.µ> monad() {
    return TaskInstances.monad();
  }

  @Override
  public Higher1<Task.µ, Todo> create(Todo todo) {
    return task(() -> {
      var created = todo.withId(counter.incrementAndGet());
      map.put(created.id(), created);
      return created;
    });
  }

  @Override
  public Higher1<Task.µ, Sequence<Todo>> findAll() {
    return task(() -> ImmutableList.from(map.values()));
  }

  @Override
  public Higher1<Task.µ, Option<Todo>> find(Id id) {
    return task(() -> Option.of(() -> map.get(id)));
  }

  @Override
  public Higher1<Task.µ, Option<Todo>> update(Todo todo) {
    return task(() -> {
      Todo existing = map.get(todo.id());
      if (existing != null) {
        map.put(todo.id(), todo);
        return Option.of(todo);
      }
      return Option.none();
    });
  }

  @Override
  public Higher1<Task.µ, Unit> deleteAll() {
    return exec(() -> map.clear());
  }

  @Override
  public Higher1<Task.µ, Unit> delete(Id id) {
    return exec(() -> map.remove(id));
  }
}
