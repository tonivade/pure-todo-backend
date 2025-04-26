/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infra;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import javax.sql.DataSource;
import com.github.tonivade.puredbc.PureDBC;
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

public final class TodoDatabaseRepository implements TodoRepository<Task<?>> {

  private final TodoDAO dao;
  private final DataSource dataSource;

  public TodoDatabaseRepository(TodoDAO dao, DataSource dataSource) {
    this.dao = checkNonNull(dao);
    this.dataSource = checkNonNull(dataSource);
  }

  @Override
  public Monad<Task<?>> monad() {
    return Instances.monad();
  }

  @Override
  public Task<Todo> create(Todo todo) {
    return PureDBC.pure(todo)
        .map(TodoEntity::fromDomain)
        .flatMap(dao::insert)
        .map(Long::intValue)
        .map(todo::withId)
        .safeRunIO(dataSource);
  }

  @Override
  public Task<Sequence<Todo>> findAll() {
    return dao.findAll()
        .<Sequence<TodoEntity>>map(ImmutableList::from)
        .map(seq -> seq.map(TodoEntity::toDomain))
        .safeRunIO(dataSource);
  }

  @Override
  public Task<Option<Todo>> find(Id id) {
    return dao.find(id.value())
        .map(option -> option.map(TodoEntity::toDomain))
        .safeRunIO(dataSource);
  }

  @Override
  public Task<Option<Todo>> update(Todo todo) {
    return dao.find(todo.getId()).flatMap(
        option -> {
          if (option.isPresent()) {
            return dao.update(TodoEntity.fromDomain(todo)).map(_ -> Option.some(todo));
          }
          return PureDBC.pure(Option.<Todo>none());
        }).safeRunIO(dataSource);
  }

  @Override
  public Task<Unit> deleteAll() {
    return dao.deleteAll().safeRunIO(dataSource);
  }

  @Override
  public Task<Unit> delete(Id id) {
    return dao.delete(id.value()).safeRunIO(dataSource);
  }
}
