CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    owner_user_id UUID,
    group_id UUID REFERENCES family_groups (id) ON DELETE CASCADE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_categories_owner_user_id ON categories (owner_user_id);
CREATE INDEX idx_categories_group_id ON categories (group_id);
CREATE INDEX idx_categories_type ON categories (type);
