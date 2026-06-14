CREATE TABLE operations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    group_id UUID REFERENCES family_groups (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories (id),
    type VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    operation_date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_operations_user_id ON operations (user_id);
CREATE INDEX idx_operations_group_id ON operations (group_id);
CREATE INDEX idx_operations_category_id ON operations (category_id);
CREATE INDEX idx_operations_operation_date ON operations (operation_date);
CREATE INDEX idx_operations_type ON operations (type);
