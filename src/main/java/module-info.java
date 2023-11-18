module com.github.tonivade.todo {
  exports com.github.tonivade.todo;

  requires com.github.tonivade.purecfg;
  requires com.github.tonivade.puredbc;
  requires com.github.tonivade.purefun.annotation;
  requires com.github.tonivade.purefun.core;
  requires com.github.tonivade.purefun.effect;
  requires com.github.tonivade.purefun.free;
  requires com.github.tonivade.purefun.transformer;
  requires com.github.tonivade.purefun.typeclasses;
  requires com.github.tonivade.purejson;
  requires com.zaxxer.hikari;
  requires org.slf4j;
  requires java.sql;
  requires com.github.tonivade.zeromock.api;
  requires com.github.tonivade.zeromock.server;
}