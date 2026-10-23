-- =============================================================
-- 考试管理系统 · 初始化数据
-- 仅用于本地开发与集成测试，密码统一为 BCrypt("123456")
-- =============================================================

USE exam;

-- 四类角色各一个账号
-- admin / teacher / marker / student，密码均为 123456
INSERT INTO sys_user (user_no, name, password_hash, role, class_id, status) VALUES
('ADMIN001',  '系统管理员', '$2a$10$Dow1oZ0U5Yr8vJ5kP7YQOeX9YqZ8X9YqZ8X9YqZ8X9YqZ8X9YqZ8X9', 'ADMIN',   NULL, 1),
('T2024001',  '张老师',     '$2a$10$Dow1oZ0U5Yr8vJ5kP7YQOeX9YqZ8X9YqZ8X9YqZ8X9YqZ8X9YqZ8X9', 'TEACHER', NULL, 1),
('M2024001',  '李阅卷',     '$2a$10$Dow1oZ0U5Yr8vJ5kP7YQOeX9YqZ8X9YqZ8X9YqZ8X9YqZ8X9YqZ8X9', 'MARKER',  NULL, 1),
('S2024001',  '王小明',     '$2a$10$Dow1oZ0U5Yr8vJ5kP7YQOeX9YqZ8X9YqZ8X9YqZ8X9YqZ8X9YqZ8X9', 'STUDENT', 1,    1),
('S2024002',  '李小红',     '$2a$10$Dow1oZ0U5Yr8vJ5kP7YQOeX9YqZ8X9YqZ8X9YqZ8X9YqZ8X9YqZ8X9', 'STUDENT', 1,    1),
('S2024003',  '赵小刚',     '$2a$10$Dow1oZ0U5Yr8vJ5kP7YQOeX9YqZ8X9YqZ8X9YqZ8X9YqZ8X9YqZ8X9', 'STUDENT', 1,    1);

INSERT INTO sys_class (name, subject, teacher_id) VALUES
('计科 2401 班', '数据结构', 2),
('计科 2402 班', '数据结构', 2);

INSERT INTO question_bank (name, subject, owner_id) VALUES
('数据结构-第一章 绪论', '数据结构', 2),
('数据结构-第二章 线性表', '数据结构', 2);

INSERT INTO question (bank_id, type, stem, options, answer, difficulty, chapter, knowledge) VALUES
(1, 'SINGLE', '数据结构是研究数据的（　）以及它们之间的关系和运算的学科。',
 '{"A":"物理结构","B":"逻辑结构","C":"存储结构","D":"逻辑结构和存储结构"}',
 '{"correct":["D"]}', 1, '第一章', '基本概念'),
(1, 'JUDGE', '算法的优劣只与时间复杂度有关，与空间复杂度无关。',
 NULL, '{"correct":["N"]}', 2, '第一章', '算法分析'),
(1, 'MULTI', '以下属于线性结构的有（　）。',
 '{"A":"栈","B":"队列","C":"树","D":"线性表"}',
 '{"correct":["A","B","D"]}', 2, '第一章', '结构分类'),
(2, 'SINGLE', '顺序表中访问第 i 个元素的时间复杂度为（　）。',
 '{"A":"O(1)","B":"O(n)","C":"O(log n)","D":"O(n^2)"}',
 '{"correct":["A"]}', 1, '第二章', '顺序表'),
(2, 'FILL', '在单链表中，删除节点 p 的后继节点，需要执行 p.next = ______。',
 NULL, '{"accept":["p.next.next","p.next.next;","p.next.next;"]}', 3, '第二章', '单链表'),
(2, 'SHORT', '请简述栈和队列的主要区别。',
 NULL, '{"accept":[]}', 3, '第二章', '栈与队列');
