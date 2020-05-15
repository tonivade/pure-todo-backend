/*
 * Copyright (c) 2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.todo;

import com.github.tonivade.purecfg.PureCFG;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation.Result;

import static com.github.tonivade.purecfg.PureCFG.map2;
import static com.github.tonivade.purecfg.PureCFG.map3;
import static com.github.tonivade.purecfg.PureCFG.readConfig;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static com.github.tonivade.purecfg.Source.fromToml;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Precondition.checkRange;

public record Config(Server server, Database database) {

  public Config {
    checkNonNull(server);
    checkNonNull(database);
  }

  public static Validation<Result<String>, Config> load(String file) {
    return map2(
        readConfig("server", Server.load()),
        readConfig("database", Database.load()),
        Config::new).validatedRun(fromToml(file));
  }

  public record Server(String host, Integer port) {

    public Server {
      checkNonNull(host);
      checkNonNull(port);
      checkRange(port, 1024, 65535);
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
      checkNonNull(url);
      checkNonNull(user);
      checkNonNull(password);
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
