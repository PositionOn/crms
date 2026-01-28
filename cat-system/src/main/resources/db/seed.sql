USE cat_system;

-- 默认管理员（首次登录会自动升级为 BCrypt）
INSERT INTO operator (username, password_hash, name, status, role)
VALUES ('admin', 'admin123', '管理员', 'ENABLED', 'ADMIN')
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status), role=VALUES(role);

-- 示例桌台
INSERT INTO dining_table (code, type, capacity, status)
VALUES
('A01','HALL',4,'FREE'),
('A02','HALL',4,'FREE'),
('B01','ROOM',8,'FREE')
ON DUPLICATE KEY UPDATE capacity=VALUES(capacity);

-- 示例分类与菜品
INSERT INTO dish_category (name, sort, status)
VALUES ('热菜', 10, 'ENABLED'), ('凉菜', 20, 'ENABLED'), ('饮品', 30, 'ENABLED')
ON DUPLICATE KEY UPDATE sort=VALUES(sort), status=VALUES(status);

-- 以分类名关联（简单种子）
INSERT INTO dish (name, category_id, price, status)
SELECT '宫保鸡丁', c.id, 38.00, 'ON' FROM dish_category c WHERE c.name='热菜'
ON DUPLICATE KEY UPDATE price=VALUES(price), status=VALUES(status);

INSERT INTO dish (name, category_id, price, status)
SELECT '拍黄瓜', c.id, 12.00, 'ON' FROM dish_category c WHERE c.name='凉菜'
ON DUPLICATE KEY UPDATE price=VALUES(price), status=VALUES(status);

INSERT INTO dish (name, category_id, price, status)
SELECT '可乐', c.id, 6.00, 'ON' FROM dish_category c WHERE c.name='饮品'
ON DUPLICATE KEY UPDATE price=VALUES(price), status=VALUES(status);

-- 示例折扣
INSERT INTO discount_rule (name, type, params_json, enabled, priority)
VALUES
('九折', 'FIXED_RATE', '{\"rate\":0.9}', 1, 100),
('满100减10', 'FULL_REDUCTION', '{\"threshold\":100,\"minus\":10}', 1, 90)
ON DUPLICATE KEY UPDATE params_json=VALUES(params_json), enabled=VALUES(enabled), priority=VALUES(priority);

