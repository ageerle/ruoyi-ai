package org.ruoyi.domain.vo.shortdrama;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ShortDramaDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private ShortDramaProjectVo project;

    private ShortDramaScriptVo script;

    private List<ShortDramaCharacterVo> characters;

    private List<ShortDramaLocationVo> locations;

    private List<ShortDramaStoryboardVo> storyboards;
}
