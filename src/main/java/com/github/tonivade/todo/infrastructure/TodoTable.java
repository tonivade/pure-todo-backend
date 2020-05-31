/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infrastructure;

import com.github.tonivade.puredbc.Row;
import com.github.tonivade.puredbc.sql.Field;
import com.github.tonivade.puredbc.sql.Table4;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple4;
import com.github.tonivade.purefun.data.NonEmptyList;

public final class TodoTable implements Table4<Long, String, Integer, Integer> {

  public final Field<Long> ID = Field.of("id");
  public final Field<String> TITLE = Field.of("title");
  public final Field<Integer> ORDER = Field.of("position");
  public final Field<Integer> COMPLETED = Field.of("completed");

  @Override
  public Tuple4<Long, String, Integer, Integer> asTuple(Row row) {
    return Tuple.of(
        row.getLong(ID),
        row.getString(TITLE),
        row.getInteger(ORDER),
        row.getInteger(COMPLETED));
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
