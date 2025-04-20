# Serviço de Operações

Microserviço responsável por receber e processar operações financeiras, enviando-as para um tópico Kafka para processamento posterior.

## Visão Geral

Este serviço oferece uma API REST para receber operações financeiras (como compra e venda) e as publica em um tópico Kafka para processamento posterior.

## Requisitos

- JDK 8 ou superior
- [Clojure](https://clojure.org/guides/getting_started)
- [Kafka](https://kafka.apache.org/) (para execução local)

## Instalação

Clone o repositório:

```
git clone https://github.com/vapordev/operations.git
cd operations
```

## Uso

### Executar o serviço

Iniciar o serviço na porta padrão (3000):

```
clojure -M:run-m
```

Iniciar o serviço em uma porta específica:

```
PORT=8080 clojure -M:run-m
```

### Construir um uberjar

Para criar um arquivo JAR executável:

```
clojure -T:build ci
```

Executar o JAR gerado:

```
java -jar target/operations-0.1.0-SNAPSHOT.jar
```

## API

### Endpoint `/operations`

**POST /operations**

Recebe uma lista de operações financeiras e as envia para processamento.

**Exemplo de requisição:**

```json
[
  {
    "operation": "buy",
    "unit-cost": 10.00,
    "quantity": 10000
  },
  {
    "operation": "sell",
    "unit-cost": 20.00,
    "quantity": 5000
  }
]
```

**Resposta de sucesso:**

```json
{
  "message": "Operations processed successfully"
}
```

**Exemplos com curl:**

Exemplo 1:
```bash
curl -X POST http://localhost:3000/operations \
  -H "Content-Type: application/json" \
  -d '[{"operation":"buy", "unit-cost":10.00, "quantity": 100},
       {"operation":"sell", "unit-cost":15.00, "quantity": 50},
       {"operation":"sell", "unit-cost":15.00, "quantity": 50}]'
```

Exemplo 2:
```bash
curl -X POST http://localhost:3000/operations \
  -H "Content-Type: application/json" \
  -d '[{"operation":"buy", "unit-cost":10.00, "quantity": 10000},
       {"operation":"sell", "unit-cost":20.00, "quantity": 5000},
       {"operation":"sell", "unit-cost":5.00, "quantity": 5000}]'
```

## Configuração

### Variáveis de ambiente

- `PORT`: Porta em que o servidor HTTP será executado (padrão: 3000)

### Configuração do Kafka

As configurações do Kafka estão disponíveis no arquivo `operations.clj`:

- Tópico: "operations"
- Bootstrap servers: "localhost:9092" (padrão)

## Desenvolvimento

### Executar testes

```
clojure -T:build test
```

### Dependências

As principais dependências do projeto incluem:

- Ring: Framework web para Clojure
- Cheshire: Biblioteca para manipulação de JSON
- Jackdaw: Cliente Kafka para Clojure

## Licença

Copyright © 2025 Vapordev

Distribuído sob a Eclipse Public License versão 1.0.
