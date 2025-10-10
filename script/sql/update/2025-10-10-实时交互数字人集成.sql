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
                                 `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '交互名称',
                                 `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '交互内容',
                                 `create_time` datetime(0) DEFAULT NULL COMMENT '创建时间',
                                 `update_time` datetime(0) DEFAULT NULL COMMENT '更新时间',
                                 `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI人类交互信息表' ROW_FORMAT = Dynamic;

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




