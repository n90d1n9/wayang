import os
import re

sdk_dir = 'sdk/wayang-gollek-sdk/src/main/java/tech/kayys/wayang/sdk/provider'
tui_dir = 'wayang-tui/src/main/java/tech/kayys/wayang/tui'

# Move ProviderFactory back to TUI since it belongs to TUI's Config system
os.system(f'mv {sdk_dir}/ProviderFactory.java {tui_dir}/provider/')

# 1. Update AnthropicProvider.java
path = f'{sdk_dir}/AnthropicProvider.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('package tech.kayys.wayang.tui.provider;', 'package tech.kayys.wayang.sdk.provider;')
content = content.replace('import tech.kayys.wayang.tui.config.Config;\n', '')
content = content.replace('import tech.kayys.wayang.tui.http.SseReader;', 'import tech.kayys.wayang.sdk.provider.SseReader;')
content = re.sub(r'private final Config\.ProviderConfig config;', 'private final String baseUrl;\n    private final String apiKeyEnv;', content)
content = re.sub(r'public AnthropicProvider\(Config\.ProviderConfig config, String model\) \{[\s\S]*?\}', 
                 'public AnthropicProvider(String baseUrl, String apiKey, String apiKeyEnv, String model) {\n        this.baseUrl = baseUrl;\n        this.apiKey = apiKey;\n        this.apiKeyEnv = apiKeyEnv;\n        this.model = model;\n        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();\n    }', content)
content = content.replace('config.apiKeyEnv', 'apiKeyEnv')
content = content.replace('config.baseUrl', 'baseUrl')
with open(path, 'w') as f: f.write(content)

# 2. Update OpenAiProvider.java
path = f'{sdk_dir}/OpenAiProvider.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('package tech.kayys.wayang.tui.provider;', 'package tech.kayys.wayang.sdk.provider;')
content = content.replace('import tech.kayys.wayang.tui.config.Config;\n', '')
content = content.replace('import tech.kayys.wayang.tui.http.SseReader;', 'import tech.kayys.wayang.sdk.provider.SseReader;')
content = re.sub(r'private final Config\.ProviderConfig config;', 'private final String baseUrl;\n    private final String apiKeyEnv;', content)
content = re.sub(r'public OpenAiProvider\(Config\.ProviderConfig config, String model\) \{[\s\S]*?\}', 
                 'public OpenAiProvider(String baseUrl, String apiKey, String apiKeyEnv, String model) {\n        this.baseUrl = baseUrl;\n        this.apiKey = apiKey;\n        this.apiKeyEnv = apiKeyEnv;\n        this.model = model;\n        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();\n    }', content)
content = content.replace('config.apiKeyEnv', 'apiKeyEnv')
content = content.replace('config.baseUrl', 'baseUrl')
with open(path, 'w') as f: f.write(content)

# 3. Update DemoProvider.java
path = f'{sdk_dir}/DemoProvider.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('package tech.kayys.wayang.tui.provider;', 'package tech.kayys.wayang.sdk.provider;')
with open(path, 'w') as f: f.write(content)

# 4. Update SseReader.java
path = f'{sdk_dir}/SseReader.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('package tech.kayys.wayang.tui.http;', 'package tech.kayys.wayang.sdk.provider;')
with open(path, 'w') as f: f.write(content)

# 5. Update ProviderFactory.java (in TUI)
path = f'{tui_dir}/provider/ProviderFactory.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('import tech.kayys.wayang.sdk.provider.Provider;', 'import tech.kayys.wayang.sdk.provider.*;')
content = content.replace('new AnthropicProvider(pc, profile.model)', 'new AnthropicProvider(pc.baseUrl, Config.resolveApiKey(pc), pc.apiKeyEnv, profile.model)')
content = content.replace('new OpenAiProvider(pc, profile.model)', 'new OpenAiProvider(pc.baseUrl, Config.resolveApiKey(pc), pc.apiKeyEnv, profile.model)')
with open(path, 'w') as f: f.write(content)

