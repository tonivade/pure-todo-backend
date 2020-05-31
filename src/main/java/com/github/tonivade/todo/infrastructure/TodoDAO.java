/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.puredbc.PureDBC;
import com.github.tonivade.puredbc.Row;
import com.github.tonivade.puredbc.sql.SQL;
import com.github.tonivade.puredbc.sql.SQL1;
import com.github.tonivade.puredbc.sql.SQL2;
import com.github.tonivade.puredbc.sql.SQL4;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Option;

public final class TodoDAO {

  private static final TodoTable TODO = new TodoTable();

  private static final SQL CREATE =
      SQL.sql("""
              create table if not exists todo (
                id identity not null,
                title varchar(100) not null,
                position int,
                completed int not null default 0,
                primary key (id))
              """);
  private static final SQL2<String, Integer> INSERT_TODO = SQL.insertInto(TODO).values(TODO.TITLE, TODO.ORDER);
  private static final SQL4<String, Integer, Boolean, Long> UPDATE_TODO =
      SQL.update(TODO).set(TODO.TITLE, TODO.ORDER, TODO.COMPLETED).where(TODO.ID.eq());
  private static final SQL FIND_ALL = SQL.select(TODO.all()).from(TODO);
  private static final SQL1<Long> FIND_BY_ID = FIND_ALL.where(TODO.ID.eq());
  private static final SQL DELETE_ALL = SQL.deleteFrom(TODO);
  private static final SQL1<Long> DELETE_BY_ID = DELETE_ALL.where(TODO.ID.eq());

  public PureDBC<Unit> create() {
    return PureDBC.update(CREATE);
  }

  public PureDBC<Long> insert(TodoEntity entity) {
    return PureDBC.updateWithKeys(
        INSERT_TODO.bind(entity.title(), entity.order()), TODO.ID).map(Option::get);
  }

  public PureDBC<Unit> update(TodoEntity entity) {
    return PureDBC.update(
        UPDATE_TODO.bind(
            entity.title(),
            entity.order(),
            entity.completed(),
            entity.id()
        )
    );
  }

  public PureDBC<Iterable<TodoEntity>> findAll() {
    return PureDBC.queryIterable(FIND_ALL, this::toEntity);
  }

  public PureDBC<Option<TodoEntity>> find(long id) {
    return PureDBC.queryOne(FIND_BY_ID.bind(id), this::toEntity);
  }

  public PureDBC<Unit> deleteAll() {
    return PureDBC.update(DELETE_ALL);
  }

  public PureDBC<Unit> delete(long id) {
    return PureDBC.update(DELETE_BY_ID.bind(id));
  }

  private TodoEntity toEntity(Row row) {
    return new TodoEntity(
        row.getLong(TODO.ID),
        row.getString(TODO.TITLE),
        row.getInteger(TODO.ORDER),
        row.getBoolean(TODO.COMPLETED));
  }
}
