-- V1__init.sql (MySQL 8.0, InnoDB, UTF8MB4)

-- =========================
-- Tabela: sector
-- =========================
CREATE TABLE sector (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(16) NOT NULL,                         -- "A", "B"
  base_price DECIMAL(10,2) NOT NULL,
  max_capacity INT NOT NULL,
  open_hour CHAR(5) NOT NULL,                        -- "HH:MM"
  close_hour CHAR(5) NOT NULL,                       -- "HH:MM"
  duration_limit_minutes INT NOT NULL,
  CONSTRAINT pk_sector PRIMARY KEY (id),
  CONSTRAINT uq_sector_code UNIQUE (code),
  CONSTRAINT chk_capacity_pos CHECK (max_capacity >= 0),
  CONSTRAINT chk_base_price_nonneg CHECK (base_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Tabela: vehicle_session  (sem FK para spot ainda)
-- =========================
CREATE TABLE vehicle_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  license_plate VARCHAR(32) NOT NULL,
  sector_id BIGINT NOT NULL,
  spot_id BIGINT NULL,                               -- FK será adicionada depois
  entry_time DATETIME(3) NOT NULL,
  exit_time DATETIME(3) NULL,
  base_price DECIMAL(10,2) NOT NULL,
  price_factor DECIMAL(5,2) NOT NULL,
  charged_amount DECIMAL(10,2) NULL,
  open_plate VARCHAR(32)
    GENERATED ALWAYS AS (CASE WHEN exit_time IS NULL THEN license_plate ELSE NULL END) VIRTUAL,
  CONSTRAINT pk_vehicle_session PRIMARY KEY (id),
  CONSTRAINT fk_vs_sector FOREIGN KEY (sector_id)
    REFERENCES sector (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT chk_price_factor CHECK (price_factor > 0),
  CONSTRAINT chk_base_price_nonneg_vs CHECK (base_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Índices úteis
CREATE UNIQUE INDEX uq_open_session_per_plate ON vehicle_session (open_plate);
CREATE INDEX idx_vs_revenue ON vehicle_session (sector_id, exit_time);
CREATE INDEX idx_vs_plate ON vehicle_session (license_plate);

-- =========================
-- Tabela: spot  (pode referenciar vehicle_session já criada)
-- =========================
CREATE TABLE spot (
  id BIGINT NOT NULL,                                -- id vem do simulador
  sector_id BIGINT NOT NULL,
  lat DECIMAL(10,6) NULL,
  lng DECIMAL(10,6) NULL,
  occupied_by_session_id BIGINT NULL,
  CONSTRAINT pk_spot PRIMARY KEY (id),
  CONSTRAINT fk_spot_sector FOREIGN KEY (sector_id)
    REFERENCES sector (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_spot_session FOREIGN KEY (occupied_by_session_id)
    REFERENCES vehicle_session (id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_spot_sector ON spot (sector_id);

-- Agora que spot existe, adicionamos a FK pendente em vehicle_session.spot_id
ALTER TABLE vehicle_session
  ADD CONSTRAINT fk_vs_spot FOREIGN KEY (spot_id)
    REFERENCES spot (id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL;
