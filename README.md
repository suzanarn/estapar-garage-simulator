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

1. Suba o banco de dados:
   ```bash
   docker compose up -d
   
2. Rode a aplicação:
   ```bash
   ./mvnw spring-boot:run

A aplicação estaŕá rodando na porta http://localhost:3003

