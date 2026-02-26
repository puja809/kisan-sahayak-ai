-- Create Fertilizer Suppliers table for data.gov.in integration
CREATE TABLE IF NOT EXISTS fertilizer_suppliers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    state VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    document_id VARCHAR(50),
    sl_no INT,
    no_of_wholesalers INT DEFAULT 0,
    no_of_retailers INT DEFAULT 0,
    fertilizer_type VARCHAR(100),
    supplier_name VARCHAR(255),
    contact_info VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for efficient querying
    INDEX idx_state_district (state, district),
    INDEX idx_fertilizer_type (fertilizer_type),
    INDEX idx_state (state),
    INDEX idx_district (district),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index for combined search
CREATE INDEX idx_state_district_type ON fertilizer_suppliers(state, district, fertilizer_type);

-- Create index for sorting by supplier count
CREATE INDEX idx_wholesalers_retailers ON fertilizer_suppliers(no_of_wholesalers, no_of_retailers);
