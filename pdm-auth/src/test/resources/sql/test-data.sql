-- Integration test data
INSERT INTO user (user_uuid, username, password, resident_uuid, user_role, permission_group_id, phone, account_status, must_change_password, register_materials)
VALUES ('admin-uuid-001', 'admin',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHsM',
        'R0001', '系统管理员', NULL, '13800000001', '有效', 0, 'test');

INSERT INTO user (user_uuid, username, password, resident_uuid, user_role, phone, account_status, must_change_password, register_materials)
VALUES ('user-uuid-002', 'police01',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHsM',
        'R0002', '民警', '13800000002', '有效', 1, 'test');

INSERT INTO user (user_uuid, username, password, resident_uuid, user_role, phone, account_status, must_change_password, register_materials)
VALUES ('user-uuid-003', 'frozen_user',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHsM',
        'R0003', '普通用户', '13800000003', '冻结', 0, 'test');

INSERT INTO user (user_uuid, username, password, resident_uuid, user_role, phone, account_status, locked_until, must_change_password, register_materials)
VALUES ('user-uuid-004', 'locked_user',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHsM',
        'R0004', '普通用户', '13800000004', '有效',
        DATE_ADD(NOW(), INTERVAL 10 MINUTE), 0, 'test');
