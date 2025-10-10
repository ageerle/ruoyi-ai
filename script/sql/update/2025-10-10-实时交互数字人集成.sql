-- Description: 实时交互数字人集成模块

-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971582278942666752, '交互数字人配置', '2000', '1', 'aihumanConfig', 'aihuman/aihumanConfig/index', 1, 0, 'C', '0', '0', 'aihuman:aihumanConfig:list', '#', 103, 1, sysdate(), null, null, '交互数字人配置菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971582278942666753, '交互数字人配置查询', 1971582278942666752, '1',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:query',        '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971582278942666754, '交互数字人配置新增', 1971582278942666752, '2',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:add',          '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971582278942666755, '交互数字人配置修改', 1971582278942666752, '3',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:edit',         '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971582278942666756, '交互数字人配置删除', 1971582278942666752, '4',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:remove',       '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971582278942666757, '交互数字人配置导出', 1971582278942666752, '5',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:export',       '#', 103, 1, sysdate(), null, null, '');

-- Description: 实时交互数字人集成模块

-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971546066781597696, '数字人信息管理', '2000', '1', 'aihumanInfo', 'aihuman/aihumanInfo/index', 1, 0, 'C', '0', '0', 'aihuman:aihumanInfo:list', '#', 103, 1, sysdate(), null, null, '数字人信息管理菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971546066781597697, '数字人信息管理查询', 1971546066781597696, '1',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:query',        '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971546066781597698, '数字人信息管理新增', 1971546066781597696, '2',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:add',          '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971546066781597699, '数字人信息管理修改', 1971546066781597696, '3',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:edit',         '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971546066781597700, '数字人信息管理删除', 1971546066781597696, '4',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:remove',       '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1971546066781597701, '数字人信息管理导出', 1971546066781597696, '5',  '#', '', 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:export',       '#', 103, 1, sysdate(), null, null, '');


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for aihuman_info
-- ----------------------------
DROP TABLE IF EXISTS `aihuman_info`;
CREATE TABLE `aihuman_info`  (
                                 `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '交互名称',
                                 `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '交互内容',
                                 `create_time` datetime(0) DEFAULT NULL COMMENT '创建时间',
                                 `update_time` datetime(0) DEFAULT NULL COMMENT '更新时间',
                                 `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI人类交互信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of aihuman_info
-- ----------------------------
INSERT INTO `aihuman_info` VALUES (1, '1', '1', '2025-09-26 18:02:00', '2025-09-26 18:02:02', '0');

SET FOREIGN_KEY_CHECKS = 1;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for aihuman_config
-- ----------------------------
DROP TABLE IF EXISTS `aihuman_config`;
CREATE TABLE `aihuman_config`  (
                                   `id` int(0) NOT NULL AUTO_INCREMENT,
                                   `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                   `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                   `model_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                   `model_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
                                   `agent_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
                                   `create_time` datetime(0) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
                                   `update_time` datetime(0) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(0),
                                   `status` int(0) DEFAULT NULL,
                                   `publish` int(0) DEFAULT NULL,
                                   `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                   `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                   `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of aihuman_config
-- ----------------------------
INSERT INTO `aihuman_config` VALUES (9, '关爱老婆数字人（梅朵）', '梅朵吉祥物', '/Live2D/models/梅朵吉祥物/梅朵吉祥物.model3.json', '{\n	\"Version\": 3,\n	\"FileReferences\": {\n		\"Moc\": \"梅朵吉祥物.moc3\",\n		\"Textures\": [\n			\"梅朵吉祥物.4096/texture_00.png\",\n			\"梅朵吉祥物.4096/texture_01.png\"\n		],\n		\"Physics\": \"梅朵吉祥物.physics3.json\",\n		\"DisplayInfo\": \"梅朵吉祥物.cdi3.json\",\n		\"MotionSync\": \"梅朵吉祥物.motionsync3.json\",\n		\"Expressions\": [\n			{\n				\"Name\": \"kaixin\",\n				\"File\": \"kaixin.exp3.json\"\n			},\n			{\n				\"Name\": \"maozi\",\n				\"File\": \"maozi.exp3.json\"\n			},\n			{\n				\"Name\": \"mouth open\",\n				\"File\": \"mouth open.exp3.json\"\n			},\n			{\n				\"Name\": \"shibai\",\n				\"File\": \"shibai.exp3.json\"\n			},\n			{\n				\"Name\": \"yinchen\",\n				\"File\": \"yinchen.exp3.json\"\n			}\n		],\n		\"Motions\": {\n			\"\": [\n				{\n					\"File\": \"mouth.motion3.json\"\n				}\n			]\n		}\n	},\n	\"Groups\": [\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"LipSync\",\n			\"Ids\": [\n				\"ParamMouthForm\",\n				\"ParamMouthOpenY\"\n			]\n		},\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"EyeBlink\",\n			\"Ids\": [\n				\"ParamEyeLOpen\",\n				\"ParamEyeROpen\"\n			]\n		}\n	],\n	\"HitAreas\": []\n}', '{\n    \"bot_id\": \"7504596188201746470\",\n    \"user_id\": \"7376476310010937396\",\n    \"stream\": true,\n    \"auto_save_history\": true\n}', '2025-09-29 16:36:46', '2025-09-29 16:36:46', 0, 1, NULL, NULL, '1');
INSERT INTO `aihuman_config` VALUES (10, '关爱老婆数字人（K）', 'kei_vowels_pro', '/Live2D/models/kei_vowels_pro/kei_vowels_pro.model3.json', '{\n	\"Version\": 3,\n	\"FileReferences\": {\n		\"Moc\": \"kei_vowels_pro.moc3\",\n		\"Textures\": [\n			\"kei_vowels_pro.2048/texture_00.png\"\n		],\n		\"Physics\": \"kei_vowels_pro.physics3.json\",\n		\"DisplayInfo\": \"kei_vowels_pro.cdi3.json\",\n		\"MotionSync\": \"kei_vowels_pro.motionsync3.json\",\n		\"Motions\": {\n			\"\": [\n				{\n					\"File\": \"motions/01_kei_en.motion3.json\",\n					\"Sound\": \"sounds/01_kei_en.wav\",\n					\"MotionSync\": \"Vowels_CRI\"\n				},\n				{\n					\"File\": \"motions/01_kei_jp.motion3.json\",\n					\"Sound\": \"sounds/01_kei_jp.wav\",\n					\"MotionSync\": \"Vowels_CRI\"\n				},\n				{\n					\"File\": \"motions/01_kei_ko.motion3.json\",\n					\"Sound\": \"sounds/01_kei_ko.wav\",\n					\"MotionSync\": \"Vowels_CRI\"\n				},\n				{\n					\"File\": \"motions/01_kei_zh.motion3.json\",\n					\"Sound\": \"sounds/01_kei_zh.wav\",\n					\"MotionSync\": \"Vowels_CRI\"\n				}\n			]\n		}\n	},\n	\"Groups\": [\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"LipSync\",\n			\"Ids\": []\n		},\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"EyeBlink\",\n			\"Ids\": [\n				\"ParamEyeLOpen\",\n				\"ParamEyeROpen\"\n			]\n		}\n	],\n	\"HitAreas\": [\n		{\n			\"Id\": \"HitAreaHead\",\n			\"Name\": \"Head\"\n		}\n	]\n}', '3', '2025-09-29 16:35:27', '2025-09-29 16:35:27', 0, 1, NULL, NULL, '1');
INSERT INTO `aihuman_config` VALUES (11, '关爱老婆数字人（March 7th）', 'March 7th', '/Live2D/models/March 7th/March 7th.model3.json', '{\n	\"Version\": 3,\n	\"FileReferences\": {\n		\"Moc\": \"March 7th.moc3\",\n		\"Textures\": [\n			\"March 7th.4096/texture_00.png\",\n			\"March 7th.4096/texture_01.png\"\n		],\n		\"Physics\": \"March 7th.physics3.json\",\n		\"DisplayInfo\": \"March 7th.cdi3.json\",\n		\"Expressions\": [\n			{\n				\"Name\": \"捂脸\",\n				\"File\": \"1.exp3.json\"\n			},\n			{\n				\"Name\": \"比耶\",\n				\"File\": \"2.exp3.json\"\n			},\n			{\n				\"Name\": \"照相\",\n				\"File\": \"3.exp3.json\"\n			},\n			{\n				\"Name\": \"脸红\",\n				\"File\": \"4.exp3.json\"\n			},\n			{\n				\"Name\": \"黑脸\",\n				\"File\": \"5.exp3.json\"\n			},\n			{\n				\"Name\": \"哭\",\n				\"File\": \"6.exp3.json\"\n			},\n			{\n				\"Name\": \"流汗\",\n				\"File\": \"7.exp3.json\"\n			},\n			{\n				\"Name\": \"星星\",\n				\"File\": \"8.exp3.json\"\n			}\n		]\n	},\n	\"Groups\": [\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"EyeBlink\",\n			\"Ids\": [\n				\"ParamEyeLOpen\",\n				\"ParamEyeROpen\"\n			]\n		},\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"LipSync\",\n			\"Ids\": [\n				\"ParamMouthOpenY\"\n			]\n		}\n	],\n	\"HitAreas\": []\n}', '3', '2025-09-29 21:09:26', '2025-09-29 21:09:28', 0, 1, NULL, NULL, NULL);
INSERT INTO `aihuman_config` VALUES (12, '关爱老婆数字人（pachan）', 'pachan', '/Live2D/models/pachan/pachan.model3.json', '{\n	\"Version\": 3,\n	\"FileReferences\": {\n		\"Moc\": \"pachirisu anime girl - top half.moc3\",\n		\"Textures\": [\n			\"pachirisu anime girl - top half.4096/texture_00.png\"\n		],\n		\"Physics\": \"pachirisu anime girl - top half.physics3.json\",\n		\"DisplayInfo\": \"pachirisu anime girl - top half.cdi3.json\"\n	},\n	\"Groups\": [\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"EyeBlink\",\n			\"Ids\": []\n		},\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"LipSync\",\n			\"Ids\": []\n		}\n	]\n}', NULL, '2025-10-05 19:49:56', '2025-10-05 19:49:56', 0, 1, NULL, NULL, NULL);
INSERT INTO `aihuman_config` VALUES (13, '关爱老婆数字人（230108）', '230108', '/Live2D/models/230108/230108.model3.json', '{\n	\"Version\": 3,\n	\"FileReferences\": {\n		\"Moc\": \"230108.moc3\",\n		\"Textures\": [\n			\"230108.4096/texture_00.png\"\n		],\n		\"Physics\": \"230108.physics3.json\",\n		\"DisplayInfo\": \"230108.cdi3.json\"\n	},\n	\"Groups\": [\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"LipSync\",\n			\"Ids\": [\n				\"ParamMouthOpenY\"\n			]\n		},\n		{\n			\"Target\": \"Parameter\",\n			\"Name\": \"EyeBlink\",\n			\"Ids\": [\n				\"ParamEyeLOpen\",\n				\"ParamEyeROpen\"\n			]\n		}\n	]\n}', NULL, '2025-10-06 19:28:20', '2025-10-06 19:28:23', 0, 1, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
                                  `dict_code` bigint(0) NOT NULL COMMENT '字典编码',
                                  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户编号',
                                  `dict_sort` int(0) DEFAULT 0 COMMENT '字典排序',
                                  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典标签',
                                  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典键值',
                                  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
                                  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
                                  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表格回显样式',
                                  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
                                  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                  `create_dept` bigint(0) DEFAULT NULL COMMENT '创建部门',
                                  `create_by` bigint(0) DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime(0) DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint(0) DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime(0) DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, '000000', 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '性别男');
INSERT INTO `sys_dict_data` VALUES (2, '000000', 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '性别女');
INSERT INTO `sys_dict_data` VALUES (3, '000000', 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, '000000', 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, '000000', 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, '000000', 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, '000000', 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (12, '000000', 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, '000000', 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, '000000', 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, '000000', 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, '000000', 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, '000000', 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, '000000', 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (19, '000000', 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (20, '000000', 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (21, '000000', 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (22, '000000', 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (23, '000000', 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (24, '000000', 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (25, '000000', 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (26, '000000', 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (27, '000000', 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (28, '000000', 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (29, '000000', 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '其他操作');
INSERT INTO `sys_dict_data` VALUES (1775756996568993793, '000000', 1, '免费用户', '0', 'sys_user_grade', '', 'info', 'N', '0', 103, 1, '2024-04-04 13:27:15', 1, '2024-04-04 13:30:09', '');
INSERT INTO `sys_dict_data` VALUES (1775757116970684418, '000000', 2, '高级会员', '1', 'sys_user_grade', '', 'success', 'N', '0', 103, 1, '2024-04-04 13:27:43', 1, '2024-04-04 13:30:15', '');
INSERT INTO `sys_dict_data` VALUES (1776109770934677506, '000000', 0, 'token计费', '1', 'sys_model_billing', '', 'primary', 'N', '0', 103, 1, '2024-04-05 12:49:03', 1, '2024-04-21 00:05:41', '');
INSERT INTO `sys_dict_data` VALUES (1776109853377916929, '000000', 0, '次数计费', '2', 'sys_model_billing', '', 'success', 'N', '0', 103, 1, '2024-04-05 12:49:22', 1, '2024-04-05 12:49:22', '');
INSERT INTO `sys_dict_data` VALUES (1780264338471858177, '000000', 0, '未支付', '1', 'pay_state', '', 'info', 'N', '0', 103, 1, '2024-04-16 23:57:49', 1, '2024-04-16 23:58:29', '');
INSERT INTO `sys_dict_data` VALUES (1780264431589601282, '000000', 2, '已支付', '2', 'pay_state', '', 'success', 'N', '0', 103, 1, '2024-04-16 23:58:11', 1, '2024-04-16 23:58:21', '');
INSERT INTO `sys_dict_data` VALUES (1933094189606670338, '000000', 0, '知识库', 'vector', 'prompt_template_type', NULL, '', 'N', '0', 103, 1, '2025-06-12 17:29:05', 1, '2025-06-12 17:29:05', NULL);
INSERT INTO `sys_dict_data` VALUES (1938226101050925057, '000000', 1, '中转模型-chat', 'chat', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:21:28', 1, '2025-06-26 21:22:26', NULL);
INSERT INTO `sys_dict_data` VALUES (1938226833825193985, '000000', 1, '本地部署模型-ollama', 'ollama', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:24:22', 1, '2025-06-26 21:24:22', NULL);
INSERT INTO `sys_dict_data` VALUES (1938226919661625345, '000000', 1, 'DIFY-dify', 'dify', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:24:43', 1, '2025-06-26 21:24:43', NULL);
INSERT INTO `sys_dict_data` VALUES (1938226981422751746, '000000', 1, '扣子-coze', 'coze', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:24:58', 1, '2025-06-26 21:24:58', NULL);
INSERT INTO `sys_dict_data` VALUES (1938227034350673922, '000000', 1, '智谱清言-zhipu', 'zhipu', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:25:10', 1, '2025-06-26 21:25:10', NULL);
INSERT INTO `sys_dict_data` VALUES (1938227086750113793, '000000', 1, '深度求索-deepseek', 'deepseek', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:25:23', 1, '2025-06-26 21:25:23', NULL);
INSERT INTO `sys_dict_data` VALUES (1938227141741633537, '000000', 1, '通义千问-qianwen', 'qianwen', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:25:36', 1, '2025-06-26 21:25:36', NULL);
INSERT INTO `sys_dict_data` VALUES (1938227191834206209, '000000', 1, '知识库向量模型-vector', 'vector', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:25:48', 1, '2025-06-26 21:25:48', NULL);
INSERT INTO `sys_dict_data` VALUES (1938227249417805826, '000000', 1, '图片识别模型-image', 'image', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-06-26 21:26:01', 1, '2025-06-26 21:26:01', NULL);
INSERT INTO `sys_dict_data` VALUES (1940594785010503681, '000000', 1, 'FASTGPT-fastgpt', 'fastgpt', 'chat_model_category', NULL, '', 'N', '0', 103, 1, '2025-07-03 10:13:46', 1, '2025-07-03 10:13:46', NULL);
INSERT INTO `sys_dict_data` VALUES (1971580207002615809, '000000', 0, '未发布', '0', 'aihuman_is_publish', NULL, '#949494', 'N', '0', NULL, NULL, '2025-09-26 22:18:46', NULL, '2025-09-26 22:18:46', NULL);
INSERT INTO `sys_dict_data` VALUES (1971580286589534210, '000000', 1, '已发布', '1', 'aihuman_is_publish', NULL, '#00a89d', 'N', '0', NULL, NULL, '2025-09-26 22:19:05', 1, '2025-09-26 22:19:25', NULL);

SET FOREIGN_KEY_CHECKS = 1;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
                                  `dict_id` bigint(0) NOT NULL COMMENT '字典主键',
                                  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户编号',
                                  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典名称',
                                  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
                                  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                  `create_dept` bigint(0) DEFAULT NULL COMMENT '创建部门',
                                  `create_by` bigint(0) DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime(0) DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint(0) DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime(0) DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`dict_id`) USING BTREE,
                                  UNIQUE INDEX `tenant_id`(`tenant_id`, `dict_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '000000', '用户性别', 'sys_user_sex', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '000000', '菜单状态', 'sys_show_hide', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '000000', '系统开关', 'sys_normal_disable', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (6, '000000', '系统是否', 'sys_yes_no', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '000000', '通知类型', 'sys_notice_type', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '000000', '通知状态', 'sys_notice_status', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '000000', '操作类型', 'sys_oper_type', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '000000', '系统状态', 'sys_common_status', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083714, '911866', '用户性别', 'sys_user_sex', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083715, '911866', '菜单状态', 'sys_show_hide', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083716, '911866', '系统开关', 'sys_normal_disable', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083717, '911866', '系统是否', 'sys_yes_no', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083718, '911866', '通知类型', 'sys_notice_type', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083719, '911866', '通知状态', 'sys_notice_status', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083720, '911866', '操作类型', 'sys_oper_type', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083721, '911866', '系统状态', 'sys_common_status', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (1775756736895438849, '000000', '用户等级', 'sys_user_grade', '0', 103, 1, '2024-04-04 13:26:13', 1, '2024-04-04 13:26:13', '');
INSERT INTO `sys_dict_type` VALUES (1776109665045278721, '000000', '模型计费方式', 'sys_model_billing', '0', 103, 1, '2024-04-05 12:48:37', 1, '2024-04-08 11:22:18', '模型计费方式');
INSERT INTO `sys_dict_type` VALUES (1780263881368219649, '000000', '支付状态', 'pay_state', '0', 103, 1, '2024-04-16 23:56:00', 1, '2025-03-29 15:21:57', '支付状态');
INSERT INTO `sys_dict_type` VALUES (1904565568803217409, '000000', '状态类型', 'status_type', '0', 103, 1, '2025-03-26 00:06:31', 1, '2025-03-26 00:06:31', NULL);
INSERT INTO `sys_dict_type` VALUES (1933093946274123777, '000000', '提示词模板分类', 'prompt_template_type', '0', 103, 1, '2025-06-12 17:28:07', 1, '2025-06-12 17:28:07', '提示词模板类型');
INSERT INTO `sys_dict_type` VALUES (1938225899023884289, '000000', '模型分类', 'chat_model_category', '0', 103, 1, '2025-06-26 21:20:39', 1, '2025-06-26 21:20:39', '模型分类');
INSERT INTO `sys_dict_type` VALUES (1971579935501123586, '000000', '发布状态', 'aihuman_is_publish', '0', NULL, NULL, '2025-09-26 22:17:41', NULL, '2025-09-26 22:17:41', '0 代表未发布，1代表发布');

SET FOREIGN_KEY_CHECKS = 1;




SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
                             `menu_id` bigint(0) NOT NULL COMMENT '菜单ID',
                             `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
                             `parent_id` bigint(0) DEFAULT 0 COMMENT '父菜单ID',
                             `order_num` int(0) DEFAULT 0 COMMENT '显示顺序',
                             `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由地址',
                             `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
                             `query_param` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由参数',
                             `is_frame` int(0) DEFAULT 1 COMMENT '是否为外链（0是 1否）',
                             `is_cache` int(0) DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
                             `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
                             `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '显示状态（0显示 1隐藏）',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
                             `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
                             `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '#' COMMENT '菜单图标',
                             `create_dept` bigint(0) DEFAULT NULL COMMENT '创建部门',
                             `create_by` bigint(0) DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime(0) DEFAULT NULL COMMENT '创建时间',
                             `update_by` bigint(0) DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime(0) DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
                             PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 5, 'system', '', '', 1, 0, 'M', '0', '0', '', 'eos-icons:system-group', 103, 1, '2023-05-14 15:19:39', 1, '2025-09-26 20:19:31', '系统管理目录');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1775500307898949634, 1, 'user', 'operator/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'ph:user-fill', 103, 1, '2023-05-14 15:19:39', 1, '2024-10-07 21:29:29', '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'ri:user-3-fill', 103, 1, '2023-05-14 15:19:39', 1, '2024-10-07 21:04:59', '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'typcn:th-menu-outline', 103, 1, '2023-05-14 15:19:39', 1, '2024-10-07 21:06:06', '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', 1, 0, 'C', '1', '1', 'system:dept:list', 'mdi:company', 103, 1, '2023-05-14 15:19:39', 1, '2024-10-07 21:07:38', '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', 1, 0, 'C', '1', '1', 'system:post:list', 'post', 103, 1, '2023-05-14 15:19:39', 1, '2024-04-04 22:36:15', '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'fluent-mdl2:dictionary', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:14:33', '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '系统参数', 1, 10, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'tdesign:system-code', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:11:07', '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 14, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'icon-park-solid:volume-notice', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:11:42', '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'icon-park-solid:log', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:10:41', '日志管理菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 1, 5, 'cache', 'monitor/cache/index', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'octicon:cache-24', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:09:44', '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'icon-park-solid:log', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:13:20', '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'icon-park-solid:log', 103, 1, '2023-05-14 15:19:40', 1, '2024-10-07 21:13:33', '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1050, '账户解锁', 501, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000, '在线开发', 0, 20, 'dev', '', '', 1, 0, 'M', '0', '0', '', 'carbon:development', 103, 1, '2025-07-11 19:38:05', 1, '2025-07-11 19:43:03', '在线开发目录');
INSERT INTO `sys_menu` VALUES (1775500307898949634, '运营管理', 0, 3, 'operate', '', NULL, 1, 0, 'M', '0', '0', NULL, 'icon-park-outline:appointment', 103, 1, '2024-04-03 20:27:15', 1, '2025-09-26 20:19:38', '');
INSERT INTO `sys_menu` VALUES (1775895273104068610, '系统模型', 1775500307898949634, 2, 'model', 'operator/model/index', NULL, 1, 0, 'C', '0', '0', 'system:model:list', 'ph:list-fill', 103, 1, '2024-04-05 12:00:38', 1, '2024-10-07 21:36:00', '系统模型菜单');
INSERT INTO `sys_menu` VALUES (1775895273104068611, '系统模型查询', 1775895273104068610, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:query', '#', 103, 1, '2024-04-05 12:00:38', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1775895273104068612, '系统模型新增', 1775895273104068610, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:add', '#', 103, 1, '2024-04-05 12:00:38', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1775895273104068613, '系统模型修改', 1775895273104068610, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:edit', '#', 103, 1, '2024-04-05 12:00:38', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1775895273104068614, '系统模型删除', 1775895273104068610, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:remove', '#', 103, 1, '2024-04-05 12:00:38', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1775895273104068615, '系统模型导出', 1775895273104068610, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:export', '#', 103, 1, '2024-04-05 12:00:38', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780240077690507266, '聊天消息', 1775500307898949634, 5, 'chatMessage', 'operator/message/index', NULL, 1, 0, 'C', '0', '0', 'system:message:list', 'bx:chat', 103, 1, '2024-04-16 22:24:48', 1, '2024-10-07 21:38:49', '聊天消息菜单');
INSERT INTO `sys_menu` VALUES (1780240077690507267, '聊天消息查询', 1780240077690507266, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 103, 1, '2024-04-16 22:24:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780240077690507268, '聊天消息新增', 1780240077690507266, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:add', '#', 103, 1, '2024-04-16 22:24:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780240077690507269, '聊天消息修改', 1780240077690507266, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:edit', '#', 103, 1, '2024-04-16 22:24:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780240077690507270, '聊天消息删除', 1780240077690507266, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:remove', '#', 103, 1, '2024-04-16 22:24:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780240077690507271, '聊天消息导出', 1780240077690507266, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:export', '#', 103, 1, '2024-04-16 22:24:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780255628576018433, '支付订单', 1775500307898949634, 6, 'order', 'operator/payOrder/index', NULL, 1, 0, 'C', '0', '0', 'system:order:list', 'material-symbols:order-approve', 103, 1, '2024-04-16 23:32:48', 1, '2025-03-30 21:12:38', '支付订单菜单');
INSERT INTO `sys_menu` VALUES (1780255628576018434, '支付订单查询', 1780255628576018433, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:orders:query', '#', 103, 1, '2024-04-16 23:32:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780255628576018435, '支付订单新增', 1780255628576018433, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:orders:add', '#', 103, 1, '2024-04-16 23:32:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780255628576018436, '支付订单修改', 1780255628576018433, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:orders:edit', '#', 103, 1, '2024-04-16 23:32:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780255628576018437, '支付订单删除', 1780255628576018433, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:orders:remove', '#', 103, 1, '2024-04-16 23:32:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1780255628576018438, '支付订单导出', 1780255628576018433, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:orders:export', '#', 103, 1, '2024-04-16 23:32:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1843281231381852162, '文件管理', 1775500307898949634, 20, 'file', 'operator/oss/index', NULL, 1, 0, 'C', '0', '0', NULL, 'material-symbols-light:folder', 103, 1, '2024-10-07 21:24:27', 1, '2024-12-27 23:03:04', '');
INSERT INTO `sys_menu` VALUES (1898286496441393153, '知识库管理', 1775500307898949634, 10, 'knowledgeBase', 'operator/knowledgeBase/index', NULL, 1, 0, 'C', '0', '0', '', 'garden:knowledge-base-26', 103, 1, '2025-03-08 16:15:44', 1, '2025-03-10 00:21:26', '');
INSERT INTO `sys_menu` VALUES (1906674838461321217, '配置信息', 1775500307898949634, 13, 'configurationManage', 'operator/configurationManage/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'mdi:archive-cog-outline', 103, 1, '2025-03-31 19:48:48', 1, '2025-03-31 19:59:58', '配置信息菜单');
INSERT INTO `sys_menu` VALUES (1906674838461321218, '配置信息查询', 1906674838461321217, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:query', '#', 103, 1, '2025-03-31 19:48:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1906674838461321219, '配置信息新增', 1906674838461321217, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:add', '#', 103, 1, '2025-03-31 19:48:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1906674838461321220, '配置信息修改', 1906674838461321217, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:edit', '#', 103, 1, '2025-03-31 19:48:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1906674838461321221, '配置信息删除', 1906674838461321217, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:remove', '#', 103, 1, '2025-03-31 19:48:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1906674838461321222, '配置信息导出', 1906674838461321217, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:export', '#', 103, 1, '2025-03-31 19:48:48', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1929170702299045890, '提示词模板', 1775500307898949634, 1, 'promptTemplate', 'operator/promptTemplate/index', '', 1, 0, 'C', '0', '0', 'system:promptTemplate:list', 'fluent:prompt-16-filled', 103, 1, '2025-09-17 16:43:40', NULL, NULL, '提示词模板菜单');
INSERT INTO `sys_menu` VALUES (1929170702299045891, '提示词模板查询', 1929170702299045890, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:promptTemplate:query', '#', 103, 1, '2025-09-17 16:43:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1929170702299045892, '提示词模板新增', 1929170702299045890, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:promptTemplate:add', '#', 103, 1, '2025-09-17 16:43:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1929170702299045893, '提示词模板修改', 1929170702299045890, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:promptTemplate:edit', '#', 103, 1, '2025-09-17 16:43:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1929170702299045894, '提示词模板删除', 1929170702299045890, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:promptTemplate:remove', '#', 103, 1, '2025-09-17 16:43:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1929170702299045895, '提示词模板导出', 1929170702299045890, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:promptTemplate:export', '#', 103, 1, '2025-09-17 16:43:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944213468857495553, '模型分组', 2000, 1, 'schemaGroup', 'dev/schemaGroup/index', NULL, 1, 0, 'C', '0', '0', NULL, '#', 103, 1, '2025-07-13 09:53:07', 1, '2025-07-13 09:54:45', '模型分组菜单');
INSERT INTO `sys_menu` VALUES (1944213468857495554, '模型分组查询', 1944213468857495553, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaGroup:list', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944213468857495555, '模型分组新增', 1944213468857495553, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaGroup:add', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944213468857495556, '模型分组修改', 1944213468857495553, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaGroup:edit', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944213468857495557, '模型分组删除', 1944213468857495553, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaGroup:remove', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944229086906281985, '数据模型', 2000, 2, 'schema', 'dev/schema/index', NULL, 1, 0, 'C', '0', '0', NULL, '#', 103, 1, '2025-07-13 10:55:11', NULL, '2025-07-13 10:55:11', '数据模型菜单');
INSERT INTO `sys_menu` VALUES (1944229086906281986, '模型数据查询', 1944229086906281985, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schema:list', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944229086906281987, '模型数据新增', 1944229086906281985, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schema:add', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944229086906281988, '模型数据修改', 1944229086906281985, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schema:edit', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1944229086906281989, '模型数据删除', 1944229086906281985, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schema:remove', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1946466176918249473, '模型字段管理', 2000, 3, 'schemaField', 'dev/schemaField/index', NULL, 1, 0, 'C', '0', '0', NULL, '#', 103, 1, '2025-07-19 15:04:35', NULL, '2025-07-19 15:04:35', '模型字段管理菜单');
INSERT INTO `sys_menu` VALUES (1946466176918249474, '模型字段管理查询', 1946466176918249473, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaField:list', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1946466176918249475, '模型字段管理新增', 1946466176918249473, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaField:add', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1946466176918249476, '模型字段管理修改', 1946466176918249473, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaField:edit', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1946466176918249477, '模型字段管理删除', 1946466176918249473, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'dev:schemaField:remove', '#', 103, 1, '2025-06-24 19:06:58', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1946483381643743233, '知识库角色管理', 1775500307898949634, 12, 'knowledgeRole', 'operator/knowledgeRole/index', NULL, 1, 0, 'C', '0', '0', NULL, 'ri:user-3-fill', 103, 1, '2025-07-19 16:41:17', NULL, NULL, '知识库角色管理');
INSERT INTO `sys_menu` VALUES (1971550631887237121, '数字人管理', 0, 0, 'aihuman', '', NULL, 1, 0, 'M', '0', '0', NULL, 'mdi:account-cog', NULL, NULL, '2025-09-26 20:21:15', NULL, '2025-09-26 20:21:15', '');
INSERT INTO `sys_menu` VALUES (1971582278942666752, '交互数字人配置', 1971550631887237121, 1, 'aihumanConfig', 'aihuman/aihumanConfig/index', NULL, 1, 0, 'C', '0', '0', 'aihuman:aihumanConfig:list', '#', 103, 1, '2025-09-26 22:40:40', 1, '2025-09-26 22:43:10', '交互数字人配置菜单');
INSERT INTO `sys_menu` VALUES (1971582278942666753, '交互数字人配置查询', 1971582278942666752, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:query', '#', 103, 1, '2025-09-26 22:40:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971582278942666754, '交互数字人配置新增', 1971582278942666752, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:add', '#', 103, 1, '2025-09-26 22:40:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971582278942666755, '交互数字人配置修改', 1971582278942666752, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:edit', '#', 103, 1, '2025-09-26 22:40:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971582278942666756, '交互数字人配置删除', 1971582278942666752, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:remove', '#', 103, 1, '2025-09-26 22:40:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971582278942666757, '交互数字人配置导出', 1971582278942666752, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanConfig:export', '#', 103, 1, '2025-09-26 22:40:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1972543718952386561, 'Live2D数字人体验', 1971550631887237121, 10, 'aihumanPublish', 'aihuman/aihumanPublish/index', NULL, 1, 0, 'C', '0', '0', NULL, '#', NULL, NULL, '2025-09-29 14:07:25', 1, '2025-09-29 14:36:28', '');

SET FOREIGN_KEY_CHECKS = 1;
