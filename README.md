# Serviço de Operações

Microserviço para receber operações financeiras e enviá-las a um tópico Kafka.

## Requisitos

- JDK 8 ou superior
- Clojure
- Kafka

## Instalação

```
git clone https://github.com/vapordev/operations.git
cd operations
```

## Execução

### Local

```bash
# Porta padrão (3000)
clojure -M:run-m

# Porta específica
PORT=8080 clojure -M:run-m
```

### Com Docker Compose

```bash
# Iniciar
docker-compose up -d

# Parar
docker-compose down
```

### JAR

```bash
# Construir
clojure -T:build ci

# Executar
java -jar target/operations-0.1.0-SNAPSHOT.jar
```

## Serviços disponíveis

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| Aplicação | 3000 | API de operações |
| Kafka UI | 8080 | Interface web para Kafka |
| Kafka | 9092, 29092 | Broker Kafka |
| Zookeeper | 2181 | Coordenação do Kafka |

## API

### POST /operations

Recebe operações financeiras para processamento.

```bash
curl -X POST http://localhost:3000/operations \
  -H "Content-Type: application/json" \
  -d '[{"operation":"buy", "unit-cost":10.00, "quantity": 100},
       {"operation":"sell", "unit-cost":15.00, "quantity": 50}]'
```

### GET /health

Verifica o status da aplicação e conexão com Kafka.

```bash
curl http://localhost:3000/health
```

## Acessando eventos via Kafka UI

Além da API, você também pode acessar os eventos enviados através da interface gráfica do Kafka UI:

Acesse a interface web do Kafka UI em [http://localhost:8080](http://localhost:8080)

## Desenvolvimento

```bash
# Executar testes
clojure -X:test
```

## Licença

Copyright © 2025 Vapordev
Eclipse Public License versão 1.0.
