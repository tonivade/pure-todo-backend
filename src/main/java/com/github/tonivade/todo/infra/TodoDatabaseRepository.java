/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infra;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import javax.sql.DataSource;
import com.github.tonivade.puredbc.PureDBC;
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

public final class TodoDatabaseRepository implements TodoRepository<IO<?>> {

  private final TodoDAO dao;
  private final DataSource dataSource;

  public TodoDatabaseRepository(TodoDAO dao, DataSource dataSource) {
    this.dao = checkNonNull(dao);
    this.dataSource = checkNonNull(dataSource);
  }

  @Override
  public Monad<IO<?>> monad() {
    return Instances.monad();
  }

  @Override
  public IO<Todo> create(Todo todo) {
    return PureDBC.pure(todo)
        .map(TodoEntity::fromDomain)
        .flatMap(dao::insert)
        .map(Long::intValue)
        .map(todo::withId)
        .unsafeRunIO(dataSource);
  }

  @Override
  public IO<Sequence<Todo>> findAll() {
    return dao.findAll()
        .<Sequence<TodoEntity>>map(ImmutableList::from)
        .map(seq -> seq.map(TodoEntity::toDomain))
        .unsafeRunIO(dataSource);
  }

  @Override
  public IO<Option<Todo>> find(Id id) {
    return dao.find(id.value())
        .map(option -> option.map(TodoEntity::toDomain))
        .unsafeRunIO(dataSource);
  }

  @Override
  public IO<Option<Todo>> update(Todo todo) {
    return dao.find(todo.getId()).flatMap(
        option -> {
          if (option.isPresent()) {
            return dao.update(TodoEntity.fromDomain(todo)).map(_ -> Option.some(todo));
          }
          return PureDBC.pure(Option.<Todo>none());
        }).unsafeRunIO(dataSource);
  }

  @Override
  public IO<Unit> deleteAll() {
    return dao.deleteAll().unsafeRunIO(dataSource);
  }

  @Override
  public IO<Unit> delete(Id id) {
    return dao.delete(id.value()).unsafeRunIO(dataSource);
  }
}
