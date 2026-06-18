CREATE TABLE family_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES family_groups (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX ux_family_members_group_user ON family_members (group_id, user_id);
