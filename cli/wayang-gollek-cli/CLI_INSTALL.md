Wayang CLI packaging and install

Build

  mvn -pl cli/wayang-gollek-cli -am -DskipTests package

Local launcher

The repository contains a lightweight launcher script at:

  cli/wayang-gollek-cli/bin/wayang

Install system-wide (example):

  cp cli/wayang-gollek-cli/target/wayang-gollek-cli-1.0.0-SNAPSHOT.jar /opt/wayang/wayang.jar
  sudo ln -s /opt/wayang/wayang.jar /usr/local/lib/wayang.jar
  sudo tee /usr/local/bin/wayang >/dev/null <<'SH'
#!/usr/bin/env sh
exec java -jar /usr/local/lib/wayang.jar "$@"
SH
  sudo chmod +x /usr/local/bin/wayang

Using the launcher (development):

  cli/wayang-gollek-cli/bin/wayang serve --rest

Notes

- The shaded jar is produced during packaging; the launcher points to the module artifact under target/.
- For system packaging, create a proper package (deb/rpm) or install the jar under /opt and symlink.
- The REST server listens on 8080 by default; gRPC listens on 50051.
