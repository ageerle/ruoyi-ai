package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ruoyi.controller.shortdrama.ShortDramaSkillController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class ShortDramaSkillCategoriesTest {
    @TempDir Path root;

    @Test void categoriesEndpointReturnsCatalogRatherThanLookingUpASkillNamedCategories() throws Exception {
        var catalog = spy(new ShortDramaSkillCatalog(root.toString()));
        var mvc = MockMvcBuilders.standaloneSetup(new ShortDramaSkillController(catalog, mock(ShortDramaSkillMarket.class))).build();
        mvc.perform(get("/short-drama/skills/categories"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[2].type").value("screenwriting"));
        verify(catalog, never()).detail("categories");
        for (var category : catalog.categories()) {
            if (category.editable()) assertDoesNotThrow(() -> catalog.list(category.type(), false));
        }
    }
}
