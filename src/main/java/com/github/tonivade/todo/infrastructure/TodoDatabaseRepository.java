/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.puredbc.PureDBC;
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

import javax.sql.DataSource;

import static java.util.Objects.requireNonNull;

public final class TodoDatabaseRepository implements TodoRepository<Task.µ> {

  private final TodoDAO dao;
  private final DataSource dataSource;

  public TodoDatabaseRepository(TodoDAO dao, DataSource dataSource) {
    this.dao = requireNonNull(dao);
    this.dataSource = requireNonNull(dataSource);
  }

  @Override
  public Monad<Task.µ> monad() {
    return TaskInstances.monad();
  }

  @Override
  public Higher1<Task.µ, Todo> create(Todo todo) {
    return PureDBC.pure(todo)
        .map(TodoEntity::fromDomain)
        .flatMap(dao::insert)
        .map(todo::withId)
        .safeRunIO(dataSource);
  }

  @Override
  public Higher1<Task.µ, Sequence<Todo>> findAll() {
    return dao.findAll()
        .<Sequence<TodoEntity>>map(ImmutableList::from)
        .map(seq -> seq.map(TodoEntity::toDomain))
        .safeRunIO(dataSource);
  }

  @Override
  public Higher1<Task.µ, Option<Todo>> find(Id id) {
    return dao.find(id.value())
        .map(option -> option.map(TodoEntity::toDomain))
        .safeRunIO(dataSource);
  }

  @Override
  public Higher1<Task.µ, Option<Todo>> update(Todo todo) {
    return dao.find(todo.getId()).flatMap(
        option -> {
          if (option.isPresent()) {
            return dao.update(TodoEntity.fromDomain(todo)).map(ignore -> Option.some(todo));
          }
          return PureDBC.pure(Option.<Todo>none());
        }).safeRunIO(dataSource);
  }

  @Override
  public Higher1<Task.µ, Unit> deleteAll() {
    return dao.deleteAll().safeRunIO(dataSource);
  }

  @Override
  public Higher1<Task.µ, Unit> delete(Id id) {
    return dao.delete(id.value()).safeRunIO(dataSource);
  }
}
