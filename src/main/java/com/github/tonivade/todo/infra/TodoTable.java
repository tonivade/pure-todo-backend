/*
 * Copyright (c) 2020-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.infra;

import com.github.tonivade.puredbc.sql.Field;
import com.github.tonivade.puredbc.sql.Table4;
import com.github.tonivade.purefun.typeclasses.TupleK;
import com.github.tonivade.purefun.typeclasses.TupleK4;

public final class TodoTable implements Table4<Long, String, Integer, Boolean> {

  public final Field<Long> ID = Field.of("id");
  public final Field<String> TITLE = Field.of("title");
  public final Field<Integer> ORDER = Field.of("position");
  public final Field<Boolean> COMPLETED = Field.of("completed");

  @Override
  public String name() {
    return "todo";
  }

  @Override
  public TupleK4<Field<?>, Long, String, Integer, Boolean> fields() {
    return TupleK.of(ID, TITLE, ORDER, COMPLETED);
  }
}
