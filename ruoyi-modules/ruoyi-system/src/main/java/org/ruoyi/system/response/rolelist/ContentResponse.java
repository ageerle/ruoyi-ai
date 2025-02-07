package org.ruoyi.system.response.rolelist;

import lombok.Data;

/**
 *
 * 描述：获取当前用户的语音角色列表返回对象
 *
 * @author ageerle@163.com
 * date 2024/4/27
 */
import java.util.List;

@Data
public class ContentResponse {

    /**
     * 语音角色 ID
     */
    private String id;

    /**
     * 语音角色名称
     */
    private String name;

    /**
     * 语音角色状态，可以为pending（瞬时克隆已完成）、lora-pending（专业克隆训练中）、lora-success（专业克隆已完成）、lora-failed（专业克隆失败）
     */
    private String status;


    private Metadata metadata;
    @Data
    public static class Metadata {

        /**
         * 语音角色头像 URL
         */
        private String avatar;

        /**
         * 语音角色描述
         */
        private String description;

        /**
         * 语音角色风格列表
         */
        private List<prompt> prompts;

        private String previewAudio;

        private String promptMP3StorageUrl;

        @Data
        public static class prompt {
            /**
             * 角色风格 ID
             */
            private String id;

            /**
             * 角色风格名称
             */
            private String name;

            /**
             * 角色风格样本音频 URL
             */
            private String promptOriginAudioStorageUrl;

        }

    }

}

