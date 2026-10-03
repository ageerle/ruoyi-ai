package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.util.function.Consumer;

/** Preview complete panels from legacy arrays or a scene_plan/panels envelope; never approves partial data. */
final class ShortDramaPanelStreamParser {
    private final Consumer<JsonNode> sink;
    private final StringBuilder object = new StringBuilder();
    private int arrayDepth, objectDepth, captureParentDepth;
    private boolean string, escaped, capturing;
    ShortDramaPanelStreamParser(Consumer<JsonNode> sink) { this.sink = sink; }
    void accept(String chunk) {
        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);
            if (capturing) object.append(c);
            if (string) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') string = false;
                continue;
            }
            if (c == '"') { string = true; continue; }
            if (c == '[') arrayDepth++;
            else if (c == ']') arrayDepth--;
            else if (c == '{') {
                if (!capturing && arrayDepth == 1 && objectDepth <= 1) {
                    capturing = true; captureParentDepth = objectDepth; object.setLength(0); object.append(c);
                }
                objectDepth++;
            } else if (c == '}') {
                objectDepth--;
                if (capturing && objectDepth == captureParentDepth) {
                    capturing = false;
                    try {
                        JsonNode node = AtlasMediaSupport.OBJECT_MAPPER.readTree(object.toString());
                        if (node.isObject() && (node.has("panel_number") || node.has("panelNumber"))) sink.accept(node);
                    } catch (java.io.IOException ignored) { /* malformed objects await full batch validation */ }
                    object.setLength(0);
                }
            }
        }
    }
}
