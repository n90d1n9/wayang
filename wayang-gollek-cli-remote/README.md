# Wayang Remote CLI

`wayang-gollek-cli-remote` is an executable CLI distribution that includes the
remote SDK provider.

Use it when the CLI should talk to a Wayang API:

```bash
java -jar target/wayang-gollek-cli-remote-1.0.0-SNAPSHOT.jar \
  --sdk-mode REMOTE \
  --endpoint https://wayang.example.com \
  status
```

The default `wayang-gollek-cli` jar stays local-first and does not bundle the
remote provider.
