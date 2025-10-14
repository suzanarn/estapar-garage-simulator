
CREATE DATABASE IF NOT EXISTS estapar CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE estapar;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS vehicle_session;
DROP TABLE IF EXISTS spot;
DROP TABLE IF EXISTS sector;

SET FOREIGN_KEY_CHECKS = 1;

-- ======================================
-- Tabela: sector
-- ======================================
CREATE TABLE sector (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  base_price DECIMAL(10,2) NULL,
  max_capacity INT NOT NULL,
  open_hour VARCHAR(8) NOT NULL,
  close_hour VARCHAR(8) NOT NULL,
  duration_limit_minutes INT NOT NULL,
  CONSTRAINT pk_sector PRIMARY KEY (id),
  CONSTRAINT uq_sector_code UNIQUE (code),
  CONSTRAINT chk_capacity_pos CHECK (max_capacity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ======================================
-- Tabela: spot
-- ======================================
CREATE TABLE spot (
  id BIGINT NOT NULL,
  sector_id BIGINT NOT NULL,
  lat DECIMAL(10,6) NULL,
  lng DECIMAL(10,6) NULL,
  occupied_by_session_id BIGINT NULL,
  CONSTRAINT pk_spot PRIMARY KEY (id),
  CONSTRAINT fk_spot_sector FOREIGN KEY (sector_id)
    REFERENCES sector (id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_spot_sector_occ ON spot (sector_id, occupied_by_session_id);
CREATE INDEX idx_spot_occ ON spot (occupied_by_session_id);
CREATE INDEX idx_spot_lat_lng ON spot (lat, lng);

-- ======================================
-- Tabela: vehicle_session
-- ======================================
CREATE TABLE vehicle_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  license_plate VARCHAR(32) NOT NULL,
  sector_id BIGINT NULL,
  spot_id BIGINT NULL,
  entry_time TIMESTAMP(3) NOT NULL,
  exit_time TIMESTAMP(3) NULL,
  base_price DECIMAL(10,2) NULL,
  price_factor DECIMAL(5,2) NOT NULL,
  charged_amount DECIMAL(10,2) NULL,
  open_plate VARCHAR(32)
    GENERATED ALWAYS AS (CASE WHEN exit_time IS NULL THEN license_plate ELSE NULL END) VIRTUAL,
  CONSTRAINT pk_vehicle_session PRIMARY KEY (id),
  CONSTRAINT fk_vs_sector FOREIGN KEY (sector_id)
    REFERENCES sector (id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_vs_spot FOREIGN KEY (spot_id)
    REFERENCES spot (id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uq_open_session_per_plate ON vehicle_session (open_plate);
CREATE INDEX idx_vs_plate_exit ON vehicle_session (license_plate, exit_time);
CREATE INDEX idx_vs_exit_time ON vehicle_session (exit_time);
CREATE INDEX idx_vs_plate ON vehicle_session (license_plate);
