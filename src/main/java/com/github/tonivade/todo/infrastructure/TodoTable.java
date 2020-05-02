/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.puredbc.sql.Field;
import com.github.tonivade.puredbc.sql.Row;
import com.github.tonivade.puredbc.sql.Table4;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple4;
import com.github.tonivade.purefun.data.NonEmptyList;

import java.sql.SQLException;

public final class TodoTable implements Table4<Integer, String, Integer, Boolean> {

  public final Field<Integer> ID = Field.of("id");
  public final Field<String> TITLE = Field.of("title");
  public final Field<Integer> ORDER = Field.of("position");
  public final Field<Boolean> COMPLETED = Field.of("completed");

  @Override
  public Tuple4<Integer, String, Integer, Boolean> asTuple(Row row) throws SQLException {
    return Tuple.of(
        row.getInteger(ID),
        row.getString(TITLE),
        row.getInteger(ORDER),
        row.getBoolean(COMPLETED));
  }

  @Override
  public NonEmptyList<Field<?>> all() {
    return NonEmptyList.of(ID, TITLE, ORDER, COMPLETED);
  }

  @Override
  public String name() {
    return "todo";
  }
}
