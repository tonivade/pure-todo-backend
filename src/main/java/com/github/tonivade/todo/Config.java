package com.github.tonivade.todo;

import com.github.tonivade.purecfg.PureCFG;
import com.github.tonivade.purefun.type.Validation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import static com.github.tonivade.purecfg.PureCFG.map2;
import static com.github.tonivade.purecfg.PureCFG.map3;
import static com.github.tonivade.purecfg.PureCFG.readConfig;
import static com.github.tonivade.purecfg.PureCFG.readInt;
import static com.github.tonivade.purecfg.PureCFG.readString;
import static java.util.Objects.requireNonNull;

public record Config(Server server, Database database) {

  public static Validation<Validation.Result<String>, Config> load(String file) {
    return map2(
        readConfig("server", Server.load()),
        readConfig("database", Database.load()),
        Config::new
    ).validatedRun(readProperties(file));
  }

  public Config {
    requireNonNull(server);
    requireNonNull(database);
  }

  public record Server(String host, Integer port) {

    public Server {
      requireNonNull(host);
      requireNonNull(port);
    }

    public static PureCFG<Server> load() {
      return map2(
          readString("host"),
          readInt("port"),
          Server::new);
    }
  }
  public record Database(String url, String user, String pass) {

    public Database {
      requireNonNull(url);
      requireNonNull(user);
      requireNonNull(pass);
    }

    public static PureCFG<Database> load() {
      return map3(
          readString("url"),
          readString("user"),
          readString("password"),
          Database::new);
    }
  }

  private static Properties readProperties(String file) {
    try {
      var properties = new Properties();
      properties.load(requireNonNull(Config.class.getClassLoader().getResourceAsStream(file)));
      return properties;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
