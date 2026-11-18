create database carePortal;
use carePortal;

CREATE TABLE equipment (
    equipment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_name VARCHAR(100) NOT NULL,
    equipment_type VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    model_number VARCHAR(50) NOT NULL,
    serial_number VARCHAR(50) NOT NULL UNIQUE,
    assigned_patient_id VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Requested',
    notes VARCHAR(500) NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_equipment_status CHECK (status IN ('Requested', 'Approved', 'Shipped', 'Delivered', 'In Use', 'Returned', 'Maintenance'))
);

CREATE TABLE shipment (
    shipment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    shipment_date DATE NOT NULL,
    expected_delivery_date DATE NOT NULL,
    actual_delivery_date DATE NULL,
    shipment_status VARCHAR(20) NOT NULL DEFAULT 'Created',
    notes VARCHAR(500) NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_shipment_status CHECK (shipment_status IN ('Created', 'In Transit', 'Out for Delivery', 'Delivered', 'Delayed', 'Cancelled'))
);

CREATE TABLE activity_log (
    activity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    user_id VARCHAR(50) NOT NULL,
    activity_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_entity_type CHECK (entity_type IN ('EQUIPMENT', 'SHIPMENT')),
    CONSTRAINT chk_action_type CHECK (action_type IN ('ADD', 'EDIT', 'STATUS_CHANGE'))
);

INSERT INTO equipment (equipment_name, equipment_type, manufacturer, model_number, serial_number, assigned_patient_id, status, notes) VALUES
('Hospital Bed',    'Furniture',   'BedWorks',   'HB-7',   'SN-3001', 'PAT-007', 'Returned',  NULL),
('Pulse Oximeter',  'Monitoring',  'VitalSigns', 'PO-12',  'SN-3002', 'PAT-003', 'Approved',  NULL),
('Wheelchair',      'Mobility',    'RollMed',    'WC-3',   'SN-3003', 'PAT-009', 'Inactive',  NULL),
('Infusion Pump',   'Respiratory', 'MedSupply',  'IP-1',   'SN-3004', NULL,      'Active',    NULL),
('Nebulizer',       'Monitoring',  'MedSupply',  'NB-2',   'SN-3005', 'PAT-011', 'Requested', NULL),
('Walker',          'Mobility',    'RollMed',    'WK-4',   'SN-3006', NULL,      'Active',    NULL),
('BP Monitor',      'Monitoring',  'VitalSigns', 'BP-9',   'SN-3007', 'PAT-012', 'Active',    NULL),
('Suction Unit',    'Respiratory', 'OxyCare',    'SU-5',   'SN-3008', NULL,      'Approved',  NULL),
('Patient Lift',    'Mobility',    'LiftCo',     'PL-6',   'SN-3009', 'PAT-013', 'Inactive',  NULL),
('Glucose Meter',   'Monitoring',  'VitalSigns', 'GM-8',   'SN-3010', NULL,      'Requested', NULL),
('CPAP Machine',    'Respiratory', 'OxyCare',    'CP-3',   'SN-3011', 'PAT-014', 'Active',    NULL),
('Bed Rail',        'Furniture',   'BedWorks',   'BR-2',   'SN-3012', NULL,      'Active',    NULL);

