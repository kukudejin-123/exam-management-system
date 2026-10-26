-- =============================================================
-- 考试管理系统 · 核心表结构
-- 对应 Wiki 07 页「七实体关系」与 11 页第三节
-- MySQL 8.0，引擎 InnoDB，字符集 utf8mb4
--
-- 三条设计约束（来自竞品分析结论，不可违反）：
--   1. 题、题库、试卷、组卷规则四个实体分开建模
--   2. 成绩独立于考试存在，否则无法做多次考试加权汇总
--   3. 阅卷与判分解耦
-- =============================================================

CREATE DATABASE IF NOT EXISTS exam
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE exam;

-- -------------------------------------------------------------
-- 1. 系统用户：管理员 / 教师 / 阅卷人 / 学生
--    user_no 为学号或工号，全局唯一
-- -------------------------------------------------------------
CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_no       VARCHAR(32)  NOT NULL COMMENT '学号/工号，唯一',
  name          VARCHAR(64)  NOT NULL COMMENT '姓名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码散列',
  role          VARCHAR(16)  NOT NULL COMMENT '角色：ADMIN/TEACHER/MARKER/STUDENT',
  class_id      BIGINT       NULL COMMENT '所属班级（学生必填，教师可空）',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未删 1 软删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_no (user_no),
  KEY idx_class_id (class_id),
  KEY idx_role (role)
) ENGINE = InnoDB COMMENT = '系统用户';

-- -------------------------------------------------------------
-- 2. 班级：组织单元，只做「班级」一层
--    不做部门/学院层级，避免竞品那种重组织模型
-- -------------------------------------------------------------
CREATE TABLE sys_class (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL COMMENT '班级名称',
  subject     VARCHAR(64)  NOT NULL COMMENT '所属科目',
  teacher_id  BIGINT       NULL COMMENT '班主任（sys_user.id）',
  archived    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 归档',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_teacher_id (teacher_id)
) ENGINE = InnoDB COMMENT = '班级';

-- -------------------------------------------------------------
-- 3. 题库：题目归属的容器，可跨试卷复用
-- -------------------------------------------------------------
CREATE TABLE question_bank (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  name       VARCHAR(128) NOT NULL COMMENT '题库名称',
  subject    VARCHAR(64)  NOT NULL COMMENT '学科',
  owner_id   BIGINT       NOT NULL COMMENT '归属教师（sys_user.id）',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_owner_id (owner_id),
  KEY idx_subject (subject)
) ENGINE = InnoDB COMMENT = '题库';

-- -------------------------------------------------------------
-- 4. 题目：独立于试卷存在，支持停用后不进组卷候选池
--    options / answer 用 JSON 存储，便于单选/多选/填空/简答统一
-- -------------------------------------------------------------
CREATE TABLE question (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  bank_id    BIGINT       NOT NULL COMMENT '所属题库',
  type       VARCHAR(16)  NOT NULL COMMENT 'SINGLE/MULTI/JUDGE/FILL/SHORT',
  stem       TEXT         NOT NULL COMMENT '题干',
  options    JSON         NULL COMMENT '选项，如 {"A":"..","B":".."}',
  answer     JSON         NOT NULL COMMENT '答案，如 {"correct":["A"]} 或 {"accept":["..",".."]}',
  difficulty TINYINT      NOT NULL DEFAULT 2 COMMENT '1 易 2 中 3 难',
  chapter    VARCHAR(64)  NULL COMMENT '章节',
  knowledge  VARCHAR(128) NULL COMMENT '知识点',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 0 停用（停用不进候选池）',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_bank_chapter (bank_id, chapter),
  KEY idx_bank_difficulty (bank_id, difficulty),
  KEY idx_knowledge (knowledge),
  FULLTEXT KEY ft_stem (stem) WITH PARSER ngram COMMENT '题干全文检索'
) ENGINE = InnoDB COMMENT = '题目';

-- -------------------------------------------------------------
-- 5. 组卷规则：从题库抽题的规则，与试卷分开建模
--    rule_id 为空表示手工组卷
-- -------------------------------------------------------------
CREATE TABLE paper_rule (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  name       VARCHAR(128) NOT NULL COMMENT '规则名称',
  spec       JSON         NOT NULL COMMENT '抽题规格，如 {"chapters":[{"chapter":"第一章","count":5}],"difficulty":[1,2,2],"types":["SINGLE","JUDGE"]}',
  total_score INT         NOT NULL COMMENT '目标总分',
  created_by BIGINT       NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_created_by (created_by)
) ENGINE = InnoDB COMMENT = '组卷规则';

-- 规则与题库的多对多关联
CREATE TABLE paper_rule_bank (
  rule_id BIGINT NOT NULL,
  bank_id BIGINT NOT NULL,
  PRIMARY KEY (rule_id, bank_id),
  KEY idx_bank_id (bank_id)
) ENGINE = InnoDB COMMENT = '组卷规则-题库关联';

-- -------------------------------------------------------------
-- 6. 试卷：手工组卷或按规则生成，题目分值存在关联表
-- -------------------------------------------------------------
CREATE TABLE exam_paper (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL COMMENT '试卷名称',
  rule_id     BIGINT       NULL COMMENT '来源组卷规则，NULL 表示手工组卷',
  total_score INT          NOT NULL DEFAULT 0 COMMENT '总分',
  question_count INT       NOT NULL DEFAULT 0 COMMENT '题目数',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 草稿 2 已发布',
  created_by  BIGINT       NOT NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_rule_id (rule_id),
  KEY idx_created_by (created_by)
) ENGINE = InnoDB COMMENT = '试卷';

-- 试卷-题目关联：同一道题在不同试卷里分值可以不同
CREATE TABLE exam_paper_question (
  paper_id     BIGINT NOT NULL,
  question_id  BIGINT NOT NULL,
  seq          INT    NOT NULL DEFAULT 0 COMMENT '题号',
  score        INT    NOT NULL COMMENT '该题分值',
  PRIMARY KEY (paper_id, question_id),
  KEY idx_question_id (question_id),
  KEY idx_paper_seq (paper_id, seq)
) ENGINE = InnoDB COMMENT = '试卷-题目关联';

-- -------------------------------------------------------------
-- 7. 考试：一场考试的发布与考场参数
--    score_rule：HIGHEST 取最高分 / LAST 取末次 / AVERAGE 平均
-- -------------------------------------------------------------
CREATE TABLE exam (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  paper_id       BIGINT       NOT NULL COMMENT '所用试卷',
  class_id       BIGINT       NOT NULL COMMENT '应考班级',
  name           VARCHAR(128) NOT NULL COMMENT '考试名称',
  start_at       DATETIME     NOT NULL COMMENT '开始时间',
  end_at         DATETIME     NOT NULL COMMENT '截止时间',
  duration       INT          NOT NULL COMMENT '答题时长（分钟）',
  pass_line      INT          NULL COMMENT '及格线',
  max_attempts   INT          NOT NULL DEFAULT 1 COMMENT '每人可考次数',
  score_rule     VARCHAR(16)  NOT NULL DEFAULT 'LAST' COMMENT 'HIGHEST/LAST/AVERAGE',
  retake_policy  TINYINT      NOT NULL DEFAULT 0 COMMENT '缺考是否允许补考：0 否 1 是',
  publish_mode   VARCHAR(16)  NOT NULL DEFAULT 'IMMEDIATE' COMMENT '成绩发布：IMMEDIATE/DELAYED/MANUAL',
  status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未开始 1 进行中 2 已结束',
  created_by     BIGINT       NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_class_start (class_id, start_at),
  KEY idx_paper_id (paper_id),
  KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '考试';

-- -------------------------------------------------------------
-- 8. 作答记录：按 (exam, student, attempt_no) 唯一，判分幂等
-- -------------------------------------------------------------
CREATE TABLE exam_attempt (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  exam_id      BIGINT   NOT NULL,
  student_id   BIGINT   NOT NULL,
  attempt_no   INT      NOT NULL DEFAULT 1 COMMENT '第几次尝试',
  status       TINYINT  NOT NULL DEFAULT 0 COMMENT '0 进行中 1 已交卷 2 到时自动交卷',
  started_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submit_at    DATETIME  NULL,
  score        INT       NULL COMMENT '本次尝试得分',
  cheat_events JSON      NULL COMMENT '行为留痕：[{"type":"BLUR","at":".."}]',
  created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exam_student_attempt (exam_id, student_id, attempt_no),
  KEY idx_student_id (student_id),
  KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '作答记录';

-- 每次尝试的逐题作答明细
CREATE TABLE exam_attempt_item (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  attempt_id  BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer      JSON   NULL COMMENT '考生作答',
  gained      INT    NULL COMMENT '该题得分（判分后回填）',
  graded_by   BIGINT       NULL COMMENT '阅卷人（sys_user.id）',
  graded_at   DATETIME     NULL COMMENT '阅卷时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_attempt_question (attempt_id, question_id),
  KEY idx_question_id (question_id)
) ENGINE = InnoDB COMMENT = '作答明细';

-- -------------------------------------------------------------
-- 9. 成绩：独立于考试存在，可按班级汇总
--    缺考也占一行，用 status 区分缺考 / 0 分 / 未阅卷
-- -------------------------------------------------------------
CREATE TABLE score (
  id         BIGINT  NOT NULL AUTO_INCREMENT,
  student_id BIGINT  NOT NULL,
  class_id   BIGINT  NOT NULL,
  exam_id    BIGINT  NULL COMMENT '来源考试，汇总成绩可为空',
  raw_score  INT     NULL COMMENT '原始得分',
  final_score INT     NULL COMMENT '最终得分（多次尝试按规则汇总）',
  passed     TINYINT NULL COMMENT '1 及格 0 不及格 NULL 未判定',
  status     TINYINT NOT NULL DEFAULT 1 COMMENT '1 正常 2 缺考 3 0分 4 未阅卷',
  graded_by  BIGINT   NULL COMMENT '最后修改人',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_class_student (class_id, student_id),
  KEY idx_exam_id (exam_id),
  KEY idx_student_id (student_id)
) ENGINE = InnoDB COMMENT = '成绩';

-- -------------------------------------------------------------
-- 10. 操作日志：成绩修改、权限变更留痕
-- -------------------------------------------------------------
CREATE TABLE sys_operation_log (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL COMMENT '操作人',
  action     VARCHAR(64)  NOT NULL COMMENT '动作，如 SCORE_UPDATE',
  target     VARCHAR(128) NULL COMMENT '对象',
  before_val JSON          NULL COMMENT '改前',
  after_val  JSON          NULL COMMENT '改后',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_action (action),
  KEY idx_created_at (created_at)
) ENGINE = InnoDB COMMENT = '操作日志';
