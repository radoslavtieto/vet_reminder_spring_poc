CREATE TABLE pms_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id CHAR(2) NOT NULL,
    username VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    staff_id BIGINT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_pms_user_tenant_username (tenant_id, username),
    KEY idx_pms_user_tenant_staff (tenant_id, staff_id),
    CONSTRAINT chk_pms_user_role CHECK (role IN ('ADMIN', 'VET', 'NURSE', 'RECEPTION')),
    CONSTRAINT fk_pms_user_staff FOREIGN KEY (tenant_id, staff_id) REFERENCES staff (tenant_id, id)
) ENGINE=InnoDB;
