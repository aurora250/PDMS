-- Integration test data
INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role, permission_group_id, phone, account_status, must_change_password, register_materials)
VALUES ('admin-uuid-001', 'admin',
        '$2b$10$prAIsqHtIZifAJkviOkbRe4IAJm4CEd7cS6tozqAZtw.O3DpWfVOC',
        'R0001', '系统管理员', NULL, '13800000001', '有效', FALSE, 'test');

INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role, phone, account_status, must_change_password, register_materials)
VALUES ('user-uuid-002', 'police01',
        '$2b$10$prAIsqHtIZifAJkviOkbRe4IAJm4CEd7cS6tozqAZtw.O3DpWfVOC',
        'R0002', '民警', '13800000002', '有效', TRUE, 'test');

INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role, phone, account_status, must_change_password, register_materials)
VALUES ('user-uuid-003', 'frozen_user',
        '$2b$10$prAIsqHtIZifAJkviOkbRe4IAJm4CEd7cS6tozqAZtw.O3DpWfVOC',
        'R0003', '普通用户', '13800000003', '冻结', FALSE, 'test');

INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role, phone, account_status, locked_until, must_change_password, register_materials)
VALUES ('user-uuid-004', 'locked_user',
        '$2b$10$prAIsqHtIZifAJkviOkbRe4IAJm4CEd7cS6tozqAZtw.O3DpWfVOC',
        'R0004', '普通用户', '13800000004', '有效',
        NOW() + INTERVAL '10 minutes', FALSE, 'test');
