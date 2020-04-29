/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.puredbc.PureDBC;
import com.github.tonivade.puredbc.sql.Row;
import com.github.tonivade.puredbc.sql.SQL;
import com.github.tonivade.puredbc.sql.SQL1;
import com.github.tonivade.puredbc.sql.SQL2;
import com.github.tonivade.puredbc.sql.SQL4;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Option;

import java.sql.SQLException;

public class TodoDAO {

  private static TodoTable TODO = new TodoTable();

  private static SQL CREATE =
      SQL.sql("""
              create table todo (
                id identity not null,
                title varchar(100) not null,
                position int,
                completed int not null default 0,
                primary key (id))
              """);
  private static SQL2<String, Integer> INSERT_TODO = SQL.insert(TODO).values(TODO.TITLE, TODO.ORDER);
  private static SQL4<String, Integer, Boolean, Integer> UPDATE_TODO =
      SQL.update(TODO).set(TODO.TITLE, TODO.ORDER, TODO.COMPLETED).where(TODO.ID.eq());
  private static SQL FIND_ALL = SQL.select(TODO.all()).from(TODO);
  private static SQL1<Integer> FIND_BY_ID = FIND_ALL.where(TODO.ID.eq());
  private static SQL DELETE_ALL = SQL.delete(TODO);
  private static SQL1<Integer> DELETE_BY_ID = DELETE_ALL.where(TODO.ID.eq());

  public PureDBC<Unit> create() {
    return PureDBC.update(CREATE);
  }

  public PureDBC<Integer> insert(TodoEntity entity) {
    return PureDBC.updateWithKeys(
        INSERT_TODO.bind(entity.title(), entity.order()),
        row -> row.getInteger(TODO.ID)).map(Option::get);
  }

  public PureDBC<Unit> update(TodoEntity entity) {
    return PureDBC.update(UPDATE_TODO.bind(entity.title(), entity.order(), entity.completed(), entity.id()));
  }

  public PureDBC<Iterable<TodoEntity>> findAll() {
    return PureDBC.queryIterable(FIND_ALL, this::toEntity);
  }

  public PureDBC<Option<TodoEntity>> find(int id) {
    return PureDBC.queryOne(FIND_BY_ID.bind(id), this::toEntity);
  }

  public PureDBC<Unit> deleteAll() {
    return PureDBC.update(DELETE_ALL);
  }

  public PureDBC<Unit> delete(int id) {
    return PureDBC.update(DELETE_BY_ID.bind(id));
  }

  private TodoEntity toEntity(Row row) throws SQLException {
    return new TodoEntity(
        row.getInteger(TODO.ID),
        row.getString(TODO.TITLE),
        row.getInteger(TODO.ORDER),
        row.getBoolean(TODO.COMPLETED));
  }
}
