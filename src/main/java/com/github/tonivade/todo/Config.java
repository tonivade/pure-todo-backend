/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import com.github.tonivade.purecfg.PureCFG;
import com.github.tonivade.purefun.type.Validation;

import static com.github.tonivade.purecfg.PureCFG.map2;
import static com.github.tonivade.purecfg.PureCFG.map3;
import static com.github.tonivade.purecfg.PureCFG.readConfig;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static com.github.tonivade.purecfg.Source.fromToml;
import static java.util.Objects.requireNonNull;

public record Config(Server server, Database database) {

  public Config {
    requireNonNull(server);
    requireNonNull(database);
  }

  public static Validation<Validation.Result<String>, Config> load(String file) {
    return map2(
        readConfig("server", Server.load()),
        readConfig("database", Database.load()),
        Config::new).validatedRun(fromToml(file));
  }

  public record Server(String host, Integer port) {

    public Server {
      requireNonNull(host);
      requireNonNull(port);
      if (port < 1024 || port > 65535) {
        throw new IllegalArgumentException("port out of ranges: " + port);
      }
    }

    public static PureCFG<Server> load() {
      return map2(
          readString("host"),
          readInt("port"),
          Server::new);
    }
  }

  public record Database(String url, String user, String password) {

    public Database {
      requireNonNull(url);
      requireNonNull(user);
      requireNonNull(password);
    }

    public static PureCFG<Database> load() {
      return map3(
          readString("url"),
          readString("user"),
          readString("password"),
          Database::new);
    }
  }
}
