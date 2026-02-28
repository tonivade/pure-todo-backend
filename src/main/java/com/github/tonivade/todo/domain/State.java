/*
 * Copyright (c) 2020-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo.domain;

public enum State {
  DRAFT() {
    @Override
    public boolean isDraft() {
      return true;
    }
    
    @Override
    public boolean isCompleted() {
      return false;
    }
  },
  COMPLETED() {
    @Override
    public boolean isDraft() {
      return false;
    }
    
    @Override
    public boolean isCompleted() {
      return true;
    }
  },
  NOT_COMPLETED() {
    @Override
    public boolean isDraft() {
      return false;
    }
    
    @Override
    public boolean isCompleted() {
      return false;
    }
  };

  public abstract boolean isDraft();

  public abstract boolean isCompleted();
}
