CREATE TABLE payment_saga (
    payment_id VARCHAR(36) NOT NULL,
    saga_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    payment_method_type VARCHAR(20) NOT NULL,
    payment_token VARCHAR(150) NOT NULL,
    card_last4 VARCHAR(4),
    installments INT,
    status VARCHAR(30) NOT NULL,
    current_step VARCHAR(40) NOT NULL,
    failure_reason VARCHAR(255),
    compensation_reason VARCHAR(255),
    shipping_street VARCHAR(180) NOT NULL,
    shipping_city VARCHAR(80) NOT NULL,
    shipping_state VARCHAR(80),
    shipping_country VARCHAR(2) NOT NULL,
    shipping_postal_code VARCHAR(12) NOT NULL,
    shipping_reference VARCHAR(180),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (payment_id),
    CONSTRAINT uk_payment_saga_saga_id UNIQUE (saga_id),
    CONSTRAINT uk_payment_saga_order_id UNIQUE (order_id)
);

CREATE TABLE payment_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    sku VARCHAR(40) NOT NULL,
    name VARCHAR(120),
    quantity INT NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_item_saga
        FOREIGN KEY (payment_id) REFERENCES payment_saga(payment_id)
        ON DELETE CASCADE
);

CREATE TABLE payment_saga_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    step_name VARCHAR(40) NOT NULL,
    service_name VARCHAR(60) NOT NULL,
    step_sequence INT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(60),
    error_message VARCHAR(255),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_step_saga
        FOREIGN KEY (payment_id) REFERENCES payment_saga(payment_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_payment_step UNIQUE (payment_id, step_name, direction)
);

CREATE TABLE payment_saga_event (
    event_id VARCHAR(36) NOT NULL,
    payment_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload CLOB NOT NULL,
    publication_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    PRIMARY KEY (event_id),
    CONSTRAINT fk_payment_event_saga
        FOREIGN KEY (payment_id) REFERENCES payment_saga(payment_id)
        ON DELETE CASCADE
);

CREATE TABLE idempotency_request (
    request_id VARCHAR(100) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    payment_id VARCHAR(36),
    response_body CLOB,
    http_status INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    PRIMARY KEY (request_id),
    CONSTRAINT fk_idempotency_payment
        FOREIGN KEY (payment_id) REFERENCES payment_saga(payment_id)
        ON DELETE SET NULL
);