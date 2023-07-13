package com.github.tonivade.todo.application;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.script;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.zeromock.api.Bytes;
import com.github.tonivade.zeromock.api.HttpRequest;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.HttpStatus;
import java.util.concurrent.atomic.AtomicInteger;
import j2html.tags.specialized.H2Tag;
import j2html.tags.specialized.HtmlTag;

public class TodoApp {

  AtomicInteger count = new AtomicInteger();

  public UIO<HttpResponse> index(HttpRequest request) {
    return UIO.task(() -> new HttpResponse(HttpStatus.OK, Bytes.asBytes(render())).withHeader("Content-Type", "text/html"));
  }

  private String render() {
    HtmlTag content = html(
      head(
        script().withSrc("/webjars/htmx.org/1.9.2/dist/htmx.min.js")),
      body(
        h1("Hello world"),
        createCounterElement(count.incrementAndGet()))
      );
    return "<!DOCTYPE html>\n" + content.render();
  }

  private H2Tag createCounterElement(int count) {
    return h2("count: " + count).withId("counter");
  }
}
