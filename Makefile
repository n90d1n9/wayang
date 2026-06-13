# Wayang-Gollek Build Commands
MVEN = ./mvnw
PARALLEL = -T 1C

.PHONY: help build test validate clean install package verify build-agent build-tools build-runtime check dev prod b t c bt v tree tree-banned conflicts analyze

help:
	@echo "Wayang-Gollek Build Commands"
	@echo "============================"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "Build Targets:"
	@echo "  build        - Build all modules (parallel, skip tests)"
	@echo "  build-all    - Full build with tests"
	@echo "  install      - Install to local repository"
	@echo "  clean        - Clean all modules"
	@echo ""
	@echo "Validation:"
	@echo "  validate     - Validate dependency policy"
	@echo ""
	@echo "Module Builds:"
	@echo "  build-agent  - Build agent modules only"
	@echo "  build-tools  - Build tool modules only"
	@echo "  build-runtime- Build runtime modules only"
	@echo "  build-sdk    - Build Gollek SDK modules"
	@echo ""
	@echo "Quick Commands:"
	@echo "  b            - Build (skip tests)"
	@echo "  t            - Run tests"
	@echo "  c            - Clean"
	@echo "  bt           - Build and test"
	@echo "  v            - Validate only"
build:
	$(MVEN) $(PARALLEL) clean install -DskipTests

build-all:
	$(MVEN) $(PARALLEL) clean verify

install:
	$(MVEN) $(PARALLEL) clean install

clean:
	$(MVEN) clean

validate:
	$(MVEN) enforcer:enforce

check:
	$(MVEN) enforcer:enforce

build-agent:
	$(MVEN) $(PARALLEL) -pl agent/agent-core,agent/agent-spi,agent/agent-backend-gollek,agent/agent-backend-gamelan -am build

build-tools:
	$(MVEN) $(PARALLEL) -pl tools/tools-spi,tools/wayang-tool-runtime -am build

build-runtime:
	$(MVEN) $(PARALLEL) -pl runtime-quarkus -am build

build-sdk:
	$(MVEN) $(PARALLEL) -pl wayang-gollek-sdk,wayang-gollek-sdk-remote,wayang-gollek-cli -am build

test:
	$(MVEN) $(PARALLEL) test

b:
	$(MVEN) $(PARALLEL) install -DskipTests

t:
	$(MVEN) test

c:
	$(MVEN) clean

bt:
	$(MVEN) $(PARALLEL) clean verify

v:
	$(MVEN) validate

tree-banned:
	$(MVEN) dependency:tree | grep -E "(gamelan-engine-core|gollek-engine)" || echo "No banned dependencies found"
