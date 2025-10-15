# Parking System – Estapar Backend Test

Projeto backend em **Java 21 / Spring Boot 3** com **MySQL** para gerenciar um estacionamento: vagas, entrada/saída e faturamento.

---

## Arquitetura (resumo)

- **Camadas**
  - `api`: controllers REST e DTOs.
  - `application`: serviços de orquestração (ENTRY, PARKED, EXIT, REVENUE).
  - `domain`: entidades, regras de negócio e repositórios JPA.
  - `infrastructure`: integração com o simulador e bootstrap da garagem.
- **Banco**: MySQL 8 (migrações via Flyway).
- **Integrações**:
  - Simulador da garagem (`GET /garage`) — popula setores e vagas.
  - Webhook local (`POST /webhook`) — recebe eventos de entrada, estacionamento e saída.

---

## Rodando a aplicação localmente

### Pré-requisitos
- Java 17+
- Docker (para subir o banco via docker-compose)
- Maven (ou use o wrapper `./mvnw` incluído no projeto)

### Passos

1. Suba a aplicação com:
   ```bash
   
      docker compose up -d --build

   ```

A aplicação estará rodando na porta 3003

2. Acessar o swagger do endpoint /revenue: 
http://localhost:3003/swagger-ui/index.html#/revenue-controller/getRevenue

Obs: Como o desafio espera um GET /revenue com json, um endpoint /POST foi criado para caso haja a necessidade de retornar os dados pelo swagger

### Melhorias Futuras:
1. Sistema de fila
2. Implementar Strategy pattern para EntryAllocator para regras de alocação 