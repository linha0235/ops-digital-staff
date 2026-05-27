-- 创建数据库
CREATE DATABASE IF NOT EXISTS ops_digital_staff DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ops_digital_staff;

-- 1. 运维人员账号表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(32) NOT NULL UNIQUE COMMENT '账号',
    password VARCHAR(128) NOT NULL COMMENT '密码',
    real_name VARCHAR(32) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(16) COMMENT '手机号',
    role TINYINT NOT NULL DEFAULT 1 COMMENT '角色：1普通运维 2管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0冻结 1正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维人员账号表';

-- 2. 运维知识库FAQ表
CREATE TABLE IF NOT EXISTS ops_faq (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    question VARCHAR(512) NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '答案',
    category VARCHAR(64) COMMENT '问题分类',
    is_synced TINYINT NOT NULL DEFAULT 0 COMMENT '是否已同步到向量库：0未同步 1已同步',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维知识库FAQ表';

-- 3. 运维工单表
CREATE TABLE IF NOT EXISTS ops_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    reporter_name VARCHAR(64) NOT NULL COMMENT '报障人姓名',
    reporter_phone VARCHAR(16) NOT NULL COMMENT '报障人联系方式',
    problem_title VARCHAR(256) NOT NULL COMMENT '问题标题',
    problem_desc TEXT NOT NULL COMMENT '问题详细描述',
    problem_images VARCHAR(1024) COMMENT '问题截图URL，多个逗号分隔',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '工单状态：0待处理 1处理中 2已完成',
    handler_id BIGINT COMMENT '处理人ID，关联sys_user.id',
    handle_result TEXT COMMENT '处理结果说明',
    visit_status TINYINT NOT NULL DEFAULT 0 COMMENT '回访状态：0未回访 1已回访',
    satisfaction TINYINT COMMENT '满意度：1-5分',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维工单表';

-- 4. 系统字典表
CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dict_type VARCHAR(64) NOT NULL COMMENT '字典类型',
    dict_code VARCHAR(64) NOT NULL COMMENT '字典编码',
    dict_name VARCHAR(128) NOT NULL COMMENT '字典名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    UNIQUE KEY uk_dict_type_code(dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典表';

-- 初始化默认管理员账号：admin / 123456
INSERT INTO sys_user(username, password, real_name, role) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '系统管理员', 2);

-- 初始化示例FAQ
INSERT INTO ops_faq(question, answer, category) VALUES ('账号冻结怎么处理?', '通过自助方式访问网址http://ops.xxx.com/unfreeze 或者拨打热线电话400-1234567', '账号类问题');
