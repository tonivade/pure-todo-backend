/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import static com.github.tonivade.zeromock.api.Bytes.asBytes;
import static com.github.tonivade.zeromock.api.Requests.delete;
import static com.github.tonivade.zeromock.api.Requests.get;
import static com.github.tonivade.zeromock.api.Requests.post;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tonivade.zeromock.api.HttpHeaders;
import com.github.tonivade.zeromock.api.HttpResponse;
import com.github.tonivade.zeromock.api.HttpStatus;
import com.github.tonivade.zeromock.api.HttpUIOService;
import com.github.tonivade.zeromock.api.Requests;
import com.github.tonivade.zeromock.client.UIOHttpClient;
import com.github.tonivade.zeromock.junit5.MockHttpServerExtension;
import com.github.tonivade.zeromock.server.UIOMockHttpServer;

@ExtendWith(MockHttpServerExtension.class)
public class EndToEndTest {
  
  private Config config = Application.loadConfig();
  
  private HttpUIOService service = Application.buildService(config);
  
  @Test
  public void findAllWhenEmpty(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount("/todo", service);
    
    HttpResponse response = client.request(delete("/todo"))
        .andThen(client.request(get("/todo")))
        .unsafeRunSync();
    
    assertEquals(HttpStatus.OK, response.status());
    assertEquals(asBytes("[]"), response.body());
  }
  
  @Test
  public void createItem(UIOMockHttpServer server, UIOHttpClient client) {
    server.mount("/todo", service);

    HttpResponse response = client.request(delete("/todo"))
        .andThen(client.request(post("/todo")
          .withHeader("Content-type", "application/json")
          .withBody("{\"title\":\"asdfg\"}")))
        .unsafeRunSync();

    assertEquals(HttpStatus.CREATED, response.status());
  }
}
